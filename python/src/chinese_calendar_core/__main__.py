"""Command-line entry point: python -m chinese_calendar_core YEAR MONTH DAY [HOUR MINUTE]."""

import argparse

from . import format_calendar


def main() -> None:
    parser = argparse.ArgumentParser(description="Chinese calendar in UTC+08:00 (1813–2100)")
    for name in ("year", "month", "day"):
        parser.add_argument(name, type=int)
    parser.add_argument("hour", type=int, nargs="?", default=12)
    parser.add_argument("minute", type=int, nargs="?", default=0)
    arguments = parser.parse_args()
    try:
        print(
            format_calendar(
                arguments.year, arguments.month, arguments.day, arguments.hour, arguments.minute
            )
        )
    except (TypeError, ValueError) as error:
        parser.error(str(error))


if __name__ == "__main__":
    main()
