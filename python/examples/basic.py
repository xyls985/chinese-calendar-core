from chinese_calendar_core import format_calendar, get_calendar

calendar = get_calendar(2026, 10, 4, 9, 30)
print(calendar.lunar)
print(calendar.pillars)
print(format_calendar(2026, 10, 4, 9, 30))
