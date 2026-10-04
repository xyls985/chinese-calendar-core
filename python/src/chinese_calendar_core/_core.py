"""Internal astronomical calculations; public callers use the package API.

All instants use UTC+08:00. See docs/ALGORITHMS.md for precision and rules.
"""

from __future__ import annotations

import math

HEAVENLY_STEMS = "甲乙丙丁戊己庚辛壬癸"
EARTHLY_BRANCHES = "子丑寅卯辰巳午未申酉戌亥"
LUNAR_MONTH_NAMES = ["", "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"]

# 十二节：黄经、月序（寅月=0 … 丑月=11）、节名、大约在公历第几天
MONTH_BOUNDARY_TERMS = (
    (315.0, 0, "立春", 35),
    (345.0, 1, "惊蛰", 65),
    (15.0, 2, "清明", 95),
    (45.0, 3, "立夏", 126),
    (75.0, 4, "芒种", 157),
    (105.0, 5, "小暑", 188),
    (135.0, 6, "立秋", 220),
    (165.0, 7, "白露", 251),
    (195.0, 8, "寒露", 281),
    (225.0, 9, "立冬", 312),
    (255.0, 10, "大雪", 342),
    (285.0, 11, "小寒", 6),
)

# 十二中气，用来定闰月
PRINCIPAL_SOLAR_TERMS = (
    (270.0, "冬至", 356),
    (300.0, "大寒", 20),
    (330.0, "雨水", 50),
    (0.0, "春分", 80),
    (30.0, "谷雨", 110),
    (60.0, "小满", 141),
    (90.0, "夏至", 173),
    (120.0, "大暑", 204),
    (150.0, "处暑", 235),
    (180.0, "秋分", 266),
    (210.0, "霜降", 297),
    (240.0, "小雪", 327),
)

_SOLAR_TERM_CACHE = {}


def normalize_degrees(deg: float) -> float:
    deg = math.fmod(deg, 360.0)
    if deg < 0.0:
        deg += 360.0
    return deg


def angular_difference(a: float, b: float) -> float:
    """a - b，折到 [-180, 180)。"""
    return normalize_degrees(a - b + 180.0) - 180.0


def gregorian_to_jdn(year: int, month: int, day: int) -> int:
    """公历日期的儒略日数（当天世界时正午）。"""
    shift = (14 - month) // 12
    y = year + 4800 - shift
    m = month + 12 * shift - 3
    return day + (153 * m + 2) // 5 + 365 * y + y // 4 - y // 100 + y // 400 - 32045


def julian_day_to_components(jd: float):
    """儒略日 -> (年, 月, 日, 当日已过小时)。"""
    z = math.floor(jd + 0.5)
    frac = jd + 0.5 - z
    if z >= 2299161:
        alpha = math.floor((z - 1867216.25) / 36524.25)
        a = z + 1 + alpha - alpha // 4
    else:
        a = z
    b = a + 1524
    c = math.floor((b - 122.1) / 365.25)
    d = math.floor(365.25 * c)
    e = math.floor((b - d) / 30.6001)
    day = b - d - math.floor(30.6001 * e) + frac
    month = e - 1 if e < 14 else e - 13
    year = c - 4716 if month > 2 else c - 4715
    day_i = int(math.floor(day))
    hour = (day - day_i) * 24.0
    return int(year), int(month), day_i, hour


def beijing_date_from_jd(jd_utc: float):
    return julian_day_to_components(jd_utc + 8.0 / 24.0)[:3]


