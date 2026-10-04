#include "chinese_calendar/calendar.hpp"

#include <stdexcept>

// Internal astronomical calculations; public API definitions follow below.
#include <cmath>
#include <cstdio>
#include <map>
#include <string>
#include <utility>
#include <vector>

namespace {

const char* HEAVENLY_STEMS[] = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
const char* EARTHLY_BRANCHES[] = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
const char* LUNAR_MONTH_NAMES[] = {"", "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"};
const char* CHINESE_DIGITS[] = {"十", "一", "二", "三", "四", "五", "六", "七", "八", "九"};

struct MonthBoundaryTerm {
    double lon;
    int slot;
    const char* name;
    int doy;
};

struct PrincipalSolarTerm {
    double lon;
    const char* name;
    int doy;
};

const MonthBoundaryTerm MONTH_BOUNDARY_TERMS[] = {
    {315.0, 0, "立春", 35},  {345.0, 1, "惊蛰", 65},  {15.0, 2, "清明", 95},
    {45.0, 3, "立夏", 126},  {75.0, 4, "芒种", 157},  {105.0, 5, "小暑", 188},
    {135.0, 6, "立秋", 220}, {165.0, 7, "白露", 251}, {195.0, 8, "寒露", 281},
    {225.0, 9, "立冬", 312}, {255.0, 10, "大雪", 342}, {285.0, 11, "小寒", 6},
};

const PrincipalSolarTerm PRINCIPAL_SOLAR_TERMS[] = {
    {270.0, "冬至", 356}, {300.0, "大寒", 20},  {330.0, "雨水", 50},
    {0.0, "春分", 80},    {30.0, "谷雨", 110},  {60.0, "小满", 141},
    {90.0, "夏至", 173},  {120.0, "大暑", 204}, {150.0, "处暑", 235},
    {180.0, "秋分", 266}, {210.0, "霜降", 297}, {240.0, "小雪", 327},
};

struct Parts {
    int year;
    int month;
    int day;
    double hour;
};

struct LunarMonthSpan {
    int start;
    int end;
    int number;
    bool leap;
    int lunar_year;
};

int floor_mod(long long value, int base) {
    long long rem = value % base;
    if (rem < 0) rem += base;
    return static_cast<int>(rem);
}

double normalize_degrees(double deg) {
    deg = std::fmod(deg, 360.0);
    if (deg < 0.0) deg += 360.0;
    return deg;
}

double angular_difference(double a, double b) {
    return normalize_degrees(a - b + 180.0) - 180.0;
}

int gregorian_to_jdn(int year, int month, int day) {
    int shift = (14 - month) / 12;
    int y = year + 4800 - shift;
    int m = month + 12 * shift - 3;
    return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
}

Parts julian_day_to_components(double jd) {
    double z = std::floor(jd + 0.5);
    double frac = jd + 0.5 - z;
    double a;
    if (z >= 2299161.0) {
        double alpha = std::floor((z - 1867216.25) / 36524.25);
        a = z + 1.0 + alpha - std::floor(alpha / 4.0);
    } else {
        a = z;
    }
    double b = a + 1524.0;
    double c = std::floor((b - 122.1) / 365.25);
    double d = std::floor(365.25 * c);
    double e = std::floor((b - d) / 30.6001);
    double day = b - d - std::floor(30.6001 * e) + frac;
    int month = e < 14.0 ? static_cast<int>(e) - 1 : static_cast<int>(e) - 13;
    int year = month > 2 ? static_cast<int>(c) - 4716 : static_cast<int>(c) - 4715;
    int day_i = static_cast<int>(std::floor(day));
    Parts out;
    out.year = year;
    out.month = month;
    out.day = day_i;
    out.hour = (day - day_i) * 24.0;
    return out;
}

void beijing_date_from_jd(double jd_utc, int& year, int& month, int& day) {
    Parts p = julian_day_to_components(jd_utc + 8.0 / 24.0);
    year = p.year;
    month = p.month;
    day = p.day;
}

void beijing_clock_from_jd(double jd_utc, int& year, int& month, int& day, int& hour, int& minute) {
    Parts p = julian_day_to_components(jd_utc + 8.0 / 24.0);
    minute = static_cast<int>(std::round((p.hour - std::floor(p.hour)) * 60.0));
    hour = static_cast<int>(std::floor(p.hour));
    if (minute == 60) {
        minute = 0;
        hour += 1;
    }
    year = p.year;
    month = p.month;
    day = p.day;
    if (hour >= 24) {
        Parts next = julian_day_to_components(static_cast<double>(gregorian_to_jdn(year, month, day) + hour / 24));
        year = next.year;
        month = next.month;
        day = next.day;
        hour %= 24;
    }
}

double beijing_time_to_jd(int year, int month, int day, int hour, int minute) {
    return gregorian_to_jdn(year, month, day) - 0.5 + (hour - 8) / 24.0 + minute / 1440.0;
}

double estimate_delta_t(int year) {
    double y = static_cast<double>(year);
    if (y < 1800.0) {
        double t = y - 1700.0;
        return 8.83 + 0.1603 * t - 0.0059285 * t * t + 0.00013336 * t * t * t
            - t * t * t * t / 1174000.0;
    }
    if (y < 1860.0) {
        double t = y - 1800.0;
        return 13.72 - 0.332447 * t + 0.0068612 * t * t + 0.0041116 * t * t * t
            - 0.00037436 * t * t * t * t + 0.0000121272 * t * t * t * t * t
            - 0.0000001699 * t * t * t * t * t * t + 0.000000000875 * t * t * t * t * t * t * t;
    }
    if (y < 1900.0) {
        double t = y - 1860.0;
        return 7.62 + 0.5737 * t - 0.251754 * t * t + 0.01680668 * t * t * t
            - 0.0004473624 * t * t * t * t + t * t * t * t * t / 233174.0;
    }
    if (y > 2005.0 && y <= 2050.0) {
        double t = y - 2000.0;
        return 62.92 + 0.32217 * t + 0.005589 * t * t;
    }
    if (y >= 1986.0 && y <= 2005.0) {
        double t = y - 2000.0;
        return 63.86 + 0.3345 * t - 0.060374 * t * t + 0.0017275 * t * t * t
            + 0.000651814 * t * t * t * t + 0.00002373599 * t * t * t * t * t;
    }
    if (y >= 1961.0 && y < 1986.0) {
        double t = y - 1975.0;
        return 45.45 + 1.067 * t - t * t / 260.0 - t * t * t / 718.0;
    }
    if (y >= 1941.0 && y < 1961.0) {
        double t = y - 1950.0;
        return 29.07 + 0.407 * t - t * t / 233.0 + t * t * t / 2547.0;
    }
    if (y >= 1920.0 && y < 1941.0) {
        double t = y - 1920.0;
        return 21.20 + 0.84493 * t - 0.076100 * t * t + 0.0020936 * t * t * t;
    }
    if (y >= 1900.0 && y < 1920.0) {
        double t = y - 1900.0;
        return -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t * t * t
            - 0.000197 * t * t * t * t;
    }
    if (y <= 2150.0) {
        double u = (y - 1820.0) / 100.0;
        return -20.0 + 32.0 * u * u - 0.5628 * (2150.0 - y);
    }
    double u = (y - 1820.0) / 100.0;
    return -20.0 + 32.0 * u * u;
}

const double kSunL0[][3] = {
    {1.75347045673, 0.00000000000, 0.00000000000},
    {0.03341656456, 4.66925680417, 6283.07584999140},
    {0.00034894275, 4.62610241759, 12566.15169998280},
    {0.00003417571, 2.82886579606, 3.52311834900},
    {0.00003497056, 2.74411800971, 5753.38488489680},
    {0.00003135896, 3.62767041758, 77713.77146812050},
    {0.00002676218, 4.41808351397, 7860.41939243920},
    {0.00002342687, 6.13516237631, 3930.20969621960},
    {0.00001273166, 2.03709655772, 529.69096509460},
    {0.00001324292, 0.74246356352, 11506.76976979360},
    {0.00000901855, 2.04505443513, 26.29831979980},
    {0.00001199167, 1.10962944315, 1577.34354244780},
    {0.00000857223, 3.50849156957, 398.14900340820},
    {0.00000779786, 1.17882652114, 5223.69391980220},
    {0.00000990250, 5.23268129594, 5884.92684658320},
    {0.00000753141, 2.53339053818, 5507.55323866740},
    {0.00000505264, 4.58292563052, 18849.22754997420},
    {0.00000492379, 4.20506639861, 775.52261132400},
    {0.00000356655, 2.91954116867, 0.06731030280},
    {0.00000284125, 1.89869034186, 796.29800681640},
    {0.00000242810, 0.34481140906, 5486.77784317500},
    {0.00000317087, 5.84901952218, 11790.62908865880},
    {0.00000271039, 0.31488607649, 10977.07880469900},
    {0.00000206160, 4.80646606059, 2544.31441988340},
    {0.00000205385, 1.86947813692, 5573.14280143310},
    {0.00000202261, 2.45767795458, 6069.77675455340},
    {0.00000126184, 1.08302630210, 20.77539549240},
    {0.00000155516, 0.83306073807, 213.29909543800},
    {0.00000115132, 0.64544911683, 0.98032106820},
    {0.00000102851, 0.63599846727, 4694.00295470760},
    {0.00000101724, 4.26679821365, 7.11354700080},
    {0.00000132212, 3.41118275555, 2942.46342329160},
    {0.00000101895, 0.97569221824, 15720.83878487840},
};

const double kSunL1[][3] = {
    {6283.31966747491, 0.00000000000, 0.00000000000},
    {0.00206058863, 2.67823455584, 6283.07584999140},
    {0.00004303430, 2.63512650414, 12566.15169998280},
    {0.00000425264, 1.59046980729, 3.52311834900},
    {0.00000108977, 2.96618001993, 1577.34354244780},
    {0.00000119261, 5.79557487799, 26.29831979980},
};

const double kSunL2[][3] = {
    {0.00052918870, 0.00000000000, 0.00000000000},
    {0.00008719837, 1.07209665242, 6283.07584999140},
    {0.00000309125, 0.86728818832, 12566.15169998280},
};

const double kSunL3[][3] = {
    {0.00000289226, 5.84384198723, 6283.07584999140},
};

const double kSunL4[][3] = {
    {0.00000114084, 3.14159265359, 0.00000000000},
};

double sum_sun(const double terms[][3], int count, double t) {
    double total = 0.0;
    for (int i = 0; i < count; ++i) {
        total += terms[i][0] * std::cos(terms[i][1] + terms[i][2] * t);
    }
    return total;
}

double solar_longitude(double jd_tt) {
    double t = (jd_tt - 2451545.0) / 365250.0;
    double lon = 0.0;
    double power = 1.0;
    const double (*groups[])[3] = {kSunL0, kSunL1, kSunL2, kSunL3, kSunL4};
    const int counts[] = {33, 6, 3, 1, 1};
    for (int g = 0; g < 5; ++g) {
        lon += sum_sun(groups[g], counts[g], t) * power;
        power *= t;
    }
    lon = lon * 180.0 / 3.141592653589793 + 180.0 - 20.4898 / 3600.0;
    double century = (jd_tt - 2451545.0) / 36525.0;
    double omega = (125.04452 - 1934.136261 * century + 0.0020708 * century * century)
        * 3.141592653589793 / 180.0;
    lon -= 17.1996 * std::sin(omega) / 3600.0;
    return normalize_degrees(lon);
}

double calculate_solar_term(int year, double target, int guess_doy) {
    static thread_local std::map<std::pair<int, int>, double> cache;
    std::pair<int, int> key(year, static_cast<int>(std::llround(target * 10.0)));
    std::map<std::pair<int, int>, double>::iterator hit = cache.find(key);
    if (hit != cache.end()) return hit->second;
    double center = gregorian_to_jdn(year, 1, 1) + (guess_doy - 1) - 0.5;
    double lo = center - 25.0;
    double hi = center + 25.0;
    double dt = estimate_delta_t(year) / 86400.0;
    for (int i = 0; i < 50; ++i) {
        double mid = (lo + hi) / 2.0;
        if (angular_difference(solar_longitude(mid + dt), target) < 0.0) lo = mid;
        else hi = mid;
    }
    double jd = (lo + hi) / 2.0;
    cache[key] = jd;
    return jd;
}

std::string format_sexagenary_cycle(int index) {
    index = floor_mod(index, 60);
    return std::string(HEAVENLY_STEMS[index % 10]) + EARTHLY_BRANCHES[index % 12];
}

int sexagenary_index(int stem, int branch) {
    stem = floor_mod(stem, 10);
    branch = floor_mod(branch, 12);
    return stem + 10 * (floor_mod(stem - branch, 12) / 2);
}

int day_cycle_index(int year, int month, int day) {
    return floor_mod(static_cast<long long>(gregorian_to_jdn(year, month, day)) - gregorian_to_jdn(2024, 1, 1), 60);
}

int year_cycle_index(int sui) { return floor_mod(sui - 4, 60); }

int solar_year_for_instant(double jd_utc, int gregorian_year) {
    double lichun = calculate_solar_term(gregorian_year, 315.0, 35);
    return jd_utc >= lichun ? gregorian_year : gregorian_year - 1;
}

int month_cycle_index(double jd_utc, int gregorian_year, int sui) {
    double best_jd = 0.0;
    int slot = 10;
    bool found = false;
    for (int year = gregorian_year - 1; year <= gregorian_year + 1; ++year) {
        for (int i = 0; i < 12; ++i) {
            double event = calculate_solar_term(year, MONTH_BOUNDARY_TERMS[i].lon, MONTH_BOUNDARY_TERMS[i].doy);
            if (event <= jd_utc && (!found || event > best_jd)) {
                best_jd = event;
                slot = MONTH_BOUNDARY_TERMS[i].slot;
                found = true;
            }
        }
    }
    int year_stem = year_cycle_index(sui) % 10;
    int yin_stem = (year_stem % 5) * 2 + 2;
    int stem = floor_mod(yin_stem + slot, 10);
    int branch = floor_mod(2 + slot, 12);
    return sexagenary_index(stem, branch);
}

int hour_branch_index(int hour) { return ((hour + 1) / 2) % 12; }

int hour_cycle_index(int day_stem, int hour) {
    int slot = hour_branch_index(hour);
    int zi_stem = (day_stem % 5) * 2;
    int stem = floor_mod(zi_stem + slot, 10);
    return sexagenary_index(stem, slot);
}

double calculate_new_moon(int k) {
    double t = k / 1236.85;
    double tt = t * t;
    double t3 = tt * t;
    double t4 = t3 * t;
    double jde = 2451550.09765 + 29.530588861 * k + 0.00015437 * tt - 0.000000150 * t3 + 0.00000000073 * t4;
    double earth = 1.0 - 0.002516 * t - 0.0000074 * tt;
    double sun_m = normalize_degrees(2.5534 + 29.10535669 * k - 0.0000218 * tt - 0.00000011 * t3) * 3.141592653589793 / 180.0;
    double moon_m = normalize_degrees(201.5643 + 385.81693528 * k + 0.0107438 * tt + 0.00001239 * t3 - 0.000000058 * t4)
        * 3.141592653589793 / 180.0;
    double arg_f = normalize_degrees(160.7108 + 390.67050274 * k - 0.0016341 * tt - 0.00000227 * t3 + 0.000000011 * t4)
        * 3.141592653589793 / 180.0;
    double omega = normalize_degrees(124.7746 - 1.56375580 * k + 0.0020691 * tt + 0.00000215 * t3) * 3.141592653589793 / 180.0;
    double shift = -0.40720 * std::sin(moon_m)
        + 0.17241 * earth * std::sin(sun_m)
        + 0.01608 * std::sin(2 * moon_m)
        + 0.01039 * std::sin(2 * arg_f)
        + 0.00739 * earth * std::sin(moon_m - sun_m)
        - 0.00514 * earth * std::sin(moon_m + sun_m)
        + 0.00208 * earth * earth * std::sin(2 * sun_m)
        - 0.00111 * std::sin(moon_m - 2 * arg_f)
        - 0.00057 * std::sin(moon_m + 2 * arg_f)
        + 0.00056 * earth * std::sin(2 * moon_m + sun_m)
        - 0.00042 * std::sin(3 * moon_m)
        + 0.00042 * earth * std::sin(sun_m + 2 * arg_f)
        + 0.00038 * earth * std::sin(sun_m - 2 * arg_f)
        - 0.00024 * earth * std::sin(2 * moon_m - sun_m)
        - 0.00017 * std::sin(omega)
        - 0.00007 * std::sin(moon_m + 2 * sun_m)
        + 0.00004 * std::sin(2 * moon_m - 2 * arg_f)
        + 0.00004 * std::sin(3 * sun_m)
        + 0.00003 * std::sin(moon_m + sun_m - 2 * arg_f)
        + 0.00003 * std::sin(2 * moon_m + 2 * arg_f)
        - 0.00003 * std::sin(moon_m + sun_m + 2 * arg_f)
        + 0.00003 * std::sin(moon_m - sun_m + 2 * arg_f)
        - 0.00002 * std::sin(moon_m - sun_m - 2 * arg_f)
        - 0.00002 * std::sin(3 * moon_m + sun_m)
        + 0.00002 * std::sin(4 * moon_m);
    double a1 = normalize_degrees(299.77 + 0.107408 * k - 0.009173 * tt) * 3.141592653589793 / 180.0;
    shift += 0.000325 * std::sin(a1);
    int year = julian_day_to_components(jde).year;
    return jde + shift - estimate_delta_t(year) / 86400.0;
}

bool contains_principal_term(int start_ord, int end_ord, const std::vector<int>& qi) {
    for (size_t i = 0; i < qi.size(); ++i) {
        if (start_ord <= qi[i] && qi[i] < end_ord) return true;
    }
    return false;
}

int find_lunar_month(const std::vector<int>& moons, int ord) {
    for (size_t i = 0; i + 1 < moons.size(); ++i) {
        if (moons[i] <= ord && ord < moons[i + 1]) return static_cast<int>(i);
    }
    return -1;
}

std::vector<LunarMonthSpan> calculate_lunar_months(int solstice_year) {
    double winter = calculate_solar_term(solstice_year, 270.0, 356);
    double winter_next = calculate_solar_term(solstice_year + 1, 270.0, 356);
    int k = static_cast<int>(std::floor((winter - 2451550.09765) / 29.530588861)) - 2;
    std::vector<int> moons;
    while (true) {
        double jd = calculate_new_moon(k);
        int y, m, d;
        beijing_date_from_jd(jd, y, m, d);
        moons.push_back(gregorian_to_jdn(y, m, d));
        if (jd > winter_next + 40.0 && moons.size() > 4) break;
        k += 1;
        if (moons.size() > 40) break;
    }
    std::vector<int> qi;
    for (int year = solstice_year - 1; year < solstice_year + 3; ++year) {
        for (int i = 0; i < 12; ++i) {
            int y, m, d;
            beijing_date_from_jd(calculate_solar_term(year, PRINCIPAL_SOLAR_TERMS[i].lon, PRINCIPAL_SOLAR_TERMS[i].doy), y, m, d);
            qi.push_back(gregorian_to_jdn(y, m, d));
        }
    }
    int wy, wm, wd, ny, nm, nd;
    beijing_date_from_jd(winter, wy, wm, wd);
    beijing_date_from_jd(winter_next, ny, nm, nd);
    int first = find_lunar_month(moons, gregorian_to_jdn(wy, wm, wd));
    int last = find_lunar_month(moons, gregorian_to_jdn(ny, nm, nd));
    int span = last - first;
    int leap_at = -1;
    if (span == 13) {
        for (int offset = 0; offset < span; ++offset) {
            if (!contains_principal_term(moons[first + offset], moons[first + offset + 1], qi)) {
                leap_at = offset;
                break;
            }
        }
    }
    std::vector<LunarMonthSpan> rows;
    int number = 11;
    bool seen_first = false;
    for (int offset = 0; offset < span; ++offset) {
        bool leap = offset == leap_at;
        int label;
        if (leap) label = number == 1 ? 12 : number - 1;
        else {
            label = number;
            number = number == 12 ? 1 : number + 1;
        }
        int lunar_year;
        if (label == 1 && !leap) {
            seen_first = true;
            lunar_year = solstice_year + 1;
        } else if (seen_first) {
            lunar_year = solstice_year + 1;
        } else {
            lunar_year = solstice_year;
        }
        LunarMonthSpan row;
        row.start = moons[first + offset];
        row.end = moons[first + offset + 1];
        row.number = label;
        row.leap = leap;
        row.lunar_year = lunar_year;
        rows.push_back(row);
    }
    return rows;
}

const std::vector<LunarMonthSpan>& get_lunar_months(int solstice_year) {
    static thread_local std::map<int, std::vector<LunarMonthSpan>> cache;
    std::map<int, std::vector<LunarMonthSpan>>::iterator hit = cache.find(solstice_year);
    if (hit == cache.end()) {
        hit = cache.insert(std::make_pair(solstice_year, calculate_lunar_months(solstice_year))).first;
    }
    return hit->second;
}

std::string format_lunar_day(int day) {
    if (day == 10) return "初十";
    if (day < 10) return std::string("初") + CHINESE_DIGITS[day];
    if (day < 20) return std::string("十") + CHINESE_DIGITS[day - 10];
    if (day == 20) return "二十";
    if (day < 30) return std::string("廿") + CHINESE_DIGITS[day - 20];
    return "三十";
}

std::string format_lunar_month(int number, bool leap) {
    std::string text = std::string(leap ? "闰" : "") + LUNAR_MONTH_NAMES[number] + "月";
    return text;
}

struct LunarDay {
    int year;
    int number;
    bool leap;
    int day;
};

LunarDay calculate_lunar_date(int year, int month, int day) {
    int ord = gregorian_to_jdn(year, month, day);
    for (int sol = year - 2; sol <= year; ++sol) {
        const std::vector<LunarMonthSpan>& table = get_lunar_months(sol);
        for (size_t i = 0; i < table.size(); ++i) {
            if (table[i].start <= ord && ord < table[i].end) {
                LunarDay out;
                out.year = table[i].lunar_year;
                out.number = table[i].number;
                out.leap = table[i].leap;
                out.day = ord - table[i].start + 1;
                return out;
            }
        }
    }
    throw std::runtime_error("Unable to calculate lunar date");
}

std::string format_lunar_date(int year, int month, int day) {
    LunarDay lunar = calculate_lunar_date(year, month, day);
    char buf[32];
    std::snprintf(buf, sizeof(buf), "%d年", lunar.year);
    return std::string(buf) + format_lunar_month(lunar.number, lunar.leap) + format_lunar_day(lunar.day);
}

std::string format_solar_term_on_date(int year, int month, int day) {
    int ord = gregorian_to_jdn(year, month, day);
    for (int yy = year - 1; yy <= year + 1; ++yy) {
        for (int i = 0; i < 12; ++i) {
            double jd = calculate_solar_term(yy, MONTH_BOUNDARY_TERMS[i].lon, MONTH_BOUNDARY_TERMS[i].doy);
            int ty, tm, td, hh, mi;
            beijing_clock_from_jd(jd, ty, tm, td, hh, mi);
            if (gregorian_to_jdn(ty, tm, td) == ord) {
                char buf[32];
                std::snprintf(buf, sizeof(buf), "%s %02d:%02d", MONTH_BOUNDARY_TERMS[i].name, hh, mi);
                return buf;
            }
        }
        for (int i = 0; i < 12; ++i) {
            double jd = calculate_solar_term(yy, PRINCIPAL_SOLAR_TERMS[i].lon, PRINCIPAL_SOLAR_TERMS[i].doy);
            int ty, tm, td, hh, mi;
            beijing_clock_from_jd(jd, ty, tm, td, hh, mi);
            if (gregorian_to_jdn(ty, tm, td) == ord) {
                char buf[32];
                std::snprintf(buf, sizeof(buf), "%s %02d:%02d", PRINCIPAL_SOLAR_TERMS[i].name, hh, mi);
                return buf;
            }
        }
    }
    return "";
}

std::string format_hour_branch(int hour) {
    return std::string(EARTHLY_BRANCHES[hour_branch_index(hour)]) + "时";
}

std::string format_calendar_unchecked(int year, int month, int day, int hour, int minute) {
    double jd = beijing_time_to_jd(year, month, day, hour, minute);
    int sui = solar_year_for_instant(jd, year);
    int day_i = day_cycle_index(year, month, day);
    char clock[16];
    std::snprintf(clock, sizeof(clock), "%s %02d:%02d", format_hour_branch(hour).c_str(), hour, minute);
    std::string text = format_sexagenary_cycle(year_cycle_index(sui)) + " "
        + format_sexagenary_cycle(month_cycle_index(jd, year, sui)) + " "
        + format_sexagenary_cycle(day_i) + " "
        + format_sexagenary_cycle(hour_cycle_index(day_i % 10, hour))
        + " | " + clock
        + " | " + format_lunar_date(year, month, day);
    std::string mark = format_solar_term_on_date(year, month, day);
    if (!mark.empty()) text += " | " + mark;
    return text;
}

}  // namespace


