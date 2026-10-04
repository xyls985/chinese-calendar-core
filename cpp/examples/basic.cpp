#include "chinese_calendar/calendar.hpp"
#include <iostream>

int main() {
    const auto calendar = chinese_calendar::get_calendar(2026, 10, 4, 9, 30);
    std::cout << calendar.lunar.year << '-' << calendar.lunar.month << '-' << calendar.lunar.day << '\n';
    std::cout << chinese_calendar::format_calendar(2026, 10, 4, 9, 30) << '\n';
}
