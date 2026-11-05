#!/usr/bin/env python3
"""Check an isolated copy of the index without modifying files or staging changes."""

import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--secrets-only", action="store_true")
    args = parser.parse_args()
    root = Path(subprocess.check_output(
        ["git", "rev-parse", "--show-toplevel"], text=True).strip())
    if not shutil.which("gitleaks"):
        raise SystemExit("gitleaks is required; install the pinned version in docs/FOUNDATION.md")
    with tempfile.TemporaryDirectory(prefix="oryxos-index-") as temporary:
        snapshot = Path(temporary)
        subprocess.run(["git", "checkout-index", "--all", f"--prefix={snapshot}{os.sep}"],
                       cwd=root, check=True)
        # Default rules, no project-provided allowlist, and redacted diagnostics.
        subprocess.run(["gitleaks", "dir", str(snapshot), "--redact", "--no-banner"],
                       cwd=temporary, check=True)
        if not args.secrets_only:
            subprocess.run([str(snapshot / "mvnw"), "-B", "-ntp", "spotless:check"],
                           cwd=snapshot, check=True)


if __name__ == "__main__":
    main()
