// Internal astronomical calculations. See docs/ALGORITHMS.md.
"use strict";

const HEAVENLY_STEMS = ["甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"];
const EARTHLY_BRANCHES = ["子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"];
const LUNAR_MONTH_NAMES = ["", "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"];
const CHINESE_DIGITS = ["十", "一", "二", "三", "四", "五", "六", "七", "八", "九"];

const MONTH_BOUNDARY_TERMS = [
  [315, 0, "立春", 35],
  [345, 1, "惊蛰", 65],
  [15, 2, "清明", 95],
  [45, 3, "立夏", 126],
  [75, 4, "芒种", 157],
  [105, 5, "小暑", 188],
  [135, 6, "立秋", 220],
  [165, 7, "白露", 251],
  [195, 8, "寒露", 281],
  [225, 9, "立冬", 312],
  [255, 10, "大雪", 342],
  [285, 11, "小寒", 6],
];

const PRINCIPAL_SOLAR_TERMS = [
  [270, "冬至", 356],
  [300, "大寒", 20],
  [330, "雨水", 50],
  [0, "春分", 80],
  [30, "谷雨", 110],
  [60, "小满", 141],
  [90, "夏至", 173],
  [120, "大暑", 204],
  [150, "处暑", 235],
  [180, "秋分", 266],
  [210, "霜降", 297],
  [240, "小雪", 327],
];

const solarTermCache = new Map();
const lunarMonthCache = new Map();

function floorMod(value, base) {
  return ((value % base) + base) % base;
}

function fmod(value, base) {
  return value - Math.floor(value / base) * base;
}

function normalizeDegrees(deg) {
  deg = fmod(deg, 360);
  if (deg < 0) deg += 360;
  return deg;
}

function angularDifference(a, b) {
  return fmod(a - b + 180, 360) - 180;
}

function gregorianToJdn(year, month, day) {
  const shift = Math.floor((14 - month) / 12);
  const y = year + 4800 - shift;
  const m = month + 12 * shift - 3;
  return day + Math.floor((153 * m + 2) / 5) + 365 * y + Math.floor(y / 4)
    - Math.floor(y / 100) + Math.floor(y / 400) - 32045;
}

function julianDayToComponents(jd) {
  const z = Math.floor(jd + 0.5);
  const frac = jd + 0.5 - z;
  let a;
  if (z >= 2299161) {
    const alpha = Math.floor((z - 1867216.25) / 36524.25);
    a = z + 1 + alpha - Math.floor(alpha / 4);
  } else {
    a = z;
  }
  const b = a + 1524;
  const c = Math.floor((b - 122.1) / 365.25);
  const d = Math.floor(365.25 * c);
  const e = Math.floor((b - d) / 30.6001);
  const day = b - d - Math.floor(30.6001 * e) + frac;
  const month = e < 14 ? e - 1 : e - 13;
  const year = month > 2 ? c - 4716 : c - 4715;
  const dayI = Math.floor(day);
  return { year, month, day: dayI, hour: (day - dayI) * 24 };
}

function beijingDateFromJd(jdUtc) {
  const p = julianDayToComponents(jdUtc + 8 / 24);
  return [p.year, p.month, p.day];
}

function beijingClockFromJd(jdUtc) {
  const p = julianDayToComponents(jdUtc + 8 / 24);
  let minute = Math.round((p.hour - Math.floor(p.hour)) * 60);
  let hour = Math.floor(p.hour);
  if (minute === 60) {
    minute = 0;
    hour += 1;
  }
  let year = p.year;
  let month = p.month;
  let day = p.day;
  if (hour >= 24) {
    const next = julianDayToComponents(gregorianToJdn(year, month, day) + Math.floor(hour / 24));
    year = next.year;
    month = next.month;
    day = next.day;
    hour %= 24;
  }
  return [year, month, day, hour, minute];
}

function beijingTimeToJd(year, month, day, hour, minute) {
  return gregorianToJdn(year, month, day) - 0.5 + (hour - 8) / 24 + minute / 1440;
}

