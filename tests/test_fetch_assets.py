import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('fetch_assets', Path(__file__).resolve().parents[1] / 'scripts/fetch-assets.py')
fetch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fetch)

class FetchAssetsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.payload = b'bootstrap fixture\n'
        folder = self.root / 'docs/recovery'
        folder.mkdir(parents=True)
        (folder / 'release-assets.json').write_text(json.dumps({'tag': 'test', 'assets': [{'name': 'bootstrap-aarch64-v66-incomplete.zip', 'bytes': len(self.payload), 'sha256': hashlib.sha256(self.payload).hexdigest()}]}))
        self.target = self.root / 'android/app/src/main/cpp/bootstrap-aarch64.zip'

    def run_fetch(self, payload=None):
        with patch.object(fetch, 'ROOT', self.root), patch('sys.argv', ['fetch-assets.py', '--bootstrap']), patch.object(fetch.urllib.request, 'urlopen', return_value=io.BytesIO(self.payload if payload is None else payload)) as request:
            fetch.main()
            return request.call_count

    def test_restores_exact_bootstrap(self):
        self.assertEqual(self.run_fetch(), 1)
        self.assertEqual(self.target.read_bytes(), self.payload)

    def test_corrupt_download_leaves_no_bootstrap(self):
        with self.assertRaises(RuntimeError):
            self.run_fetch(b'corrupt')
        self.assertFalse(self.target.exists())
        self.assertFalse(self.target.with_name(self.target.name + '.part').exists())

    def test_preserves_different_existing_file(self):
        self.target.parent.mkdir(parents=True)
        self.target.write_bytes(b'local edits')
        with self.assertRaises(SystemExit):
            self.run_fetch()
        self.assertEqual(self.target.read_bytes(), b'local edits')

    def test_matching_existing_file_needs_no_network(self):
        self.target.parent.mkdir(parents=True)
        self.target.write_bytes(self.payload)
        self.assertEqual(self.run_fetch(), 0)

if __name__ == '__main__':
    unittest.main()
