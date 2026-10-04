#!/usr/bin/env node
"use strict";

const { formatCalendar } = require("../src/index.cjs");
const arguments_ = process.argv.slice(2);
if (arguments_.length < 3 || arguments_.length > 5 || arguments_.some(value => !/^-?\d+$/.test(value))) {
  console.error("Usage: chinese-calendar YEAR MONTH DAY [HOUR [MINUTE]] (UTC+08:00)");
  process.exitCode = 2;
} else {
  try {
    console.log(formatCalendar(...arguments_.map(Number)));
  } catch (error) {
    console.error(error.message);
    process.exitCode = 2;
  }
}
