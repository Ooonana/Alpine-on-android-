#!/usr/bin/env python3
"""Download checksummed recovery assets using only Python's standard library."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
REPOSITORY = 'Ooonana/Alpine-on-android-'

def digest(path):
    with path.open('rb') as stream:
        h = hashlib.sha256()
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(chunk)
        return h.hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--bootstrap', action='store_true', help='Restore the exact unfinished v66 bootstrap into the Android source')
    group.add_argument('--all', action='store_true', help='Download every historical asset (many GB)')
    group.add_argument('--name', help='Download one asset by exact name')
    group.add_argument('--list', action='store_true', help='List asset names and sizes without downloading')
    args = parser.parse_args()
    manifest = json.loads((ROOT / 'docs/recovery/release-assets.json').read_text())
    assets = manifest['assets']
    if args.list:
        for asset in assets:
            print(f"{asset['bytes']:>12}  {asset['name']}")
        return
    wanted = 'bootstrap-aarch64-v66-incomplete.zip' if args.bootstrap else args.name
    selected = assets if args.all else [a for a in assets if a['name'] == wanted]
    if not selected:
        parser.error('Asset name not found; use --list')
    for asset in selected:
        name = asset['name']
        if Path(name).name != name or name in ('.', '..'):
            raise ValueError('Invalid asset name in manifest')
        target = ROOT / 'downloads' / name
        if args.bootstrap:
            target = ROOT / 'android/app/src/main/cpp/bootstrap-aarch64.zip'
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            if digest(target) == asset['sha256']:
                print('Verified existing:', target)
                continue
            raise SystemExit(f'Refusing to replace different existing file: {target}')
        if shutil.disk_usage(target.parent).free < asset['bytes'] + 512 * 1024**2:
            raise SystemExit(f'Not enough disk space for {name} plus 512 MiB reserve')
        url = f"https://github.com/{REPOSITORY}/releases/download/{urllib.parse.quote(manifest['tag'], safe='')}/{urllib.parse.quote(name, safe='')}"
        partial = target.with_name(target.name + '.part')
        print('Downloading:', name, flush=True)
        try:
            with urllib.request.urlopen(url, timeout=90) as response, partial.open('wb') as output:
                shutil.copyfileobj(response, output, 1024 * 1024)
            if partial.stat().st_size != asset['bytes'] or digest(partial) != asset['sha256']:
                raise RuntimeError('Downloaded size or SHA-256 mismatch: ' + name)
            os.replace(partial, target)
        except BaseException:
            partial.unlink(missing_ok=True)
            raise
        print('Verified:', target)

if __name__ == '__main__':
    main()
