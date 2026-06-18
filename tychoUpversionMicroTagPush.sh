#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

DRY_RUN=0
ASSUME_YES=0
SKIP_BUILD=0
ALLOW_NON_MASTER=0

usage() {
  cat <<'EOF'
Usage: ./tychoUpversionMicroTagPush.sh [options]

Prepare and publish an rsp-wtp-server release using the current tag-driven
GitHub Actions workflow, then bump to the next patch .Final version.

Options:
  --dry-run           Print actions without editing files, committing, tagging, or pushing.
  --yes               Skip interactive confirmation prompts.
  --skip-build        Skip local Maven validation builds.
  --allow-non-master  Allow releasing from a branch other than master.
  --help, -h          Show this help text.

Prerequisites:
  - Clean tracked git worktree
  - Authenticated GitHub CLI (`gh auth login`)
  - Current project version should be an unreleased version
  - The server release workflow in .github/workflows/gh-actions.yml remains tag-driven
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=1
      ;;
    --yes)
      ASSUME_YES=1
      ;;
    --skip-build)
      SKIP_BUILD=1
      ;;
    --allow-non-master)
      ALLOW_NON_MASTER=1
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
  shift
done

run() {
  echo "+ $*"
  if [[ ${DRY_RUN} -eq 0 ]]; then
    "$@"
  fi
}

confirm() {
  local prompt="$1"
  if [[ ${ASSUME_YES} -eq 1 ]]; then
    return 0
  fi

  local answer
  read -r -p "${prompt} [y/N] " answer
  case "${answer}" in
    y|Y|yes|YES)
      return 0
      ;;
    *)
      echo "Aborted."
      exit 1
      ;;
  esac
}

require_command() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "Required command not found: ${cmd}" >&2
    exit 1
  fi
}

parse_repo_slug() {
  local remote_url="$1"
  case "${remote_url}" in
    git@github.com:*.git)
      echo "${remote_url#git@github.com:}" | sed 's/\.git$//'
      ;;
    https://github.com/*.git)
      echo "${remote_url#https://github.com/}" | sed 's/\.git$//'
      ;;
    https://github.com/*)
      echo "${remote_url#https://github.com/}"
      ;;
    *)
      return 1
      ;;
  esac
}

current_version() {
  python3 .github/scripts/read_project_version.py pom.xml
}

public_version_from() {
  local version="$1"
  echo "${version%.Final}"
}

next_patch_final_from() {
  local public_version="$1"
  python3 - "${public_version}" <<'EOF'
import sys

version = sys.argv[1]
parts = version.split(".")
if len(parts) != 3:
    raise SystemExit(f"Unsupported version: {version}")
major, minor, patch = parts
print(f"{major}.{minor}.{int(patch) + 1}.Final")
EOF
}

latest_release_tag() {
  git tag --sort=-creatordate | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -n 1 || true
}

update_target_definition_name() {
  local version="$1"
  local target_file="targetplatform/rsp-target.target"
  if [[ ! -f "${target_file}" ]]; then
    return 0
  fi

  if [[ ${DRY_RUN} -eq 0 ]]; then
    perl -0pi -e "s/name=\"rsp-target-[^\"]+\"/name=\"rsp-target-${version}\"/" "${target_file}"
  else
    echo "+ perl -0pi -e s/name=\\\"rsp-target-[^\\\"]+\\\"/name=\\\"rsp-target-${version}\\\"/ ${target_file}"
  fi
}

set_project_version() {
  local version="$1"
  run mvn org.eclipse.tycho:tycho-versions-plugin:1.3.0:set-version "-DnewVersion=${version}" -B
  update_target_definition_name "${version}"
}

validate_release_build() {
  local release_version="$1"

  run mvn -pl targetplatform -am install -DskipTests -B
  run mvn -pl distribution/distribution -am clean package -DskipTests -B

  local artifact="distribution/distribution/target/rsp-wtp-server-${release_version}.zip"
  if [[ ${DRY_RUN} -eq 0 && ! -f "${artifact}" ]]; then
    echo "Expected release artifact not found: ${artifact}" >&2
    exit 1
  fi
}

validate_snapshot_build() {
  run mvn clean verify -DskipTests -B
}

build_release_notes() {
  local previous_tag="$1"
  local output_file="$2"
  local version="$3"
  local branch="$4"

  {
    echo "Release ${version}"
    echo
    echo "Branch: ${branch}"
    echo
    echo "Changes:"
    if [[ -n "${previous_tag}" ]]; then
      git log --pretty=format:'- %s' "${previous_tag}..HEAD"
    else
      git log --pretty=format:'- %s'
    fi
  } > "${output_file}"
}

require_command git
require_command python3
require_command mvn
require_command gh
require_command perl

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "This repository has tracked changes. Commit or stash them before releasing." >&2
  exit 1
fi

UNTRACKED_FILES="$(git ls-files --others --exclude-standard)"
if [[ -n "${UNTRACKED_FILES}" ]]; then
  echo "Warning: untracked files are present and will not be included in the release commit:"
  echo "${UNTRACKED_FILES}" | sed -n '1,20p'
  if [[ "$(echo "${UNTRACKED_FILES}" | wc -l | awk '{print $1}')" -gt 20 ]]; then
    echo "... additional untracked files omitted"
  fi
