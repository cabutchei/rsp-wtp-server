#!/usr/bin/env python3
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def main():
    pom_path = Path(sys.argv[1] if len(sys.argv) > 1 else 'pom.xml')
    root = ET.parse(pom_path).getroot()
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    version = root.findtext('m:version', namespaces=ns)
    if not version:
        raise SystemExit(f'Could not find <version> in {pom_path}')
    print(version)


if __name__ == '__main__':
    main()
