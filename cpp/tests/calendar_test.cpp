#include "chinese_calendar/calendar.hpp"
#include <fstream>
#include <iostream>
#include <sstream>
#include <stdexcept>

namespace {
void require(bool condition, const std::string& message) {
    if (!condition) throw std::runtime_error(message);
}
}

int main(int argc, char** argv) {
    try {
        require(argc == 2, "Pass the repository root as the only argument");
        const std::string root = argv[1];
        std::ifstream fixtures(root + "/tests/fixtures/readings.txt");
        require(fixtures.good(), "Cannot open regression fixtures");
        std::string line;
        int count = 0;
        while (std::getline(fixtures, line)) {
            if (!line.empty() && line.back() == '\r') line.pop_back();
            if (line.empty() || line[0] == '#') continue;
            const auto separator = line.find('\t');
            require(separator != std::string::npos, "Malformed fixture");
            std::istringstream input(line.substr(0, separator));
            int year, month, day, hour, minute;
            require(bool(input >> year >> month >> day >> hour >> minute), "Malformed date");
            const auto actual = chinese_calendar::format_calendar(year, month, day, hour, minute);
            require(actual == line.substr(separator + 1), "Regression: " + line.substr(0, separator));
            ++count;
        }
        require(count >= 1000, "Missing regression fixtures");
        std::ifstream invalid(root + "/tests/fixtures/invalid_dates.txt");
        require(invalid.good(), "Cannot open invalid-date fixtures");
        while (std::getline(invalid, line)) {
            if (line.empty() || line[0] == '#') continue;
            std::istringstream input(line);
            int year, month, day, hour, minute;
            require(bool(input >> year >> month >> day >> hour >> minute), "Malformed invalid date");
            for (int api = 0; api < 2; ++api) {
                bool rejected = false;
                try {
                    if (api == 0) chinese_calendar::format_calendar(year, month, day, hour, minute);
                    else chinese_calendar::get_calendar(year, month, day, hour, minute);
                } catch (const std::invalid_argument&) { rejected = true; }
                require(rejected, "Accepted invalid input: " + line);
            }
        }
        const auto result = chinese_calendar::get_calendar(2024, 2, 10, 8, 0);
        require(result.lunar.year == 2024 && result.lunar.month == 1 && result.lunar.day == 1, "Lunar date");
        require(!result.lunar.is_leap_month && result.pillars.hour == "戊辰", "Structured result");
        require(!result.solar_term.has_value(), "Missing solar term");
        require(chinese_calendar::get_lunar_date(2033, 12, 22).is_leap_month, "2033 leap month");
        require(chinese_calendar::get_calendar(2024, 2, 10).solar.hour == 12, "Default hour");
        require(chinese_calendar::get_calendar(2024, 2, 4).solar_term->time == "16:27", "Solar term");
        std::cout << "C++: " << count << " regression fixtures and API checks passed\n";
        return 0;
    } catch (const std::exception& error) {
        std::cerr << error.what() << '\n';
        return 1;
    }
}
