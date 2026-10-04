# Chinese Calendar Core

Chinese lunar dates, the 24 solar terms, and Four Pillars in fixed UTC+08:00, implemented in Python, JavaScript, Java, and C++ without runtime network access or third-party calendar dependencies.

[中文](README.md) · [API](docs/API.md) · [Algorithms and precision](docs/ALGORITHMS.md) · [Validation](docs/VALIDATION.md)

## Scope

Inputs cover Gregorian years **1813–2100**. The lunar year starts on lunar New Year's Day; the year pillar changes at the calculated beginning of spring, and month pillars change at the twelve month-boundary solar terms. Day pillars change at midnight. The 23:00–23:59 hour uses the current day's stem.

Solar term instants are approximate. Pillars near a term transition may differ from a high-precision ephemeris. Dates before 1929 use modern rules in fixed UTC+08:00 and can differ from historical almanacs. See the documented examples before choosing this library for minute-sensitive or historical work.

## Local installation and examples

These commands assume a source checkout; they do not assume packages have been published to a registry.

```sh
# Python 3.10+
python -m pip install ./python
python -m chinese_calendar_core 2026 10 4 9 30

# Node.js 18+; both CommonJS and ESM are supported
node javascript/examples/basic.cjs
node javascript/examples/basic.mjs

# Java: build with JDK 17+, run compiled classes on Java 8+
python scripts/test_all.py --languages java
javac -encoding UTF-8 -cp .build/tests/java -d .build/tests/java java/examples/CalendarExample.java
java '-Dfile.encoding=UTF-8' -cp .build/tests/java CalendarExample

# C++17 and CMake 3.16+
cmake -S cpp -B .build/cpp -DCMAKE_BUILD_TYPE=Release
cmake --build .build/cpp --config Release
ctest --test-dir .build/cpp -C Release --output-on-failure
```

```python
from chinese_calendar_core import get_calendar, get_lunar_date, format_calendar

result = get_calendar(2026, 10, 4, 9, 30)
print(result.lunar.month)  # 8
print(result.pillars.year)  # 丙午
print(format_calendar(2026, 10, 4, 9, 30))
```

The corresponding JavaScript and Java APIs are `getCalendar`, `getLunarDate`, and `formatCalendar`; C++ uses `chinese_calendar::get_calendar`, `get_lunar_date`, and `format_calendar`. Time defaults to **12:00**. Invalid input is rejected. A returned solar term belongs to the requested day and may occur after the requested time.

## Development

```sh
python scripts/test_all.py
```

The test runner requires Python, Node, JDK 17+, and g++; use `--languages` to select implementations, `--java-home` to select a JDK, and `--cxx` to select a compiler. C++ can also be integrated through the CMake target `ChineseCalendar::chinese_calendar`.

The repository includes 1,008 shared regression fixtures and contract tests. Previous external validation covered 73,049 HKO calendar dates and 165,257 instants per language. Regression snapshots intentionally preserve documented approximation behavior; they are not an independent astronomical oracle.

Contributions are welcome; see [CONTRIBUTING.md](CONTRIBUTING.md). Licensed under [MIT](LICENSE). Algorithm references and external data sources are documented in [REFERENCES.md](docs/REFERENCES.md).
