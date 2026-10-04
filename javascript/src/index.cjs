"use strict";

const core = require("./core.cjs");
const MIN_YEAR = 1813;
const MAX_YEAR = 2100;

function validateDateTime(year, month, day, hour, minute) {
  for (const [name, value] of Object.entries({ year, month, day, hour, minute })) {
    if (!Number.isInteger(value)) throw new TypeError(`${name} must be an integer`);
  }
  if (year < MIN_YEAR || year > MAX_YEAR) {
    throw new RangeError(`year must be between ${MIN_YEAR} and ${MAX_YEAR}`);
  }
  if (month < 1 || month > 12) throw new RangeError("month must be between 1 and 12");
  const isLeapYear = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const monthLengths = [31, isLeapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  if (day < 1 || day > monthLengths[month - 1]) throw new RangeError("day does not exist in the given month");
  if (hour < 0 || hour > 23) throw new RangeError("hour must be between 0 and 23");
  if (minute < 0 || minute > 59) throw new RangeError("minute must be between 0 and 59");
}

/** Convert a Gregorian date to a lunar date in fixed UTC+08:00. */
function getLunarDate(year, month, day) {
  validateDateTime(year, month, day, 12, 0);
  const lunar = core.calculateLunarDate(year, month, day);
  return Object.freeze({ year: lunar.year, month: lunar.number, day: lunar.day, isLeapMonth: lunar.leap });
}

/** Return structured data; solarTerm is a daily event, not necessarily already reached. */
function getCalendar(year, month, day, hour = 12, minute = 0) {
  validateDateTime(year, month, day, hour, minute);
  const instant = core.beijingTimeToJd(year, month, day, hour, minute);
  const solarYear = core.solarYearForInstant(instant, year);
  const dayCycle = core.dayCycleIndex(year, month, day);
  const termText = core.formatSolarTermOnDate(year, month, day);
  const [termName, termTime] = termText.split(" ");
  return Object.freeze({
    solar: Object.freeze({ year, month, day, hour, minute }),
    lunar: getLunarDate(year, month, day),
    pillars: Object.freeze({
      year: core.formatSexagenaryCycle(core.yearCycleIndex(solarYear)),
      month: core.formatSexagenaryCycle(core.monthCycleIndex(instant, year, solarYear)),
      day: core.formatSexagenaryCycle(dayCycle),
      hour: core.formatSexagenaryCycle(core.hourCycleIndex(dayCycle % 10, hour)),
    }),
    solarTerm: termText ? Object.freeze({ name: termName, time: termTime }) : null,
  });
}

/** Return a human-readable Chinese summary. */
function formatCalendar(year, month, day, hour = 12, minute = 0) {
  validateDateTime(year, month, day, hour, minute);
  return core.formatCalendarUnchecked(year, month, day, hour, minute);
}

module.exports = { MIN_YEAR, MAX_YEAR, getCalendar, getLunarDate, formatCalendar };