namespace {

void validate_date_time(int year, int month, int day, int hour, int minute) {
    if (year < chinese_calendar::MIN_YEAR || year > chinese_calendar::MAX_YEAR) {
        throw std::invalid_argument("year must be between 1813 and 2100");
    }
    if (month < 1 || month > 12) throw std::invalid_argument("month must be between 1 and 12");
    const bool is_leap_year = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    const int month_lengths[] = {31, is_leap_year ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    if (day < 1 || day > month_lengths[month - 1]) throw std::invalid_argument("day does not exist in the given month");
    if (hour < 0 || hour > 23) throw std::invalid_argument("hour must be between 0 and 23");
    if (minute < 0 || minute > 59) throw std::invalid_argument("minute must be between 0 and 59");
}

}  // namespace

namespace chinese_calendar {

LunarDate get_lunar_date(int year, int month, int day) {
    validate_date_time(year, month, day, 12, 0);
    const LunarDay lunar = calculate_lunar_date(year, month, day);
    return {lunar.year, lunar.number, lunar.day, lunar.leap};
}

CalendarResult get_calendar(int year, int month, int day, int hour, int minute) {
    validate_date_time(year, month, day, hour, minute);
    const double instant = beijing_time_to_jd(year, month, day, hour, minute);
    const int solar_year = solar_year_for_instant(instant, year);
    const int day_cycle = day_cycle_index(year, month, day);
    const FourPillars pillars = {
        format_sexagenary_cycle(year_cycle_index(solar_year)),
        format_sexagenary_cycle(month_cycle_index(instant, year, solar_year)),
        format_sexagenary_cycle(day_cycle),
        format_sexagenary_cycle(hour_cycle_index(day_cycle % 10, hour)),
    };
    std::optional<SolarTerm> solar_term;
    const std::string term_text = format_solar_term_on_date(year, month, day);
    if (!term_text.empty()) {
        const auto separator = term_text.find(' ');
        solar_term = SolarTerm{term_text.substr(0, separator), term_text.substr(separator + 1)};
    }
    return {{year, month, day, hour, minute}, get_lunar_date(year, month, day), pillars, solar_term};
}

std::string format_calendar(int year, int month, int day, int hour, int minute) {
    validate_date_time(year, month, day, hour, minute);
    return format_calendar_unchecked(year, month, day, hour, minute);
}

}  // namespace chinese_calendar