function estimateDeltaT(year) {
  const y = year;
  if (y < 1800) {
    const t = y - 1700;
    return 8.83 + 0.1603 * t - 0.0059285 * t * t + 0.00013336 * t ** 3 - t ** 4 / 1174000;
  }
  if (y < 1860) {
    const t = y - 1800;
    return 13.72 - 0.332447 * t + 0.0068612 * t ** 2 + 0.0041116 * t ** 3
      - 0.00037436 * t ** 4 + 0.0000121272 * t ** 5
      - 0.0000001699 * t ** 6 + 0.000000000875 * t ** 7;
  }
  if (y < 1900) {
    const t = y - 1860;
    return 7.62 + 0.5737 * t - 0.251754 * t ** 2 + 0.01680668 * t ** 3
      - 0.0004473624 * t ** 4 + t ** 5 / 233174;
  }
  if (y > 2005 && y <= 2050) {
    const t = y - 2000;
    return 62.92 + 0.32217 * t + 0.005589 * t * t;
  }
  if (y >= 1986 && y <= 2005) {
    const t = y - 2000;
    return 63.86 + 0.3345 * t - 0.060374 * t * t + 0.0017275 * t ** 3
      + 0.000651814 * t ** 4 + 0.00002373599 * t ** 5;
  }
  if (y >= 1961 && y < 1986) {
    const t = y - 1975;
    return 45.45 + 1.067 * t - t * t / 260 - t ** 3 / 718;
  }
  if (y >= 1941 && y < 1961) {
    const t = y - 1950;
    return 29.07 + 0.407 * t - t * t / 233 + t ** 3 / 2547;
  }
  if (y >= 1920 && y < 1941) {
    const t = y - 1920;
    return 21.2 + 0.84493 * t - 0.0761 * t * t + 0.0020936 * t ** 3;
  }
  if (y >= 1900 && y < 1920) {
    const t = y - 1900;
    return -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t ** 3 - 0.000197 * t ** 4;
  }
  if (y <= 2150) {
    const u = (y - 1820) / 100;
    return -20 + 32 * u * u - 0.5628 * (2150 - y);
  }
  const u = (y - 1820) / 100;
  return -20 + 32 * u * u;
}

const EARTH_L = [
  [
    [1.75347045673, 0.00000000000, 0.00000000000],
    [0.03341656456, 4.66925680417, 6283.07584999140],
    [0.00034894275, 4.62610241759, 12566.15169998280],
    [0.00003417571, 2.82886579606, 3.52311834900],
    [0.00003497056, 2.74411800971, 5753.38488489680],
    [0.00003135896, 3.62767041758, 77713.77146812050],
    [0.00002676218, 4.41808351397, 7860.41939243920],
    [0.00002342687, 6.13516237631, 3930.20969621960],
    [0.00001273166, 2.03709655772, 529.69096509460],
    [0.00001324292, 0.74246356352, 11506.76976979360],
    [0.00000901855, 2.04505443513, 26.29831979980],
    [0.00001199167, 1.10962944315, 1577.34354244780],
    [0.00000857223, 3.50849156957, 398.14900340820],
    [0.00000779786, 1.17882652114, 5223.69391980220],
    [0.00000990250, 5.23268129594, 5884.92684658320],
    [0.00000753141, 2.53339053818, 5507.55323866740],
    [0.00000505264, 4.58292563052, 18849.22754997420],
    [0.00000492379, 4.20506639861, 775.52261132400],
    [0.00000356655, 2.91954116867, 0.06731030280],
    [0.00000284125, 1.89869034186, 796.29800681640],
    [0.00000242810, 0.34481140906, 5486.77784317500],
    [0.00000317087, 5.84901952218, 11790.62908865880],
    [0.00000271039, 0.31488607649, 10977.07880469900],
    [0.00000206160, 4.80646606059, 2544.31441988340],
    [0.00000205385, 1.86947813692, 5573.14280143310],
    [0.00000202261, 2.45767795458, 6069.77675455340],
    [0.00000126184, 1.08302630210, 20.77539549240],
    [0.00000155516, 0.83306073807, 213.29909543800],
    [0.00000115132, 0.64544911683, 0.98032106820],
    [0.00000102851, 0.63599846727, 4694.00295470760],
    [0.00000101724, 4.26679821365, 7.11354700080],
    [0.00000132212, 3.41118275555, 2942.46342329160],
    [0.00000101895, 0.97569221824, 15720.83878487840],
  ],
  [
    [6283.31966747491, 0.00000000000, 0.00000000000],
    [0.00206058863, 2.67823455584, 6283.07584999140],
    [0.00004303430, 2.63512650414, 12566.15169998280],
    [0.00000425264, 1.59046980729, 3.52311834900],
    [0.00000108977, 2.96618001993, 1577.34354244780],
    [0.00000119261, 5.79557487799, 26.29831979980],
  ],
  [
    [0.00052918870, 0.00000000000, 0.00000000000],
    [0.00008719837, 1.07209665242, 6283.07584999140],
    [0.00000309125, 0.86728818832, 12566.15169998280],
  ],
  [
    [0.00000289226, 5.84384198723, 6283.07584999140],
  ],
  [
    [0.00000114084, 3.14159265359, 0.00000000000],
  ],
];

