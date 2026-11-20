#!/usr/bin/env python3
"""Update src/main/resources/recorder1.zip from the seleniumRobot-chrome-extension repository.

Steps: clone the repository, run `npm install`, zip everything (without .git)
under a top-level `recorder1/` folder, as expected by ChromeStarter.

Usage: python scripts/update_chrome_extension.py [--ref BRANCH_OR_TAG] [--repo URL] [--output ZIP]
Requirements: git, npm (Node.js) available in the PATH.
"""
import argparse
import os
import shutil
import stat
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

DEFAULT_REPO = "https://github.com/bhecquet/seleniumRobot-chrome-extension"
ROOT_DIR = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT = ROOT_DIR / "src" / "main" / "resources" / "recorder1.zip"
ARCHIVE_ROOT = "recorder1"
EXCLUDED_DIRS = {".git", ".idea", "__MACOSX"}


def run(cmd, cwd=None):
    print("> " + " ".join(cmd))
    subprocess.run(cmd, cwd=cwd, check=True)


def tool(name):
    path = shutil.which(name)
    if not path:
        sys.exit(f"'{name}' not found in PATH")
    return path


def remove_readonly(func, path, _exc):
    os.chmod(path, stat.S_IWRITE)
    func(path)


def build_zip(source: Path, output: Path):
    tmp_zip = output.with_suffix(".zip.tmp")
    with zipfile.ZipFile(tmp_zip, "w", zipfile.ZIP_DEFLATED) as zf:
        for current, dirs, files in os.walk(source):
            dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS]
            rel_dir = Path(current).relative_to(source)
            for name in files:
                arc = Path(ARCHIVE_ROOT) / rel_dir / name
                zf.write(Path(current) / name, arc.as_posix())
            if not files and not dirs:
                zf.writestr((Path(ARCHIVE_ROOT) / rel_dir).as_posix() + "/", "")
    tmp_zip.replace(output)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--repo", default=DEFAULT_REPO, help="extension git repository")
    parser.add_argument("--ref", default=None, help="branch or tag to package (default: repository default branch)")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="zip file to write")
    args = parser.parse_args()

    git, npm = tool("git"), tool("npm")
    workdir = Path(tempfile.mkdtemp(prefix="chrome-extension-"))
    try:
        source = workdir / ARCHIVE_ROOT
        clone = [git, "clone", "--depth", "1"]
        if args.ref:
            clone += ["--branch", args.ref]
        run(clone + [args.repo, str(source)])
        run([npm, "install"], cwd=source)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        build_zip(source, args.output)
        print(f"Updated {args.output}")
    finally:
        shutil.rmtree(workdir, onerror=remove_readonly)


if __name__ == "__main__":
    main()
