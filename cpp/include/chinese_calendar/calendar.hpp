#pragma once

#include <optional>
#include <string>

namespace chinese_calendar {

inline constexpr int MIN_YEAR = 1813;
inline constexpr int MAX_YEAR = 2100;

struct SolarDateTime {
    int year, month, day, hour, minute;
};

struct LunarDate {
    int year, month, day;
    bool is_leap_month;
};

struct FourPillars {
    std::string year, month, day, hour;
};

struct SolarTerm {
    std::string name;
    std::string time;  // Approximate UTC+08:00 time, rounded to HH:MM.
};

struct CalendarResult {
    SolarDateTime solar;
    LunarDate lunar;
    FourPillars pillars;
    // Daily event; it may occur after the supplied time. Empty on other days.
    std::optional<SolarTerm> solar_term;
};

// All functions use fixed UTC+08:00; invalid input throws std::invalid_argument.
LunarDate get_lunar_date(int year, int month, int day);
CalendarResult get_calendar(int year, int month, int day, int hour = 12, int minute = 0);
std::string format_calendar(int year, int month, int day, int hour = 12, int minute = 0);

}  // namespace chinese_calendar
