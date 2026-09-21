#!/usr/bin/env python3
"""Compile the interpreter and check the test programs against their comments."""

from pathlib import Path
import re
import subprocess
import sys
import tempfile


ROOT = Path(__file__).resolve().parents[1]
TESTS = ROOT / "tests"
TEST_MODES = {
    "success": ((), ("-ntc",)),
    "failure/static-semantics": ((),),
    "failure/static-semantics-ntc": (("-ntc",),),
    "failure/static-semantics-only": ((),),
    "failure/static-semantics-only-ntc": (("-ntc",),),
    "failure/dynamic-semantics": ((), ("-ntc",)),
}


def expected_result(path):
    """Read the expected diagnostic/output, retaining exact message spacing."""
    source = path.read_text(encoding="utf-8")
    error = re.search(r"// ((?:Static|Dynamic) error: [^\n]+)", source)
    if error:
        return 1, "", error.group(1) + "\n"

    # Existing fixtures describe vector output in Italian.
    vector = re.search(r'// stampa (?:la stringa ")?(VectorValue\[\d+\])', source)
    return 0, vector.group(1) + "\n" if vector else "", ""


def run_tests(classes):
    passed = 0
    total = 0
    programs = 0
    for directory, modes in TEST_MODES.items():
        paths = sorted((TESTS / directory).glob("*.txt"))
        if not paths:
            raise ValueError("No test programs found in " + directory)
        programs += len(paths)
        for path in paths:
            expected = expected_result(path)
            for options in modes:
                total += 1
                label = "{} ({})".format(
                    path.relative_to(ROOT), "-ntc" if options else "typechecking enabled"
                )
                command = [
                    "java", "-cp", str(classes), "projectLabo.Main",
                    *options, "-i", str(path),
                ]
                try:
                    result = subprocess.run(
                        command, capture_output=True, text=True, timeout=10
                    )
                except subprocess.TimeoutExpired:
                    print("FAIL {}: execution timed out".format(label))
                    continue
                actual = result.returncode, result.stdout, result.stderr
                if actual == expected:
                    passed += 1
                else:
                    print("FAIL " + label)
                    for field, wanted, got in zip(
                        ("exit status", "stdout", "stderr"), expected, actual
                    ):
                        if wanted != got:
                            print("  {}: expected {!r}, got {!r}".format(field, wanted, got))

    print("{}/{} checks passed ({} test programs).".format(passed, total, programs))
    return 0 if passed == total else 1


def main():
    try:
        # Build outside the source tree and automatically remove generated files.
        with tempfile.TemporaryDirectory(prefix="progettolp-tests-") as temporary:
            classes = Path(temporary) / "classes"
            classes.mkdir()
            sources = sorted((ROOT / "Lab" / "projectLabo").rglob("*.java"))
            build = subprocess.run(
                ["javac", "--release", "21", "-d", str(classes)]
                + [str(source) for source in sources],
                capture_output=True, text=True, timeout=60,
            )
            if build.returncode:
                print(build.stdout + build.stderr, file=sys.stderr, end="")
                return 1
            return run_tests(classes)
    except (OSError, ValueError, subprocess.TimeoutExpired) as error:
        print("Test runner error: {}".format(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
