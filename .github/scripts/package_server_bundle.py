#!/usr/bin/env python3
import argparse
import os
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED


def find_prefix(entries, suffixes):
    for suffix in suffixes:
        for entry in entries:
            if entry.endswith(suffix):
                return entry
    return None


def normalize_directory_entry(name):
    return name if name.endswith('/') else f'{name}/'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--version', required=True)
    parser.add_argument('--output', required=True)
    parser.add_argument('--product-zip')
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parents[2]
    default_zip = repo_root / 'distribution' / 'distribution' / 'target' / 'products' / 'com.github.cabutchei.rsp.server.product-macosx.cocoa.x86_64.zip'
    product_zip = Path(args.product_zip) if args.product_zip else default_zip
    if not product_zip.exists():
        raise SystemExit(f'Product zip not found: {product_zip}')

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with ZipFile(product_zip) as source_zip, ZipFile(output_path, 'w', compression=ZIP_DEFLATED) as target_zip:
        entries = source_zip.namelist()
        configuration_prefix = find_prefix(
            entries,
            (
                '/Contents/Eclipse/configuration/',
                '/configuration/',
            ),
        )
        plugins_prefix = find_prefix(
            entries,
            (
                '/Contents/Eclipse/plugins/',
                '/plugins/',
            ),
        )

        if not configuration_prefix or not plugins_prefix:
            raise SystemExit(
                f'Could not locate configuration/plugins directories inside {product_zip}'
            )

        bundle_root = 'rsp-wtp-server'
        target_zip.writestr(normalize_directory_entry(bundle_root), b'')
        target_zip.writestr(normalize_directory_entry(f'{bundle_root}/configuration'), b'')
        target_zip.writestr(normalize_directory_entry(f'{bundle_root}/plugins'), b'')

        prefixes = (
            (configuration_prefix, f'{bundle_root}/configuration/'),
            (plugins_prefix, f'{bundle_root}/plugins/'),
        )
        for source_prefix, target_prefix in prefixes:
            for entry in entries:
                if not entry.startswith(source_prefix):
                    continue
                relative = entry[len(source_prefix):]
                if not relative:
                    continue
                target_name = f'{target_prefix}{relative}'
                if entry.endswith('/'):
                    target_zip.writestr(normalize_directory_entry(target_name), b'')
                else:
                    target_zip.writestr(target_name, source_zip.read(entry))

    print(f'Packaged {output_path}')


if __name__ == '__main__':
    main()