function solarLongitude(jdTt) {
  const t = (jdTt - 2451545) / 365250;
  let lon = 0;
  let power = 1;
  for (const terms of EARTH_L) {
    let total = 0;
    for (const row of terms) total += row[0] * Math.cos(row[1] + row[2] * t);
    lon += total * power;
    power *= t;
  }
  lon = lon * 180 / Math.PI + 180 - 20.4898 / 3600;
  const century = (jdTt - 2451545) / 36525;
  const omega = (125.04452 - 1934.136261 * century + 0.0020708 * century * century) * Math.PI / 180;
  lon -= 17.1996 * Math.sin(omega) / 3600;
  return normalizeDegrees(lon);
}

function calculateSolarTerm(year, target, guessDoy) {
  const key = year + ":" + Math.round(target * 10);
  if (solarTermCache.has(key)) return solarTermCache.get(key);
  const center = gregorianToJdn(year, 1, 1) + (guessDoy - 1) - 0.5;
  let lo = center - 25;
  let hi = center + 25;
  const dt = estimateDeltaT(year) / 86400;
  for (let i = 0; i < 50; i++) {
    const mid = (lo + hi) / 2;
    if (angularDifference(solarLongitude(mid + dt), target) < 0) lo = mid;
    else hi = mid;
  }
  const jd = (lo + hi) / 2;
  solarTermCache.set(key, jd);
  return jd;
}

function formatSexagenaryCycle(index) {
  index = floorMod(index, 60);
  return HEAVENLY_STEMS[index % 10] + EARTHLY_BRANCHES[index % 12];
}

function sexagenaryIndex(stem, branch) {
  stem = floorMod(stem, 10);
  branch = floorMod(branch, 12);
  return stem + 10 * Math.floor(floorMod(stem - branch, 12) / 2);
}

function dayCycleIndex(year, month, day) {
  return floorMod(gregorianToJdn(year, month, day) - gregorianToJdn(2024, 1, 1), 60);
}

function yearCycleIndex(sui) {
  return floorMod(sui - 4, 60);
}

function solarYearForInstant(jdUtc, gregorianYear) {
  const lichun = calculateSolarTerm(gregorianYear, 315, 35);
  return jdUtc >= lichun ? gregorianYear : gregorianYear - 1;
}

function monthCycleIndex(jdUtc, gregorianYear, sui) {
  let bestJd = 0;
  let slot = 10;
  let found = false;
  for (let year = gregorianYear - 1; year <= gregorianYear + 1; year++) {
    for (const row of MONTH_BOUNDARY_TERMS) {
      const event = calculateSolarTerm(year, row[0], row[3]);
      if (event <= jdUtc && (!found || event > bestJd)) {
        bestJd = event;
        slot = row[1];
        found = true;
      }
    }
  }
  const yearStem = yearCycleIndex(sui) % 10;
  const yinStem = (yearStem % 5) * 2 + 2;
  const stem = floorMod(yinStem + slot, 10);
  const branch = floorMod(2 + slot, 12);
  return sexagenaryIndex(stem, branch);
}