def beijing_clock_from_jd(jd_utc: float):
    year, month, day, hour = julian_day_to_components(jd_utc + 8.0 / 24.0)
    minute = int(round((hour - math.floor(hour)) * 60.0))
    hh = int(math.floor(hour))
    if minute == 60:
        minute = 0
        hh += 1
    if hh >= 24:
        year, month, day, _hour = julian_day_to_components(
            float(gregorian_to_jdn(year, month, day) + hh // 24)
        )
        hh %= 24
    return year, month, day, hh, minute


def beijing_time_to_jd(year: int, month: int, day: int, hour: int, minute: int) -> float:
    """北京时间 -> 世界时儒略日。"""
    return gregorian_to_jdn(year, month, day) - 0.5 + (hour - 8) / 24.0 + minute / 1440.0


def estimate_delta_t(year: int) -> float:
    """ΔT = TT − UT，秒。NASA 五十年多项式，1800 年之前用 1700 那段兜底。"""
    y = float(year)
    if y < 1800:
        t = y - 1700.0
        return 8.83 + 0.1603 * t - 0.0059285 * t * t + 0.00013336 * t**3 - t**4 / 1174000.0
    if y < 1860:
        t = y - 1800.0
        return (
            13.72
            - 0.332447 * t
            + 0.0068612 * t**2
            + 0.0041116 * t**3
            - 0.00037436 * t**4
            + 0.0000121272 * t**5
            - 0.0000001699 * t**6
            + 0.000000000875 * t**7
        )
    if y < 1900:
        t = y - 1860.0
        return (
            7.62
            + 0.5737 * t
            - 0.251754 * t**2
            + 0.01680668 * t**3
            - 0.0004473624 * t**4
            + t**5 / 233174.0
        )
    if y < 1920:
        t = y - 1900.0
        return -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t**3 - 0.000197 * t**4
    if y < 1941:
        t = y - 1920.0
        return 21.20 + 0.84493 * t - 0.076100 * t * t + 0.0020936 * t**3
    if y < 1961:
        t = y - 1950.0
        return 29.07 + 0.407 * t - t * t / 233.0 + t**3 / 2547.0
    if y < 1986:
        t = y - 1975.0
        return 45.45 + 1.067 * t - t * t / 260.0 - t**3 / 718.0
    if y <= 2005:
        t = y - 2000.0
        return (
            63.86
            + 0.3345 * t
            - 0.060374 * t * t
            + 0.0017275 * t**3
            + 0.000651814 * t**4
            + 0.00002373599 * t**5
        )
    if y <= 2050:
        t = y - 2000.0
        return 62.92 + 0.32217 * t + 0.005589 * t * t
    if y <= 2150:
        u = (y - 1820.0) / 100.0
        return -20.0 + 32.0 * u * u - 0.5628 * (2150.0 - y)
    u = (y - 1820.0) / 100.0
    return -20.0 + 32.0 * u * u


# 地球日心黄经（弧度）。VSOP87D 只留振幅不小于 1e-6 的项，按 t 的 0 到 4 次幂分组。
# t 是距 J2000 的儒略千年。
_EARTH_L = (
    (
        (1.75347045673, 0.00000000000, 0.00000000000),
        (0.03341656456, 4.66925680417, 6283.07584999140),
        (0.00034894275, 4.62610241759, 12566.15169998280),
        (0.00003417571, 2.82886579606, 3.52311834900),
        (0.00003497056, 2.74411800971, 5753.38488489680),
        (0.00003135896, 3.62767041758, 77713.77146812050),
        (0.00002676218, 4.41808351397, 7860.41939243920),
        (0.00002342687, 6.13516237631, 3930.20969621960),
        (0.00001273166, 2.03709655772, 529.69096509460),
        (0.00001324292, 0.74246356352, 11506.76976979360),
        (0.00000901855, 2.04505443513, 26.29831979980),
        (0.00001199167, 1.10962944315, 1577.34354244780),
        (0.00000857223, 3.50849156957, 398.14900340820),
        (0.00000779786, 1.17882652114, 5223.69391980220),
        (0.00000990250, 5.23268129594, 5884.92684658320),
        (0.00000753141, 2.53339053818, 5507.55323866740),
        (0.00000505264, 4.58292563052, 18849.22754997420),
        (0.00000492379, 4.20506639861, 775.52261132400),
        (0.00000356655, 2.91954116867, 0.06731030280),
        (0.00000284125, 1.89869034186, 796.29800681640),
        (0.00000242810, 0.34481140906, 5486.77784317500),
        (0.00000317087, 5.84901952218, 11790.62908865880),
        (0.00000271039, 0.31488607649, 10977.07880469900),
        (0.00000206160, 4.80646606059, 2544.31441988340),
        (0.00000205385, 1.86947813692, 5573.14280143310),
        (0.00000202261, 2.45767795458, 6069.77675455340),
        (0.00000126184, 1.08302630210, 20.77539549240),
        (0.00000155516, 0.83306073807, 213.29909543800),
        (0.00000115132, 0.64544911683, 0.98032106820),
        (0.00000102851, 0.63599846727, 4694.00295470760),
        (0.00000101724, 4.26679821365, 7.11354700080),
        (0.00000132212, 3.41118275555, 2942.46342329160),
        (0.00000101895, 0.97569221824, 15720.83878487840),
    ),
    (
        (6283.31966747491, 0.00000000000, 0.00000000000),
        (0.00206058863, 2.67823455584, 6283.07584999140),
        (0.00004303430, 2.63512650414, 12566.15169998280),
        (0.00000425264, 1.59046980729, 3.52311834900),
        (0.00000108977, 2.96618001993, 1577.34354244780),
        (0.00000119261, 5.79557487799, 26.29831979980),
    ),
    (
        (0.00052918870, 0.00000000000, 0.00000000000),
        (0.00008719837, 1.07209665242, 6283.07584999140),
        (0.00000309125, 0.86728818832, 12566.15169998280),
    ),
    ((0.00000289226, 5.84384198723, 6283.07584999140),),
    ((0.00000114084, 3.14159265359, 0.00000000000),),
)


def solar_longitude(jd_tt: float) -> float:
    """太阳视黄经（度）。地球黄经取 VSOP87D，再加 180°、光行差和黄经章动。"""
    t = (jd_tt - 2451545.0) / 365250.0
    lon = 0.0
    power = 1.0
    for terms in _EARTH_L:
        total = 0.0
        for amplitude, phase, freq in terms:
            total += amplitude * math.cos(phase + freq * t)
        lon += total * power
        power *= t
    lon = math.degrees(lon) + 180.0 - 20.4898 / 3600.0
    century = (jd_tt - 2451545.0) / 36525.0
    omega = math.radians(125.04452 - 1934.136261 * century + 0.0020708 * century * century)
    lon -= 17.1996 * math.sin(omega) / 3600.0
    return normalize_degrees(lon)


def calculate_solar_term(year: int, target: float, guess_doy: int) -> float:
    """某公历年里，视黄经到达 target 的世界时儒略日。"""
    key = (year, round(target * 10))
    hit = _SOLAR_TERM_CACHE.get(key)
    if hit is not None:
        return hit
    center = gregorian_to_jdn(year, 1, 1) + (guess_doy - 1) - 0.5
    lo = center - 25.0
    hi = center + 25.0
    dt = estimate_delta_t(year) / 86400.0
    for _ in range(50):
        mid = (lo + hi) / 2.0
        reached = angular_difference(solar_longitude(mid + dt), target)
        if reached < 0.0:
            lo = mid
        else:
            hi = mid
    jd = (lo + hi) / 2.0
    _SOLAR_TERM_CACHE[key] = jd
    return jd


def format_sexagenary_cycle(index: int) -> str:
    index %= 60
    return HEAVENLY_STEMS[index % 10] + EARTHLY_BRANCHES[index % 12]


def sexagenary_index(stem: int, branch: int) -> int:
    """干支配成六十甲子的序号。干支必须同奇同偶。"""
    stem %= 10
    branch %= 12
    return stem + 10 * (((stem - branch) % 12) // 2)


def day_cycle_index(year: int, month: int, day: int) -> int:
    """2024-01-01 是甲子，往后每天加一。"""
    return (gregorian_to_jdn(year, month, day) - gregorian_to_jdn(2024, 1, 1)) % 60


def solar_year_for_instant(jd_utc: float, gregorian_year: int) -> int:
    lichun = calculate_solar_term(gregorian_year, 315.0, 35)
    return gregorian_year if jd_utc >= lichun else gregorian_year - 1


def year_cycle_index(sui: int) -> int:
    return (sui - 4) % 60


def month_cycle_index(jd_utc: float, gregorian_year: int, sui: int) -> int:
    """最近一个已交的「节」决定月柱。月干从该岁的年干走五虎遁。"""
    best = None
    for year in (gregorian_year - 1, gregorian_year, gregorian_year + 1):
        for lon, slot, _name, doy in MONTH_BOUNDARY_TERMS:
            event = calculate_solar_term(year, lon, doy)
            if event <= jd_utc and (best is None or event > best[0]):
                best = (event, slot)
    slot = 10 if best is None else best[1]
    year_stem = year_cycle_index(sui) % 10
    yin_stem = (year_stem % 5) * 2 + 2
    stem = (yin_stem + slot) % 10
    branch = (2 + slot) % 12
    return sexagenary_index(stem, branch)


def hour_branch_index(hour: int) -> int:
    return ((hour + 1) // 2) % 12


def hour_cycle_index(day_stem: int, hour: int) -> int:
    slot = hour_branch_index(hour)
    zi_stem = (day_stem % 5) * 2
    stem = (zi_stem + slot) % 10
    return sexagenary_index(stem, slot)


def calculate_new_moon(k: int) -> float:
    """第 k 个朔的世界时儒略日。k=0 约在 2000-01-06。"""
    t = k / 1236.85
    jde = (
        2451550.09765
        + 29.530588861 * k
        + 0.00015437 * t * t
        - 0.000000150 * t**3
        + 0.00000000073 * t**4
    )
    earth = 1.0 - 0.002516 * t - 0.0000074 * t * t
    sun_m = math.radians(
        normalize_degrees(2.5534 + 29.10535669 * k - 0.0000218 * t * t - 0.00000011 * t**3)
    )
    moon_m = math.radians(
        normalize_degrees(
            201.5643 + 385.81693528 * k + 0.0107438 * t * t + 0.00001239 * t**3 - 0.000000058 * t**4
        )
    )
    arg_f = math.radians(
        normalize_degrees(
            160.7108 + 390.67050274 * k - 0.0016341 * t * t - 0.00000227 * t**3 + 0.000000011 * t**4
        )
    )
    omega = math.radians(
        normalize_degrees(124.7746 - 1.56375580 * k + 0.0020691 * t * t + 0.00000215 * t**3)
    )
    shift = (
        -0.40720 * math.sin(moon_m)
        + 0.17241 * earth * math.sin(sun_m)
        + 0.01608 * math.sin(2 * moon_m)
        + 0.01039 * math.sin(2 * arg_f)
        + 0.00739 * earth * math.sin(moon_m - sun_m)
        - 0.00514 * earth * math.sin(moon_m + sun_m)
        + 0.00208 * earth * earth * math.sin(2 * sun_m)
        - 0.00111 * math.sin(moon_m - 2 * arg_f)
        - 0.00057 * math.sin(moon_m + 2 * arg_f)
        + 0.00056 * earth * math.sin(2 * moon_m + sun_m)
        - 0.00042 * math.sin(3 * moon_m)
        + 0.00042 * earth * math.sin(sun_m + 2 * arg_f)
        + 0.00038 * earth * math.sin(sun_m - 2 * arg_f)
        - 0.00024 * earth * math.sin(2 * moon_m - sun_m)
        - 0.00017 * math.sin(omega)
        - 0.00007 * math.sin(moon_m + 2 * sun_m)
        + 0.00004 * math.sin(2 * moon_m - 2 * arg_f)
        + 0.00004 * math.sin(3 * sun_m)
        + 0.00003 * math.sin(moon_m + sun_m - 2 * arg_f)
        + 0.00003 * math.sin(2 * moon_m + 2 * arg_f)
        - 0.00003 * math.sin(moon_m + sun_m + 2 * arg_f)
        + 0.00003 * math.sin(moon_m - sun_m + 2 * arg_f)
        - 0.00002 * math.sin(moon_m - sun_m - 2 * arg_f)
        - 0.00002 * math.sin(3 * moon_m + sun_m)
        + 0.00002 * math.sin(4 * moon_m)
    )
    a1 = math.radians(normalize_degrees(299.77 + 0.107408 * k - 0.009173 * t * t))
    shift += 0.000325 * math.sin(a1)
    year = julian_day_to_components(jde)[0]
    return jde + shift - estimate_delta_t(year) / 86400.0


def _contains_principal_term(start_ord: int, end_ord: int, qi_ords: list) -> bool:
    return any(start_ord <= qi < end_ord for qi in qi_ords)


def calculate_lunar_months(solstice_year: int):
    """从一个冬至排到下一个冬至之前。每项是 (起始儒略日数, 月号, 是否闰, 农历年)。"""
    winter = calculate_solar_term(solstice_year, 270.0, 356)
    winter_next = calculate_solar_term(solstice_year + 1, 270.0, 356)
    k = int(math.floor((winter - 2451550.09765) / 29.530588861)) - 2
    moons = []
    while True:
        jd = calculate_new_moon(k)
        ord_ = gregorian_to_jdn(*beijing_date_from_jd(jd))
        moons.append(ord_)
        if jd > winter_next + 40.0 and len(moons) > 4:
            break
        k += 1
        if len(moons) > 40:
            break
    qi = []
    for year in range(solstice_year - 1, solstice_year + 3):
        for lon, _name, doy in PRINCIPAL_SOLAR_TERMS:
            qi.append(gregorian_to_jdn(*beijing_date_from_jd(calculate_solar_term(year, lon, doy))))
    winter_ord = gregorian_to_jdn(*beijing_date_from_jd(winter))
    next_ord = gregorian_to_jdn(*beijing_date_from_jd(winter_next))
    first = next(i for i in range(len(moons) - 1) if moons[i] <= winter_ord < moons[i + 1])
    last = next(i for i in range(len(moons) - 1) if moons[i] <= next_ord < moons[i + 1])
    span = last - first
    leap_at = -1
    if span == 13:
        for offset in range(span):
            if not _contains_principal_term(moons[first + offset], moons[first + offset + 1], qi):
                leap_at = offset
                break
    rows = []
    number = 11
    seen_first_month = False
    for offset in range(span):
        leap = offset == leap_at
        if leap:
            label = 12 if number == 1 else number - 1
        else:
            label = number
            number = 1 if number == 12 else number + 1
        if label == 1 and not leap:
            seen_first_month = True
        if label == 1 and not leap:
            lunar_year = solstice_year + 1
        elif seen_first_month:
            lunar_year = solstice_year + 1
        else:
            lunar_year = solstice_year
        rows.append((moons[first + offset], moons[first + offset + 1], label, leap, lunar_year))
    return rows


_LUNAR_MONTH_CACHE = {}


def get_lunar_months(solstice_year: int):
    hit = _LUNAR_MONTH_CACHE.get(solstice_year)
    if hit is None:
        hit = calculate_lunar_months(solstice_year)
        _LUNAR_MONTH_CACHE[solstice_year] = hit
    return hit


def format_lunar_day(day: int) -> str:
    digit = "十一二三四五六七八九"
    if day == 10:
        return "初十"
    if day < 10:
        return "初" + digit[day]
    if day < 20:
        return "十" + digit[day - 10]
    if day == 20:
        return "二十"
    if day < 30:
        return "廿" + digit[day - 20]
    return "三十"


def format_lunar_month(number: int, leap: bool) -> str:
    text = LUNAR_MONTH_NAMES[number] + "月"
    return ("闰" if leap else "") + text


def calculate_lunar_date(year: int, month: int, day: int):
    ord_ = gregorian_to_jdn(year, month, day)
    for sol in (year - 2, year - 1, year):
        for start, end, number, leap, lunar_year in get_lunar_months(sol):
            if start <= ord_ < end:
                return lunar_year, number, leap, ord_ - start + 1
    raise RuntimeError("农历排不到 %04d-%02d-%02d" % (year, month, day))


def format_lunar_date(year: int, month: int, day: int) -> str:
    lunar_year, number, leap, day_no = calculate_lunar_date(year, month, day)
    return "%d年%s%s" % (lunar_year, format_lunar_month(number, leap), format_lunar_day(day_no))


def format_solar_term_on_date(year: int, month: int, day: int) -> str:
    """这一天若交节或交中气，返回「名字 时:分」，否则空串。"""
    ord_ = gregorian_to_jdn(year, month, day)
    names = [(lon, name, doy) for lon, _slot, name, doy in MONTH_BOUNDARY_TERMS]
    names += [(lon, name, doy) for lon, name, doy in PRINCIPAL_SOLAR_TERMS]
    for yy in (year - 1, year, year + 1):
        for lon, name, doy in names:
            jd = calculate_solar_term(yy, lon, doy)
            ty, tm, td, hh, mi = beijing_clock_from_jd(jd)
            if gregorian_to_jdn(ty, tm, td) == ord_:
                return "%s %02d:%02d" % (name, hh, mi)
    return ""


def format_hour_branch(hour: int) -> str:
    """钟点落在哪个时辰。23 点仍叫子时，但是当日的晚子时。"""
    return EARTHLY_BRANCHES[hour_branch_index(hour)] + "时"


def format_calendar_unchecked(year: int, month: int, day: int, hour: int, minute: int = 0) -> str:
    jd = beijing_time_to_jd(year, month, day, hour, minute)
    sui = solar_year_for_instant(jd, year)
    day_i = day_cycle_index(year, month, day)
    parts = (
        format_sexagenary_cycle(year_cycle_index(sui)),
        format_sexagenary_cycle(month_cycle_index(jd, year, sui)),
        format_sexagenary_cycle(day_i),
        format_sexagenary_cycle(hour_cycle_index(day_i % 10, hour)),
    )
    mark = format_solar_term_on_date(year, month, day)
    text = "%s %s %s %s | %s %02d:%02d | %s" % (
        parts + (format_hour_branch(hour), hour, minute, format_lunar_date(year, month, day))
    )
    if mark:
        text += " | " + mark
    return text
