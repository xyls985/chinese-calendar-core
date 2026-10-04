# 从单文件版本迁移

开源整理将演示、自检和内部天文计算分开，并提供明确的公共接口。

| 原位置 | 新位置 |
|---|---|
| `python/almanac.py` | `python/src/chinese_calendar_core/` |
| `js/almanac.js` | `javascript/src/index.cjs`、`index.mjs`、`core.cjs` |
| `java/Almanac.java` | `java/src/main/java/calendar/ChineseCalendar.java` 及内部实现 |
| `cpp/almanac.cpp` | `cpp/include/chinese_calendar/calendar.hpp`、`cpp/src/calendar.cpp` |

## 调用方式

- Python：`reading(...)` 改为 `format_calendar(...)`，从 `chinese_calendar_core` 导入；需要数据时使用 `get_calendar(...)`。
- JavaScript：通过 CommonJS 或 ESM 导入 `formatCalendar(...)` / `getCalendar(...)`。不再直接执行整个源文件来获得内部函数。
- Java：调用 `calendar.ChineseCalendar.formatCalendar(...)` / `getCalendar(...)`，不依赖默认包里的包级方法。
- C++：包含公共头文件并链接库，调用 `chinese_calendar::format_calendar(...)` / `get_calendar(...)`，不再包含带 `main` 的实现文件。
- 只查农历时使用 `get_lunar_date` / `getLunarDate`，返回明确字段的数据对象，不依赖内部 tuple、数组或 `number` 等旧字段名称。

## 行为变化

- 无效日期、小时、分钟、非整数及超范围年份现在会报错，不再静默归一化。
- 查询时间省略时默认 12:00；Java 通过三参数重载提供默认值。
- 导入库不会执行自检或打印演示。
- Java 输出固定数字格式；Java、C++ 缓存不再进行无同步的共享 Map 写入。
- 内部数学函数和常量改用描述性名称，内部实现不属于公共兼容接口。

有效输入的已有中文格式化结果保持兼容，天文算法的近似精度及历史规则差异仍然存在。它们的详细说明见 [ALGORITHMS.md](ALGORITHMS.md)。

原先依赖本机外部文件的商业软件对照脚本不随开源目录发布，替换为独立的共享样例与公开资料核验说明。