function hourBranchIndex(hour) {
  return Math.floor((hour + 1) / 2) % 12;
}

function hourCycleIndex(dayStem, hour) {
  const slot = hourBranchIndex(hour);
  const ziStem = (dayStem % 5) * 2;
  const stem = floorMod(ziStem + slot, 10);
  return sexagenaryIndex(stem, slot);
}

function calculateNewMoon(k) {
  const t = k / 1236.85;
  const tt = t * t;
  const t3 = tt * t;
  const t4 = t3 * t;
  const jde = 2451550.09765 + 29.530588861 * k + 0.00015437 * tt - 0.00000015 * t3 + 0.00000000073 * t4;
  const earth = 1 - 0.002516 * t - 0.0000074 * tt;
  const sunM = normalizeDegrees(2.5534 + 29.10535669 * k - 0.0000218 * tt - 0.00000011 * t3) * Math.PI / 180;
  const moonM = normalizeDegrees(201.5643 + 385.81693528 * k + 0.0107438 * tt + 0.00001239 * t3 - 0.000000058 * t4) * Math.PI / 180;
  const argF = normalizeDegrees(160.7108 + 390.67050274 * k - 0.0016341 * tt - 0.00000227 * t3 + 0.000000011 * t4) * Math.PI / 180;
  const omega = normalizeDegrees(124.7746 - 1.5637558 * k + 0.0020691 * tt + 0.00000215 * t3) * Math.PI / 180;
  let shift = -0.4072 * Math.sin(moonM)
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
  const a1 = normalizeDegrees(299.77 + 0.107408 * k - 0.009173 * tt) * Math.PI / 180;
  shift += 0.000325 * Math.sin(a1);
  const year = julianDayToComponents(jde).year;
  return jde + shift - estimateDeltaT(year) / 86400;
}

function containsPrincipalTerm(startOrd, endOrd, qi) {
  return qi.some((value) => startOrd <= value && value < endOrd);
}

function findLunarMonth(moons, ord) {
  for (let i = 0; i + 1 < moons.length; i++) {
    if (moons[i] <= ord && ord < moons[i + 1]) return i;
  }
  throw new Error("朔月对不上儒略日 " + ord);
}

function calculateLunarMonths(solsticeYear) {
  const winter = calculateSolarTerm(solsticeYear, 270, 356);
  const winterNext = calculateSolarTerm(solsticeYear + 1, 270, 356);
  let k = Math.floor((winter - 2451550.09765) / 29.530588861) - 2;
  const moons = [];
  while (true) {
    const jd = calculateNewMoon(k);
    const civil = beijingDateFromJd(jd);
    moons.push(gregorianToJdn(civil[0], civil[1], civil[2]));
    if (jd > winterNext + 40 && moons.length > 4) break;
    k += 1;
    if (moons.length > 40) break;
  }
  const qi = [];
  for (let year = solsticeYear - 1; year < solsticeYear + 3; year++) {
    for (const row of PRINCIPAL_SOLAR_TERMS) {
      const civil = beijingDateFromJd(calculateSolarTerm(year, row[0], row[2]));
      qi.push(gregorianToJdn(civil[0], civil[1], civil[2]));
    }
  }
  const winterCivil = beijingDateFromJd(winter);
  const nextCivil = beijingDateFromJd(winterNext);
  const first = findLunarMonth(moons, gregorianToJdn(winterCivil[0], winterCivil[1], winterCivil[2]));
  const last = findLunarMonth(moons, gregorianToJdn(nextCivil[0], nextCivil[1], nextCivil[2]));
  const span = last - first;
  let leapAt = -1;
  if (span === 13) {
    for (let offset = 0; offset < span; offset++) {
      if (!containsPrincipalTerm(moons[first + offset], moons[first + offset + 1], qi)) {
        leapAt = offset;
        break;
      }
    }
  }
  const rows = [];
  let number = 11;
  let seenFirst = false;
  for (let offset = 0; offset < span; offset++) {
    const leap = offset === leapAt;
    let label;
    if (leap) label = number === 1 ? 12 : number - 1;
    else {
      label = number;
      number = number === 12 ? 1 : number + 1;
    }
    let lunarYear;
    if (label === 1 && !leap) {
      seenFirst = true;
      lunarYear = solsticeYear + 1;
    } else if (seenFirst) {
      lunarYear = solsticeYear + 1;
    } else {
      lunarYear = solsticeYear;
    }
    rows.push({
      start: moons[first + offset],
      end: moons[first + offset + 1],
      number: label,
      leap,
      lunarYear,
    });
  }
  return rows;
}

