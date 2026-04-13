# RSP-WTP Server

This repository is a fork of the original `redhat-developer/rsp-server`.

This fork focuses on:
- Eclipse/WTP-backed server adapters
- Partial WTP support
- an Equinox-based runtime distribution
- ongoing architectural cleanup and package refactoring

## What This Project Is

Runtime Server Protocol (RSP) lets a client (IDE or tooling) manage application servers through an LSP-style transport.

The transport model is based on LSP4J, but the protocol messages are RSP-specific.

Protocol docs in this repo:
- `api/docs/com.github.cabutchei.rsp.schema/src/main/resources/schemaMD/specification.md`
- `api/docs/com.github.cabutchei.rsp.schema/src/main/resources/schemaMD/lsp_base_protocol.md`

## Repository Identity

- Fork repository: `https://github.com/cabutchei/rsp-server`
- Upstream project: `https://github.com/redhat-developer/rsp-server`

## Build

### Prerequisites

- Maven 3.x
- JDK 21 (recommended for this build as currently configured)

### Full Build

```bash
mvn clean verify
```

### Faster Compile-Only Check

```bash
mvn clean verify -DskipTests=true
```

## Equinox Distribution

The product module is:
- `distribution/distribution`

Build only distribution (and required modules):

```bash
mvn -pl distribution/distribution -am package
```

Typical outputs:
- `distribution/distribution/target/products/com.github.cabutchei.rsp.server.product-*.zip`
- `distribution/distribution/target/com.github.cabutchei.rsp.distribution.product-*.zip`

## Running the Product (Local Build Output)

After packaging, product layouts are generated under:
- `distribution/distribution/target/products/com.github.cabutchei.rsp.server.product/...`

Examples:
- macOS executable: `.../rsp-wtp-server.app/Contents/MacOS/eclipse`
- Windows executable: `.../rsp-wtp-server/eclipse.exe`

## Modules (High Level)

- `api`: protocol schema and API artifacts
- `client`: CLI and client-side artifacts
- `framework`: core server framework and SPI
- `runtimes`: runtime/server adapter implementations (WST/WTP and others)
- `distribution`: product/distribution assembly