"""Chinese lunar dates and Four Pillars calculated in fixed UTC+08:00.

Solar terms are approximate. Consult docs/ALGORITHMS.md before relying on
minute-level transitions or historical calendar dates before 1929.
"""

from dataclasses import dataclass
from datetime import date

from . import _core

__version__ = "0.1.0"
MIN_YEAR = 1813
MAX_YEAR = 2100
__all__ = [
    "MIN_YEAR",
    "MAX_YEAR",
    "SolarDateTime",
    "LunarDate",
    "FourPillars",
    "SolarTerm",
    "CalendarResult",
    "get_calendar",
    "get_lunar_date",
    "format_calendar",
]


@dataclass(frozen=True)
class SolarDateTime:
    year: int
    month: int
    day: int
    hour: int
    minute: int


@dataclass(frozen=True)
class LunarDate:
    year: int
    month: int
    day: int
    is_leap_month: bool


@dataclass(frozen=True)
class FourPillars:
    year: str
    month: str
    day: str
    hour: str


@dataclass(frozen=True)
class SolarTerm:
    name: str
    time: str


@dataclass(frozen=True)
class CalendarResult:
    solar: SolarDateTime
    lunar: LunarDate
    pillars: FourPillars
    solar_term: SolarTerm | None


def _validate(year: int, month: int, day: int, hour: int, minute: int) -> None:
    for name, value in zip(
        ("year", "month", "day", "hour", "minute"), (year, month, day, hour, minute)
    ):
        if not isinstance(value, int) or isinstance(value, bool):
            raise TypeError(f"{name} must be an integer")
    if not MIN_YEAR <= year <= MAX_YEAR:
        raise ValueError(f"year must be between {MIN_YEAR} and {MAX_YEAR}")
    date(year, month, day)
    if not 0 <= hour <= 23:
        raise ValueError("hour must be between 0 and 23")
    if not 0 <= minute <= 59:
        raise ValueError("minute must be between 0 and 59")


def get_lunar_date(year: int, month: int, day: int) -> LunarDate:
    """Convert a valid Gregorian date to a lunar date; raises TypeError/ValueError."""
    _validate(year, month, day, 12, 0)
    lunar_year, lunar_month, is_leap_month, lunar_day = _core.calculate_lunar_date(year, month, day)
    return LunarDate(lunar_year, lunar_month, lunar_day, is_leap_month)


def get_calendar(
    year: int, month: int, day: int, hour: int = 12, minute: int = 0
) -> CalendarResult:
    """Return structured calendar data for a UTC+08:00 wall-clock time.

    The optional solar_term describes a term anywhere on the requested day,
    even when the input time is earlier than the term. Its displayed time is
    rounded to a minute; the pillar transition uses the unrounded estimate.
    """
    _validate(year, month, day, hour, minute)
    instant = _core.beijing_time_to_jd(year, month, day, hour, minute)
    solar_year = _core.solar_year_for_instant(instant, year)
    day_cycle = _core.day_cycle_index(year, month, day)
    pillars = FourPillars(
        _core.format_sexagenary_cycle(_core.year_cycle_index(solar_year)),
        _core.format_sexagenary_cycle(_core.month_cycle_index(instant, year, solar_year)),
        _core.format_sexagenary_cycle(day_cycle),
        _core.format_sexagenary_cycle(_core.hour_cycle_index(day_cycle % 10, hour)),
    )
    term_text = _core.format_solar_term_on_date(year, month, day)
    solar_term = SolarTerm(*term_text.split(" ", 1)) if term_text else None
    return CalendarResult(
        SolarDateTime(year, month, day, hour, minute),
        get_lunar_date(year, month, day),
        pillars,
        solar_term,
    )


def format_calendar(year: int, month: int, day: int, hour: int = 12, minute: int = 0) -> str:
    """Return the human-readable Chinese summary; use get_calendar for data."""
    _validate(year, month, day, hour, minute)
    return _core.format_calendar_unchecked(year, month, day, hour, minute)