function getLunarMonths(solsticeYear) {
  if (!lunarMonthCache.has(solsticeYear)) lunarMonthCache.set(solsticeYear, calculateLunarMonths(solsticeYear));
  return lunarMonthCache.get(solsticeYear);
}

function formatLunarDay(day) {
  if (day === 10) return "初十";
  if (day < 10) return "初" + CHINESE_DIGITS[day];
  if (day < 20) return "十" + CHINESE_DIGITS[day - 10];
  if (day === 20) return "二十";
  if (day < 30) return "廿" + CHINESE_DIGITS[day - 20];
  return "三十";
}

function formatLunarMonth(number, leap) {
  return (leap ? "闰" : "") + LUNAR_MONTH_NAMES[number] + "月";
}

function calculateLunarDate(year, month, day) {
  const ord = gregorianToJdn(year, month, day);
  for (let sol = year - 2; sol <= year; sol++) {
    for (const row of getLunarMonths(sol)) {
      if (row.start <= ord && ord < row.end) {
        return { year: row.lunarYear, number: row.number, leap: row.leap, day: ord - row.start + 1 };
      }
    }
  }
  throw new Error("农历排不到 " + year + "-" + month + "-" + day);
}

function formatLunarDate(year, month, day) {
  const lunar = calculateLunarDate(year, month, day);
  return lunar.year + "年" + formatLunarMonth(lunar.number, lunar.leap) + formatLunarDay(lunar.day);
}

function formatSolarTermOnDate(year, month, day) {
  const ord = gregorianToJdn(year, month, day);
  for (let yy = year - 1; yy <= year + 1; yy++) {
    for (const row of MONTH_BOUNDARY_TERMS) {
      const clock = beijingClockFromJd(calculateSolarTerm(yy, row[0], row[3]));
      if (gregorianToJdn(clock[0], clock[1], clock[2]) === ord) {
        return row[2] + " " + pad(clock[3]) + ":" + pad(clock[4]);
      }
    }
    for (const row of PRINCIPAL_SOLAR_TERMS) {
      const clock = beijingClockFromJd(calculateSolarTerm(yy, row[0], row[2]));
      if (gregorianToJdn(clock[0], clock[1], clock[2]) === ord) {
        return row[1] + " " + pad(clock[3]) + ":" + pad(clock[4]);
      }
    }
  }
  return "";
}

function formatHourBranch(hour) {
  return EARTHLY_BRANCHES[hourBranchIndex(hour)] + "时";
}

function formatCalendarUnchecked(year, month, day, hour, minute) {
  const jd = beijingTimeToJd(year, month, day, hour, minute);
  const sui = solarYearForInstant(jd, year);
  const dayI = dayCycleIndex(year, month, day);
  let text = [
    formatSexagenaryCycle(yearCycleIndex(sui)),
    formatSexagenaryCycle(monthCycleIndex(jd, year, sui)),
    formatSexagenaryCycle(dayI),
    formatSexagenaryCycle(hourCycleIndex(dayI % 10, hour)),
  ].join(" ") + " | " + formatHourBranch(hour) + " " + pad(hour) + ":" + pad(minute)
    + " | " + formatLunarDate(year, month, day);
  const mark = formatSolarTermOnDate(year, month, day);
  if (mark) text += " | " + mark;
  return text;
}

function pad(n) {
  return String(n).padStart(2, "0");
}

module.exports = { beijingTimeToJd, solarYearForInstant, dayCycleIndex, yearCycleIndex, monthCycleIndex, hourCycleIndex, formatSexagenaryCycle, calculateLunarDate, formatSolarTermOnDate, formatCalendarUnchecked };
