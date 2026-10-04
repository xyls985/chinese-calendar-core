import dataclasses
from pathlib import Path
import subprocess
import sys
import unittest

from chinese_calendar_core import format_calendar, get_calendar, get_lunar_date

ROOT = Path(__file__).resolve().parents[2]


class CalendarTests(unittest.TestCase):
    def test_regression_fixtures(self):
        count = 0
        for line in (ROOT / "tests/fixtures/readings.txt").read_text(encoding="utf-8").splitlines():
            if not line or line.startswith("#"):
                continue
            arguments, expected = line.split("\t", 1)
            with self.subTest(arguments=arguments):
                self.assertEqual(format_calendar(*map(int, arguments.split())), expected)
            count += 1
        self.assertGreaterEqual(count, 1000)

    def test_invalid_dates(self):
        for line in (ROOT / "tests/fixtures/invalid_dates.txt").read_text().splitlines():
            if not line or line.startswith("#"):
                continue
            values = list(map(int, line.split()))
            for function in (format_calendar, get_calendar):
                with self.subTest(function=function.__name__, values=values):
                    with self.assertRaises(ValueError):
                        function(*values)
        with self.assertRaises(ValueError):
            get_lunar_date(2023, 2, 29)

    def test_reject_nonintegers(self):
        for value in (True, 2024.0, "2024", None):
            with self.subTest(value=value), self.assertRaises(TypeError):
                get_calendar(value, 1, 1)

    def test_structured_result(self):
        result = get_calendar(2024, 2, 10, 8)
        self.assertEqual((result.lunar.year, result.lunar.month, result.lunar.day), (2024, 1, 1))
        self.assertFalse(result.lunar.is_leap_month)
        self.assertEqual(result.pillars.hour, "戊辰")
        self.assertIsNone(result.solar_term)
        with self.assertRaises(dataclasses.FrozenInstanceError):
            result.lunar.day = 2
        self.assertTrue(get_lunar_date(2033, 12, 22).is_leap_month)

    def test_solar_term_and_defaults(self):
        term = get_calendar(2024, 2, 4).solar_term
        self.assertEqual((term.name, term.time), ("立春", "16:27"))
        self.assertEqual(get_calendar(2024, 2, 10).solar.hour, 12)
        self.assertEqual(format_calendar(2024, 2, 10), format_calendar(2024, 2, 10, 12, 0))

    def test_import_is_silent(self):
        process = subprocess.run(
            [sys.executable, "-B", "-c", "import chinese_calendar_core"],
            capture_output=True,
            text=True,
            check=True,
        )
        self.assertEqual(process.stdout, "")
        self.assertEqual(process.stderr, "")

    def test_cli(self):
        arguments = [sys.executable, "-B", "-m", "chinese_calendar_core"]
        process = subprocess.run(
            arguments + ["2024", "2", "10"], capture_output=True, text=True, encoding="utf-8"
        )
        self.assertEqual(process.returncode, 0)
        self.assertEqual(process.stdout.strip(), format_calendar(2024, 2, 10))
        invalid = subprocess.run(arguments + ["2023", "2", "29"], capture_output=True, text=True)
        self.assertEqual(invalid.returncode, 2)


if __name__ == "__main__":
    unittest.main()
