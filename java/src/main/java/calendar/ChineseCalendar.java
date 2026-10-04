package calendar;

import java.time.DateTimeException;
import java.time.LocalDate;

/** Chinese calendar in fixed UTC+08:00. Solar term instants are approximate. */
public final class ChineseCalendar {
    public static final int MIN_YEAR = 1813;
    public static final int MAX_YEAR = 2100;

    private ChineseCalendar() { }

    public static final class SolarDateTime {
        public final int year, month, day, hour, minute;
        private SolarDateTime(int year, int month, int day, int hour, int minute) {
            this.year = year; this.month = month; this.day = day;
            this.hour = hour; this.minute = minute;
        }
    }

    public static final class LunarDate {
        public final int year, month, day;
        public final boolean isLeapMonth;
        private LunarDate(int year, int month, int day, boolean isLeapMonth) {
            this.year = year; this.month = month; this.day = day;
            this.isLeapMonth = isLeapMonth;
        }
    }

    public static final class FourPillars {
        public final String year, month, day, hour;
        private FourPillars(String year, String month, String day, String hour) {
            this.year = year; this.month = month; this.day = day; this.hour = hour;
        }
    }

    public static final class SolarTerm {
        public final String name;
        /** Approximate UTC+08:00 time rounded to HH:MM. */
        public final String time;
        private SolarTerm(String name, String time) { this.name = name; this.time = time; }
    }

    public static final class CalendarResult {
        public final SolarDateTime solar;
        public final LunarDate lunar;
        public final FourPillars pillars;
        /** A term on the requested day, or null; it may occur after the input time. */
        public final SolarTerm solarTerm;
        private CalendarResult(SolarDateTime solar, LunarDate lunar, FourPillars pillars, SolarTerm solarTerm) {
            this.solar = solar; this.lunar = lunar; this.pillars = pillars; this.solarTerm = solarTerm;
        }
    }

    private static void validateDateTime(int year, int month, int day, int hour, int minute) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException("year must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        try {
            LocalDate.of(year, month, day);
        } catch (DateTimeException error) {
            throw new IllegalArgumentException("Invalid Gregorian date: " + error.getMessage(), error);
        }
        if (hour < 0 || hour > 23) throw new IllegalArgumentException("hour must be between 0 and 23");
        if (minute < 0 || minute > 59) throw new IllegalArgumentException("minute must be between 0 and 59");
    }

    /** Convert a Gregorian date, rejecting invalid inputs with IllegalArgumentException. */
    public static LunarDate getLunarDate(int year, int month, int day) {
        validateDateTime(year, month, day, 12, 0);
        AstronomicalCalendar.LunarDay lunar = AstronomicalCalendar.calculateLunarDate(year, month, day);
        return new LunarDate(lunar.year, lunar.number, lunar.day, lunar.leap);
    }

    public static CalendarResult getCalendar(int year, int month, int day) {
        return getCalendar(year, month, day, 12, 0);
    }

    public static CalendarResult getCalendar(int year, int month, int day, int hour, int minute) {
        validateDateTime(year, month, day, hour, minute);
        double instant = AstronomicalCalendar.beijingTimeToJd(year, month, day, hour, minute);
        int solarYear = AstronomicalCalendar.solarYearForInstant(instant, year);
        int dayCycle = AstronomicalCalendar.dayCycleIndex(year, month, day);
        FourPillars pillars = new FourPillars(
            AstronomicalCalendar.formatSexagenaryCycle(AstronomicalCalendar.yearCycleIndex(solarYear)),
            AstronomicalCalendar.formatSexagenaryCycle(AstronomicalCalendar.monthCycleIndex(instant, year, solarYear)),
            AstronomicalCalendar.formatSexagenaryCycle(dayCycle),
            AstronomicalCalendar.formatSexagenaryCycle(AstronomicalCalendar.hourCycleIndex(dayCycle % 10, hour)));
        String termText = AstronomicalCalendar.formatSolarTermOnDate(year, month, day);
        SolarTerm solarTerm = null;
        if (!termText.isEmpty()) {
            String[] term = termText.split(" ", 2);
            solarTerm = new SolarTerm(term[0], term[1]);
        }
        return new CalendarResult(new SolarDateTime(year, month, day, hour, minute),
                                  getLunarDate(year, month, day), pillars, solarTerm);
    }

    public static String formatCalendar(int year, int month, int day) {
        return formatCalendar(year, month, day, 12, 0);
    }

    public static String formatCalendar(int year, int month, int day, int hour, int minute) {
        validateDateTime(year, month, day, hour, minute);
        return AstronomicalCalendar.formatCalendarUnchecked(year, month, day, hour, minute);
    }
}
