"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");
const { getCalendar, getLunarDate, formatCalendar } = require("..");
const root = path.resolve(__dirname, "../..");

test("matches all shared regression fixtures", () => {
  let count = 0;
  for (const line of fs.readFileSync(path.join(root, "tests/fixtures/readings.txt"), "utf8").split(/\r?\n/)) {
    if (!line || line.startsWith("#")) continue;
    const [arguments_, expected] = line.split("\t");
    assert.equal(formatCalendar(...arguments_.split(" ").map(Number)), expected, arguments_);
    count++;
  }
  assert.ok(count >= 1000);
});

test("rejects invalid dates, times, and non-integer values", () => {
  for (const line of fs.readFileSync(path.join(root, "tests/fixtures/invalid_dates.txt"), "utf8").split(/\r?\n/)) {
    if (!line || line.startsWith("#")) continue;
    const arguments_ = line.split(" ").map(Number);
    assert.throws(() => getCalendar(...arguments_), RangeError);
    assert.throws(() => formatCalendar(...arguments_), RangeError);
  }
  assert.throws(() => getLunarDate(2023, 2, 29), RangeError);
  for (const value of [true, "2024", null, NaN, Infinity, 2024.5]) {
    assert.throws(() => getCalendar(value, 1, 1), TypeError);
  }
});

test("returns structured immutable data and documented defaults", () => {
  const result = getCalendar(2024, 2, 10, 8);
  assert.deepEqual(result.lunar, { year: 2024, month: 1, day: 1, isLeapMonth: false });
  assert.equal(result.pillars.hour, "戊辰");
  assert.equal(result.solarTerm, null);
  assert.throws(() => { result.lunar.day = 2; }, TypeError);
  assert.equal(getLunarDate(2033, 12, 22).isLeapMonth, true);
  assert.deepEqual(getCalendar(2024, 2, 4).solarTerm, { name: "立春", time: "16:27" });
  assert.equal(getCalendar(2024, 2, 10).solar.hour, 12);
  assert.equal(formatCalendar(2024, 2, 10), formatCalendar(2024, 2, 10, 12, 0));
});

test("CommonJS import is silent and ESM exports match", async () => {
  const process_ = spawnSync(process.execPath, ["-e", "require('./src/index.cjs')"],
                            { cwd: path.join(root, "javascript"), encoding: "utf8" });
  assert.equal(process_.status, 0);
  assert.equal(process_.stdout, "");
  assert.equal(process_.stderr, "");
  const esm = await import("../src/index.mjs");
  assert.equal(esm.formatCalendar(2024, 2, 10), formatCalendar(2024, 2, 10));
});

test("CLI returns output or an actionable input error", () => {
  const cli = path.join(root, "javascript/bin/calendar.cjs");
  const valid = spawnSync(process.execPath, [cli, "2024", "2", "10"], { encoding: "utf8" });
  assert.equal(valid.status, 0);
  assert.equal(valid.stdout.trim(), formatCalendar(2024, 2, 10));
  const invalid = spawnSync(process.execPath, [cli, "2023", "2", "29"], { encoding: "utf8" });
  assert.equal(invalid.status, 2);
});
