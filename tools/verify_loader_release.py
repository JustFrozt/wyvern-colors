#!/usr/bin/env python3
"""Check the identity, assets, and loader boundaries of a native release JAR."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import tomllib
from zipfile import ZipFile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('jar', type=Path)
    parser.add_argument('loader', choices=('forge', 'neoforge'))
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    version = next(line.split('=', 1)[1] for line in
                   (root / 'gradle.properties').read_text().splitlines()
                   if line.startswith('mod_version='))
    metadata_path = ('META-INF/neoforge.mods.toml' if args.loader == 'neoforge'
                     else 'META-INF/mods.toml')
    with ZipFile(args.jar) as jar:
        names = set(jar.namelist())
        metadata = tomllib.loads(jar.read(metadata_path).decode())
        mod, = metadata['mods']
        assert (mod['modId'], mod['version'], mod['authors']) == (
            'wyverncolors', version, 'JustFrozt')
        assert mod['logoFile'] in names
        dependencies = {item['modId']: item for item in metadata['dependencies']['wyverncolors']}
        assert dependencies['minecraft']['versionRange'] == '[1.21.11]'
        assert args.loader in dependencies
        assert all(item['side'] == 'CLIENT' for item in dependencies.values())
        assert 'fabric.mod.json' not in names
        assert 'LICENSE_wyvern-colors' in names and 'NOTICE_wyvern-colors' in names
        assert 'JustFrozt' in jar.read('LICENSE_wyvern-colors').decode()
        assert metadata['license'] == 'MIT'
        assert 'Optional credit request (not part of the MIT License)' in jar.read('LICENSE_wyvern-colors').decode()
        assert jar.read('LICENSE_wyvern-colors').decode() == (root / 'LICENSE').read_text()
        assert not any(name.startswith(('config/', 'logs/', '.gradle/')) for name in names)
        approved = json.loads((root / 'tools/approved_assets_sha256.json').read_text())
        for name, digest in approved.items():
            assert hashlib.sha256(jar.read(name)).hexdigest() == digest, name
        classes = [name for name in names if name.endswith('.class')]
        assert classes
        for name in classes:
            assert name.startswith('dev/justfrozt/wyverncolors/'), name
            data = jar.read(name)
            assert struct.unpack('>IHH', data[:8]) == (0xCAFEBABE, 0, 65), name
            assert b'net/fabricmc/loader/' not in data, name
            assert b'net/fabricmc/api/ClientModInitializer' not in data, name
        mixins = json.loads(jar.read('wyverncolors.mixins.json'))
        assert mixins['required']
        assert {'NativePotentialOverlayMixin', 'wynntils.BitmapDigitSelectionMixin',
                'wynntils.BitmapDigitInkMixin', 'wynntils.BakedGlyphRectAccessor'} <= set(mixins['client'])
        assert 'dev/justfrozt/wyverncolors/config/NativePotentialSettings.class' in names
        assert 'dev/justfrozt/wyverncolors/NativePotentialRenderer.class' in names
        assert {'ItemModelManagerMixin', 'BasicItemModelUnbakedMixin',
                'CompositeItemModelAccessor', 'ColorResourceReloadMixin'} <= set(mixins['client'])
        if args.loader == 'neoforge':
            assert {item['config'] for item in metadata['mixins']} == {
                'wyverncolors.mixins.json', 'wyverncolors.wynntils.mixins.json'}
            assert 'dev/justfrozt/wyverncolors/NeoForgeClient.class' in names
        else:
            assert b'screen.wyverncolors.potential.nativeConflict' in jar.read(
                'dev/justfrozt/wyverncolors/config/WyvernSettingsScreen.class')
            assert metadata['clientSideOnly']
            assert 'dev/justfrozt/wyverncolors/ForgeMod.class' in names
            assert 'wyverncolors.mixins.json' in jar.read('META-INF/MANIFEST.MF').decode()
        print(f'{args.loader}: metadata, loader boundary, {len(approved)} assets, '
              f'and {len(classes)} Java 21 classes verified')
        print('SHA-256:', hashlib.sha256(args.jar.read_bytes()).hexdigest())


if __name__ == '__main__':
    main()