fi

if [[ ${DRY_RUN} -eq 0 ]]; then
  gh auth status >/dev/null
fi

BRANCH="$(git branch --show-current)"
if [[ "${BRANCH}" != "master" && ${ALLOW_NON_MASTER} -eq 0 ]]; then
  echo "Releases should be cut from master because the workflow updates LATEST on master." >&2
  echo "Re-run with --allow-non-master only if you intentionally want to release from ${BRANCH}." >&2
  exit 1
fi

REMOTE_URL="$(git remote get-url origin)"
REPO_SLUG="$(parse_repo_slug "${REMOTE_URL}")" || {
  echo "Unsupported origin remote URL: ${REMOTE_URL}" >&2
  exit 1
}

CURRENT_VERSION="$(current_version)"
PUBLIC_RELEASE_VERSION="$(public_version_from "${CURRENT_VERSION}")"
NEXT_FINAL_VERSION="$(next_patch_final_from "${PUBLIC_RELEASE_VERSION}")"
if [[ "${CURRENT_VERSION}" == *.Final ]]; then
  EFFECTIVE_RELEASE_VERSION="${CURRENT_VERSION}"
else
  EFFECTIVE_RELEASE_VERSION="${PUBLIC_RELEASE_VERSION}.Final"
fi
TAG="v${PUBLIC_RELEASE_VERSION}"
PREVIOUS_TAG="$(latest_release_tag)"
ARTIFACT_PATH="distribution/distribution/target/rsp-wtp-server-${PUBLIC_RELEASE_VERSION}.zip"

if git rev-parse -q --verify "refs/tags/${TAG}" >/dev/null; then
  echo "Tag ${TAG} already exists. Bump the project version before releasing again." >&2
  exit 1
fi

echo "Repository: ${REPO_SLUG}"
echo "Branch: ${BRANCH}"
echo "Current version: ${CURRENT_VERSION}"
echo "Public release version: ${PUBLIC_RELEASE_VERSION}"
echo "Maven release version: ${EFFECTIVE_RELEASE_VERSION}"
echo "Next development version: ${NEXT_FINAL_VERSION}"
echo "Release tag: ${TAG}"
echo "Expected artifact: ${ARTIFACT_PATH}"
if [[ -n "${PREVIOUS_TAG}" ]]; then
  echo "Previous release tag: ${PREVIOUS_TAG}"
fi
if [[ ${DRY_RUN} -eq 1 ]]; then
  echo "Mode: dry-run"
fi

confirm "Proceed with the server release flow?"

NOTES_FILE="$(mktemp)"
trap 'rm -f "${NOTES_FILE}"' EXIT
build_release_notes "${PREVIOUS_TAG}" "${NOTES_FILE}" "${PUBLIC_RELEASE_VERSION}" "${BRANCH}"

if [[ "${CURRENT_VERSION}" != *.Final ]]; then
  echo "Setting project version to release version ${EFFECTIVE_RELEASE_VERSION}."
  set_project_version "${EFFECTIVE_RELEASE_VERSION}"
else
  echo "Project is already on a .Final version; using ${CURRENT_VERSION} as-is."
fi

if [[ ${SKIP_BUILD} -eq 0 ]]; then
  echo "Running release validation build."
  validate_release_build "${PUBLIC_RELEASE_VERSION}"
else
  echo "Skipping release validation build."
fi

if [[ "${CURRENT_VERSION}" != *.Final ]]; then
  run git add -u
  run git commit -m "Prepare release ${EFFECTIVE_RELEASE_VERSION}" --signoff
  run git push origin "${BRANCH}"
fi

echo
echo "Release notes preview:"
sed -n '1,40p' "${NOTES_FILE}"
echo

confirm "Create and push tag ${TAG}? This will trigger the server GitHub Actions release workflow."

run git tag -a "${TAG}" -m "Release ${TAG}"
run git push origin "${TAG}"

echo
echo "Tag pushed. The workflow at .github/workflows/gh-actions.yml will:"
echo "  - build distribution/distribution/target/rsp-wtp-server-${PUBLIC_RELEASE_VERSION}.zip"
echo "  - publish the GitHub release"
echo "  - update LATEST on master"
echo
echo "Expected release URL:"
echo "  https://github.com/${REPO_SLUG}/releases/tag/${TAG}"
echo

confirm "Continue to bump the repository to ${NEXT_FINAL_VERSION}?"

set_project_version "${NEXT_FINAL_VERSION}"

if [[ ${SKIP_BUILD} -eq 0 ]]; then
  echo "Running next-version validation build."
  validate_snapshot_build
else
  echo "Skipping next-version validation build."
fi

run git add -u
run git commit -m "Move to ${NEXT_FINAL_VERSION}" --signoff
run git push origin "${BRANCH}"

echo
echo "Release preparation complete."
echo "Published tag: ${TAG}"
echo "Next development version: ${NEXT_FINAL_VERSION}"
