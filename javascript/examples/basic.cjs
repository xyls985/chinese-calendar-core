const { getCalendar, formatCalendar } = require("../src/index.cjs");

const calendar = getCalendar(2026, 10, 4, 9, 30);
console.log(calendar.lunar);
console.log(calendar.pillars);
console.log(formatCalendar(2026, 10, 4, 9, 30));
