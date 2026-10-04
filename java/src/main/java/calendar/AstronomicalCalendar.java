package calendar;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

/** Internal astronomical calculations; see the public ChineseCalendar API. */
final class AstronomicalCalendar {
    private static final String[] HEAVENLY_STEMS = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] EARTHLY_BRANCHES = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final String[] LUNAR_MONTH_NAMES = {"", "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"};
    private static final String[] CHINESE_DIGITS = {"十", "一", "二", "三", "四", "五", "六", "七", "八", "九"};

    private static final double[] MONTH_BOUNDARY_LON = {315, 345, 15, 45, 75, 105, 135, 165, 195, 225, 255, 285};
    private static final int[] MONTH_BOUNDARY_SLOT = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final String[] MONTH_BOUNDARY_NAME = {
        "立春", "惊蛰", "清明", "立夏", "芒种", "小暑", "立秋", "白露", "寒露", "立冬", "大雪", "小寒"
    };
    private static final int[] MONTH_BOUNDARY_DOY = {35, 65, 95, 126, 157, 188, 220, 251, 281, 312, 342, 6};

    private static final double[] PRINCIPAL_TERM_LON = {270, 300, 330, 0, 30, 60, 90, 120, 150, 180, 210, 240};
    private static final String[] PRINCIPAL_TERM_NAME = {
        "冬至", "大寒", "雨水", "春分", "谷雨", "小满", "夏至", "大暑", "处暑", "秋分", "霜降", "小雪"
    };
    private static final int[] PRINCIPAL_TERM_DOY = {356, 20, 50, 80, 110, 141, 173, 204, 235, 266, 297, 327};

    private static final Map<String, Double> SOLAR_TERM_CACHE = new ConcurrentHashMap<String, Double>();
    private static final Map<Integer, List<LunarMonthSpan>> LUNAR_MONTH_CACHE = new ConcurrentHashMap<Integer, List<LunarMonthSpan>>();

    private AstronomicalCalendar() {
    }

    private static int floorMod(int value, int base) {
        int rem = value % base;
        return rem < 0 ? rem + base : rem;
    }

    private static double fmod(double value, double base) {
        return value - Math.floor(value / base) * base;
    }

    private static double normalizeDegrees(double deg) {
        deg = fmod(deg, 360.0);
        if (deg < 0.0) {
            deg += 360.0;
        }
        return deg;
    }

    private static double angularDifference(double a, double b) {
        return fmod(a - b + 180.0, 360.0) - 180.0;
    }

    static int gregorianToJdn(int year, int month, int day) {
        int shift = (14 - month) / 12;
        int y = year + 4800 - shift;
        int m = month + 12 * shift - 3;
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    }

    private static final class Parts {
        int year;
        int month;
        int day;
        double hour;
    }

    private static Parts julianDayToComponents(double jd) {
        double z = Math.floor(jd + 0.5);
        double frac = jd + 0.5 - z;
        double a;
        if (z >= 2299161.0) {
            double alpha = Math.floor((z - 1867216.25) / 36524.25);
            a = z + 1.0 + alpha - Math.floor(alpha / 4.0);
        } else {
            a = z;
        }
        double b = a + 1524.0;
        double c = Math.floor((b - 122.1) / 365.25);
        double d = Math.floor(365.25 * c);
        double e = Math.floor((b - d) / 30.6001);
        double day = b - d - Math.floor(30.6001 * e) + frac;
        int month = e < 14.0 ? (int) e - 1 : (int) e - 13;
        int year = month > 2 ? (int) c - 4716 : (int) c - 4715;
        int dayI = (int) Math.floor(day);
        Parts out = new Parts();
        out.year = year;
        out.month = month;
        out.day = dayI;
        out.hour = (day - dayI) * 24.0;
        return out;
    }

    private static int[] beijingDateFromJd(double jdUtc) {
        Parts p = julianDayToComponents(jdUtc + 8.0 / 24.0);
        return new int[] {p.year, p.month, p.day};
    }

    private static int[] beijingClockFromJd(double jdUtc) {
        Parts p = julianDayToComponents(jdUtc + 8.0 / 24.0);
        int minute = (int) Math.round((p.hour - Math.floor(p.hour)) * 60.0);
        int hour = (int) Math.floor(p.hour);
        if (minute == 60) {
            minute = 0;
            hour += 1;
        }
        int year = p.year;
        int month = p.month;
        int day = p.day;
        if (hour >= 24) {
            Parts next = julianDayToComponents(gregorianToJdn(year, month, day) + hour / 24);
            year = next.year;
            month = next.month;
            day = next.day;
            hour %= 24;
        }
        return new int[] {year, month, day, hour, minute};
    }

    static double beijingTimeToJd(int year, int month, int day, int hour, int minute) {
        return gregorianToJdn(year, month, day) - 0.5 + (hour - 8) / 24.0 + minute / 1440.0;
    }

    private static double estimateDeltaT(int year) {
        double y = year;
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

    private static final double[][][] EARTH_L = {
        {
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
        },
        {
            {6283.31966747491, 0.00000000000, 0.00000000000},
            {0.00206058863, 2.67823455584, 6283.07584999140},
            {0.00004303430, 2.63512650414, 12566.15169998280},
            {0.00000425264, 1.59046980729, 3.52311834900},
            {0.00000108977, 2.96618001993, 1577.34354244780},
            {0.00000119261, 5.79557487799, 26.29831979980},
        },
        {
            {0.00052918870, 0.00000000000, 0.00000000000},
            {0.00008719837, 1.07209665242, 6283.07584999140},
            {0.00000309125, 0.86728818832, 12566.15169998280},
        },
        {
            {0.00000289226, 5.84384198723, 6283.07584999140},
        },
        {
            {0.00000114084, 3.14159265359, 0.00000000000},
        },
    };

    private static double solarLongitude(double jdTt) {
        double t = (jdTt - 2451545.0) / 365250.0;
        double lon = 0.0;
        double power = 1.0;
        for (double[][] terms : EARTH_L) {
            double total = 0.0;
            for (double[] row : terms) {
                total += row[0] * Math.cos(row[1] + row[2] * t);
            }
            lon += total * power;
            power *= t;
        }
        lon = Math.toDegrees(lon) + 180.0 - 20.4898 / 3600.0;
        double century = (jdTt - 2451545.0) / 36525.0;
        double omega = Math.toRadians(125.04452 - 1934.136261 * century + 0.0020708 * century * century);
        lon -= 17.1996 * Math.sin(omega) / 3600.0;
        return normalizeDegrees(lon);
    }

    private static double calculateSolarTerm(int year, double target, int guessDoy) {
        String key = year + ":" + Math.round(target * 10.0);
        Double hit = SOLAR_TERM_CACHE.get(key);
        if (hit != null) {
            return hit.doubleValue();
        }
        double center = gregorianToJdn(year, 1, 1) + (guessDoy - 1) - 0.5;
        double lo = center - 25.0;
        double hi = center + 25.0;
        double dt = estimateDeltaT(year) / 86400.0;
        for (int i = 0; i < 50; i++) {
            double mid = (lo + hi) / 2.0;
            if (angularDifference(solarLongitude(mid + dt), target) < 0.0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double jd = (lo + hi) / 2.0;
        SOLAR_TERM_CACHE.put(key, Double.valueOf(jd));
        return jd;
    }

    static String formatSexagenaryCycle(int index) {
        index = floorMod(index, 60);
        return HEAVENLY_STEMS[index % 10] + EARTHLY_BRANCHES[index % 12];
    }

    private static int sexagenaryIndex(int stem, int branch) {
        stem = floorMod(stem, 10);
        branch = floorMod(branch, 12);
        return stem + 10 * (floorMod(stem - branch, 12) / 2);
    }

    static int dayCycleIndex(int year, int month, int day) {
        return floorMod(gregorianToJdn(year, month, day) - gregorianToJdn(2024, 1, 1), 60);
    }

    static int yearCycleIndex(int sui) {
        return floorMod(sui - 4, 60);
    }

    static int solarYearForInstant(double jdUtc, int gregorianYear) {
        double lichun = calculateSolarTerm(gregorianYear, 315.0, 35);
        return jdUtc >= lichun ? gregorianYear : gregorianYear - 1;
    }

    static int monthCycleIndex(double jdUtc, int gregorianYear, int sui) {
        double bestJd = 0.0;
        int slot = 10;
        boolean found = false;
        for (int year = gregorianYear - 1; year <= gregorianYear + 1; year++) {
            for (int i = 0; i < 12; i++) {
                double event = calculateSolarTerm(year, MONTH_BOUNDARY_LON[i], MONTH_BOUNDARY_DOY[i]);
                if (event <= jdUtc && (!found || event > bestJd)) {
                    bestJd = event;
                    slot = MONTH_BOUNDARY_SLOT[i];
                    found = true;
                }
            }
        }
        int yearStem = yearCycleIndex(sui) % 10;
        int yinStem = (yearStem % 5) * 2 + 2;
        int stem = floorMod(yinStem + slot, 10);
        int branch = floorMod(2 + slot, 12);
        return sexagenaryIndex(stem, branch);
    }

    private static int hourBranchIndex(int hour) {
        return ((hour + 1) / 2) % 12;
    }

    static int hourCycleIndex(int dayStem, int hour) {
        int slot = hourBranchIndex(hour);
        int ziStem = (dayStem % 5) * 2;
        int stem = floorMod(ziStem + slot, 10);
        return sexagenaryIndex(stem, slot);
    }

    private static double calculateNewMoon(int k) {
        double t = k / 1236.85;
        double tt = t * t;
        double t3 = tt * t;
        double t4 = t3 * t;
        double jde = 2451550.09765 + 29.530588861 * k + 0.00015437 * tt
            - 0.000000150 * t3 + 0.00000000073 * t4;
        double earth = 1.0 - 0.002516 * t - 0.0000074 * tt;
        double sunM = Math.toRadians(normalizeDegrees(2.5534 + 29.10535669 * k - 0.0000218 * tt - 0.00000011 * t3));
        double moonM = Math.toRadians(normalizeDegrees(201.5643 + 385.81693528 * k + 0.0107438 * tt
            + 0.00001239 * t3 - 0.000000058 * t4));
        double argF = Math.toRadians(normalizeDegrees(160.7108 + 390.67050274 * k - 0.0016341 * tt
            - 0.00000227 * t3 + 0.000000011 * t4));
        double omega = Math.toRadians(normalizeDegrees(124.7746 - 1.56375580 * k + 0.0020691 * tt + 0.00000215 * t3));
        double shift = -0.40720 * Math.sin(moonM)
            + 0.17241 * earth * Math.sin(sunM)
            + 0.01608 * Math.sin(2 * moonM)
            + 0.01039 * Math.sin(2 * argF)
            + 0.00739 * earth * Math.sin(moonM - sunM)
            - 0.00514 * earth * Math.sin(moonM + sunM)
            + 0.00208 * earth * earth * Math.sin(2 * sunM)
            - 0.00111 * Math.sin(moonM - 2 * argF)
            - 0.00057 * Math.sin(moonM + 2 * argF)
            + 0.00056 * earth * Math.sin(2 * moonM + sunM)
            - 0.00042 * Math.sin(3 * moonM)
            + 0.00042 * earth * Math.sin(sunM + 2 * argF)
            + 0.00038 * earth * Math.sin(sunM - 2 * argF)
            - 0.00024 * earth * Math.sin(2 * moonM - sunM)
            - 0.00017 * Math.sin(omega)
            - 0.00007 * Math.sin(moonM + 2 * sunM)
            + 0.00004 * Math.sin(2 * moonM - 2 * argF)
            + 0.00004 * Math.sin(3 * sunM)
            + 0.00003 * Math.sin(moonM + sunM - 2 * argF)
            + 0.00003 * Math.sin(2 * moonM + 2 * argF)
            - 0.00003 * Math.sin(moonM + sunM + 2 * argF)
            + 0.00003 * Math.sin(moonM - sunM + 2 * argF)
            - 0.00002 * Math.sin(moonM - sunM - 2 * argF)
            - 0.00002 * Math.sin(3 * moonM + sunM)
            + 0.00002 * Math.sin(4 * moonM);
        double a1 = Math.toRadians(normalizeDegrees(299.77 + 0.107408 * k - 0.009173 * tt));
        shift += 0.000325 * Math.sin(a1);
        int year = julianDayToComponents(jde).year;
        return jde + shift - estimateDeltaT(year) / 86400.0;
    }

    private static final class LunarMonthSpan {
        int start;
        int end;
        int number;
        boolean leap;
        int lunarYear;
    }

    private static boolean containsPrincipalTerm(int startOrd, int endOrd, List<Integer> qi) {
        for (int i = 0; i < qi.size(); i++) {
            int value = qi.get(i).intValue();
            if (startOrd <= value && value < endOrd) {
                return true;
            }
        }
        return false;
    }

    private static int findLunarMonth(List<Integer> moons, int ord) {
        for (int i = 0; i + 1 < moons.size(); i++) {
            if (moons.get(i).intValue() <= ord && ord < moons.get(i + 1).intValue()) {
                return i;
            }
        }
        throw new IllegalStateException("朔月对不上儒略日 " + ord);
    }

    private static List<LunarMonthSpan> calculateLunarMonths(int solsticeYear) {
        double winter = calculateSolarTerm(solsticeYear, 270.0, 356);
        double winterNext = calculateSolarTerm(solsticeYear + 1, 270.0, 356);
        int k = (int) Math.floor((winter - 2451550.09765) / 29.530588861) - 2;
        List<Integer> moons = new ArrayList<Integer>();
        while (true) {
            double jd = calculateNewMoon(k);
            int[] civil = beijingDateFromJd(jd);
            moons.add(Integer.valueOf(gregorianToJdn(civil[0], civil[1], civil[2])));
            if (jd > winterNext + 40.0 && moons.size() > 4) {
                break;
            }
            k += 1;
            if (moons.size() > 40) {
                break;
            }
        }
        List<Integer> qi = new ArrayList<Integer>();
        for (int year = solsticeYear - 1; year < solsticeYear + 3; year++) {
            for (int i = 0; i < 12; i++) {
                int[] civil = beijingDateFromJd(calculateSolarTerm(year, PRINCIPAL_TERM_LON[i], PRINCIPAL_TERM_DOY[i]));
                qi.add(Integer.valueOf(gregorianToJdn(civil[0], civil[1], civil[2])));
            }
        }
        int[] winterCivil = beijingDateFromJd(winter);
        int[] nextCivil = beijingDateFromJd(winterNext);
        int first = findLunarMonth(moons, gregorianToJdn(winterCivil[0], winterCivil[1], winterCivil[2]));
        int last = findLunarMonth(moons, gregorianToJdn(nextCivil[0], nextCivil[1], nextCivil[2]));
        int span = last - first;
        int leapAt = -1;
        if (span == 13) {
            for (int offset = 0; offset < span; offset++) {
                if (!containsPrincipalTerm(moons.get(first + offset).intValue(), moons.get(first + offset + 1).intValue(), qi)) {
                    leapAt = offset;
                    break;
                }
            }
        }
        List<LunarMonthSpan> rows = new ArrayList<LunarMonthSpan>();
        int number = 11;
        boolean seenFirst = false;
        for (int offset = 0; offset < span; offset++) {
            boolean leap = offset == leapAt;
            int label;
            if (leap) {
                label = number == 1 ? 12 : number - 1;
            } else {
                label = number;
                number = number == 12 ? 1 : number + 1;
            }
            int lunarYear;
            if (label == 1 && !leap) {
                seenFirst = true;
                lunarYear = solsticeYear + 1;
            } else if (seenFirst) {
                lunarYear = solsticeYear + 1;
            } else {
                lunarYear = solsticeYear;
            }
            LunarMonthSpan row = new LunarMonthSpan();
            row.start = moons.get(first + offset).intValue();
            row.end = moons.get(first + offset + 1).intValue();
            row.number = label;
            row.leap = leap;
            row.lunarYear = lunarYear;
            rows.add(row);
        }
        return rows;
    }

    private static List<LunarMonthSpan> getLunarMonths(int solsticeYear) {
        List<LunarMonthSpan> hit = LUNAR_MONTH_CACHE.get(Integer.valueOf(solsticeYear));
        if (hit == null) {
            hit = calculateLunarMonths(solsticeYear);
            LUNAR_MONTH_CACHE.put(Integer.valueOf(solsticeYear), hit);
        }
        return hit;
    }

    private static String formatLunarDay(int day) {
        if (day == 10) {
            return "初十";
        }
        if (day < 10) {
            return "初" + CHINESE_DIGITS[day];
        }
        if (day < 20) {
            return "十" + CHINESE_DIGITS[day - 10];
        }
        if (day == 20) {
            return "二十";
        }
        if (day < 30) {
            return "廿" + CHINESE_DIGITS[day - 20];
        }
        return "三十";
    }

    private static String formatLunarMonth(int number, boolean leap) {
        return (leap ? "闰" : "") + LUNAR_MONTH_NAMES[number] + "月";
    }

    static final class LunarDay {
        int year;
        int number;
        boolean leap;
        int day;
    }

    static LunarDay calculateLunarDate(int year, int month, int day) {
        int ord = gregorianToJdn(year, month, day);
        for (int sol = year - 2; sol <= year; sol++) {
            List<LunarMonthSpan> table = getLunarMonths(sol);
            for (int i = 0; i < table.size(); i++) {
                LunarMonthSpan row = table.get(i);
                if (row.start <= ord && ord < row.end) {
                    LunarDay out = new LunarDay();
                    out.year = row.lunarYear;
                    out.number = row.number;
                    out.leap = row.leap;
                    out.day = ord - row.start + 1;
                    return out;
                }
            }
        }
        throw new IllegalStateException(String.format(java.util.Locale.ROOT, "农历排不到 %04d-%02d-%02d", year, month, day));
    }

    private static String formatLunarDate(int year, int month, int day) {
        LunarDay lunar = calculateLunarDate(year, month, day);
        return lunar.year + "年" + formatLunarMonth(lunar.number, lunar.leap) + formatLunarDay(lunar.day);
    }

    static String formatSolarTermOnDate(int year, int month, int day) {
        int ord = gregorianToJdn(year, month, day);
        for (int yy = year - 1; yy <= year + 1; yy++) {
            for (int i = 0; i < 12; i++) {
                int[] clock = beijingClockFromJd(calculateSolarTerm(yy, MONTH_BOUNDARY_LON[i], MONTH_BOUNDARY_DOY[i]));
                if (gregorianToJdn(clock[0], clock[1], clock[2]) == ord) {
                    return String.format(java.util.Locale.ROOT, "%s %02d:%02d", MONTH_BOUNDARY_NAME[i], clock[3], clock[4]);
                }
            }
            for (int i = 0; i < 12; i++) {
                int[] clock = beijingClockFromJd(calculateSolarTerm(yy, PRINCIPAL_TERM_LON[i], PRINCIPAL_TERM_DOY[i]));
                if (gregorianToJdn(clock[0], clock[1], clock[2]) == ord) {
                    return String.format(java.util.Locale.ROOT, "%s %02d:%02d", PRINCIPAL_TERM_NAME[i], clock[3], clock[4]);
                }
            }
        }
        return "";
    }

    private static String formatHourBranch(int hour) {
        return EARTHLY_BRANCHES[hourBranchIndex(hour)] + "时";
    }

    static String formatCalendarUnchecked(int year, int month, int day, int hour, int minute) {
        double jd = beijingTimeToJd(year, month, day, hour, minute);
        int sui = solarYearForInstant(jd, year);
        int dayI = dayCycleIndex(year, month, day);
        String text = formatSexagenaryCycle(yearCycleIndex(sui)) + " "
            + formatSexagenaryCycle(monthCycleIndex(jd, year, sui)) + " "
            + formatSexagenaryCycle(dayI) + " "
            + formatSexagenaryCycle(hourCycleIndex(dayI % 10, hour))
            + " | " + String.format(java.util.Locale.ROOT, "%s %02d:%02d", formatHourBranch(hour), hour, minute)
            + " | " + formatLunarDate(year, month, day);
        String mark = formatSolarTermOnDate(year, month, day);
        if (mark.length() > 0) {
            text += " | " + mark;
        }
        return text;
    }

}
