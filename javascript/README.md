# Chinese Calendar Core for JavaScript

Node.js 18+，无运行时依赖，支持 CommonJS、ESM 和 TypeScript 类型声明。

在你的项目里安装本地源码包：

```sh
npm install ../calendar/javascript
```

```javascript
const { getCalendar, getLunarDate, formatCalendar } = require("chinese-calendar-core");
console.log(getCalendar(2026, 10, 4, 9, 30).lunar);
console.log(formatCalendar(2026, 10, 4, 9, 30));
```

```javascript
import { getCalendar } from "chinese-calendar-core";
console.log(getCalendar(2026, 10, 4, 9, 30));
```

导入没有打印或自检副作用。CLI：`node bin/calendar.cjs 2026 10 4 9 30`。从源码仓库运行测试：`npm test`。

默认时间为 12:00，固定 UTC+08:00，有效范围 1813–2100。非整数输入抛出 TypeError，非法日期/时间或超范围年份抛出 RangeError。

节气采用近似天文算法，交节附近的分钟级四柱不保证与高精度星历相同；1929 年前存在历史历法兼容性差异。详细规则见源码仓库的 `docs/`。

MIT License。此说明以本地源码安装为准，不假定已在 npm 发布。
