#!/usr/bin/env python3
"""Run the repository's dependency-free tests; no package publication or downloads."""

import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def executable(name: str) -> str:
    found = shutil.which(name)
    if not found:
        raise RuntimeError(
            f"Missing {name}; install it and add it to PATH, or select fewer --languages"
        )
    return found


def run(arguments: list[str], environment: dict[str, str]) -> None:
    print("Running:", " ".join(map(str, arguments)), flush=True)
    subprocess.run(list(map(str, arguments)), cwd=ROOT, env=environment, check=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--languages",
        nargs="+",
        choices=["python", "javascript", "java", "cpp"],
        default=["python", "javascript", "java", "cpp"],
    )
    parser.add_argument(
        "--java-home", type=Path, help="JDK 17+ directory, otherwise JAVA_HOME/PATH"
    )
    parser.add_argument("--cxx", help="C++17 compiler executable (defaults to CXX or g++)")
    arguments = parser.parse_args()
    environment = os.environ.copy()
    environment["PYTHONPATH"] = str(ROOT / "python/src")
    environment["PYTHONDONTWRITEBYTECODE"] = "1"
    environment["PYTHONIOENCODING"] = "utf-8"
    build = ROOT / ".build/tests"
    build.mkdir(parents=True, exist_ok=True)
    try:
        if "python" in arguments.languages:
            run(
                [sys.executable, "-B", "-m", "unittest", "discover", "-s", "python/tests", "-v"],
                environment,
            )
        if "javascript" in arguments.languages:
            run([executable("node"), "--test", "javascript/test/calendar.test.cjs"], environment)
        if "java" in arguments.languages:
            java_home = arguments.java_home or (
                Path(os.environ["JAVA_HOME"]) if os.environ.get("JAVA_HOME") else None
            )
            suffix = ".exe" if os.name == "nt" else ""
            javac = (
                str(java_home / "bin" / ("javac" + suffix)) if java_home else executable("javac")
            )
            java = str(java_home / "bin" / ("java" + suffix)) if java_home else executable("java")
            classes = build / "java"
            classes.mkdir(exist_ok=True)
            sources = sorted((ROOT / "java/src").rglob("*.java"))
            run(
                [
                    javac,
                    "-encoding",
                    "UTF-8",
                    "--release",
                    "8",
                    "-Xlint:all",
                    "-d",
                    str(classes),
                    *map(str, sources),
                ],
                environment,
            )
            run(
                [
                    java,
                    "-Dfile.encoding=UTF-8",
                    "-cp",
                    str(classes),
                    "calendar.CalendarTest",
                    str(ROOT),
                ],
                environment,
            )
        if "cpp" in arguments.languages:
            compiler = arguments.cxx or os.environ.get("CXX") or executable("g++")
            binary = build / ("calendar_test.exe" if os.name == "nt" else "calendar_test")
            run(
                [
                    compiler,
                    "-std=c++17",
                    "-O2",
                    "-Wall",
                    "-Wextra",
                    "-Wpedantic",
                    "-finput-charset=UTF-8",
                    "-fexec-charset=UTF-8",
                    "-I",
                    str(ROOT / "cpp/include"),
                    str(ROOT / "cpp/src/calendar.cpp"),
                    str(ROOT / "cpp/tests/calendar_test.cpp"),
                    "-o",
                    str(binary),
                ],
                environment,
            )
            run([str(binary), str(ROOT)], environment)
    except (RuntimeError, subprocess.CalledProcessError, OSError) as error:
        parser.exit(1, f"Test run failed: {error}\n")
    print("All selected language tests passed.")


if __name__ == "__main__":
    main()
