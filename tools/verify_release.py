#!/usr/bin/env python3
"""Check approved assets and release boundaries without third-party Python packages."""
from pathlib import Path
import argparse
import hashlib
import json
import struct
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('jar', nargs='?', type=Path, help='Optional remapped release JAR')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    expected = json.loads((root / 'tools/approved_assets_sha256.json').read_text())
    for name, digest in expected.items():
        actual = hashlib.sha256((root / 'src/main/resources' / name).read_bytes()).hexdigest()
        if actual != digest:
            raise ValueError(f'Approved artwork changed: {name}')
    print(f'Approved models, item tint definitions and texture: {len(expected)} files unchanged')

    if args.jar is None:
        return
    with zipfile.ZipFile(args.jar) as jar:
        names = set(jar.namelist())
        metadata = json.loads(jar.read('fabric.mod.json'))
        if metadata['environment'] != 'client' or metadata['depends']['minecraft'] != '1.21.11':
            raise ValueError('Unexpected client/game scope')
        version = next(line.split('=', 1)[1] for line in
                       (root / 'gradle.properties').read_text().splitlines()
                       if line.startswith('mod_version='))
        authors = json.loads((root / 'src/main/resources/fabric.mod.json').read_text())['authors']
        if metadata['version'] != version or metadata.get('authors') != authors:
            raise ValueError('Unexpected release metadata')
        for optional in ('wynntils', 'modmenu'):
            if optional in metadata['depends'] or optional not in metadata.get('suggests', {}):
                raise ValueError(f'Incorrect optional dependency: {optional}')
        for name in ('LICENSE_wyvern-colors', 'NOTICE_wyvern-colors'):
            if name not in names:
                raise ValueError(f'Missing release notice: {name}')
        if metadata.get('license') != 'MIT':
            raise ValueError('Unexpected license identifier')
        if jar.read('LICENSE_wyvern-colors').decode() != (root / 'LICENSE').read_text():
            raise ValueError('Packaged MIT license does not match source')
        if metadata.get('icon') not in names:
            raise ValueError('Missing project icon')
        for name, digest in expected.items():
            if hashlib.sha256(jar.read(name)).hexdigest() != digest:
                raise ValueError(f'Packaged artwork changed: {name}')
        classes = [name for name in names if name.endswith('.class')]
        if not classes:
            raise ValueError('Release has no compiled classes')
        for name in classes:
            if not name.startswith('dev/justfrozt/wyverncolors/'):
                raise ValueError(f'Third-party class bundled: {name}')
            magic, _, major = struct.unpack('>IHH', jar.read(name)[:8])
            if magic != 0xCAFEBABE or major != 65:
                raise ValueError(f'Unexpected Java bytecode: {name}')
        if any(name.endswith('.jar') for name in names):
            raise ValueError('Unexpected bundled dependency')
        for name in ('dev/justfrozt/wyverncolors/config/WyvernSettings.class',
                     'dev/justfrozt/wyverncolors/config/WyvernSettingsScreen.class',
                     'dev/justfrozt/wyverncolors/GuiColorRenderer.class',
                     'dev/justfrozt/wyverncolors/ColorFallbackWarnings.class',
                     'dev/justfrozt/wyverncolors/RenderIsolation.class',
                     'dev/justfrozt/wyverncolors/WarningLimiter.class',
                     'dev/justfrozt/wyverncolors/WyvernItemModels.class',
                     'dev/justfrozt/wyverncolors/mixin/BasicItemModelUnbakedMixin.class',
                     'dev/justfrozt/wyverncolors/mixin/CompositeItemModelAccessor.class',
                     'dev/justfrozt/wyverncolors/mixin/ColorResourceReloadMixin.class'):
            if name not in names:
                raise ValueError(f'Missing settings/fallback implementation: {name}')
        mixins = json.loads(jar.read('wyverncolors.mixins.json'))
        if not {'ItemModelManagerMixin', 'BasicItemModelUnbakedMixin', 'CompositeItemModelAccessor', 'ColorResourceReloadMixin'}.issubset(
                mixins.get('client', [])):
            raise ValueError('Missing required native rendering/reload mixin')
        for removed in ('MountLayoutOptions', 'MountLayoutOptionsMixin', 'MountLayoutSettingsScreenMixin'):
            if any(name.endswith('/' + removed + '.class') for name in classes):
                raise ValueError(f'Obsolete settings hook bundled: {removed}')
        print(f'Release metadata, notices, assets and {len(classes)} Java 21 classes verified')
        print('JAR SHA-256:', hashlib.sha256(args.jar.read_bytes()).hexdigest())


if __name__ == '__main__':
    main()
