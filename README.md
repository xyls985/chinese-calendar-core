# Chinese Calendar Core

固定 UTC+08:00 的农历、二十四节气与四柱计算库，提供 Python、JavaScript、Java 和 C++ 四种实现。运行时不依赖网络服务或第三方历算包。

[English](README.en.md) · [API 文档](docs/API.md) · [算法与精度](docs/ALGORITHMS.md) · [测试与核验](docs/VALIDATION.md) · [贡献指南](CONTRIBUTING.md)

## 功能与边界

- 公历转农历：农历年、月、日及闰月标记。
- 四柱：年柱按立春时刻切换，月柱按十二节切换，日柱在零点切换。
- 时柱采用当日晚子时：23:00–23:59 使用当日日干。
- 查询当天的节气名称和近似时刻。
- 同时提供结构化数据与中文格式化文本；非法日期、时间及超出范围的年份会报错。

输入范围为 **1813–2100 年**。所有输入都是 UTC+08:00 的当地钟表时间，不转换系统时区，也不使用夏令时或真太阳时。

**节气使用近似天文算法，不能保证交节附近每一分钟的四柱与高精度星历一致。1929 年以前采用现代规则重算，可能与当年颁行的历史历日不同。** 这些限制及具体案例见[算法与精度](docs/ALGORITHMS.md)。

## 快速开始

以下命令分别从仓库根目录执行。当前文档以源码安装为准，不假定已发布到任何包仓库。

### Python 3.10+

```sh
python -m pip install ./python
python -m chinese_calendar_core 2026 10 4 9 30
```

```python
from chinese_calendar_core import get_calendar, get_lunar_date, format_calendar

result = get_calendar(2026, 10, 4, 9, 30)
print(result.lunar)         # LunarDate(year=2026, month=8, day=24, is_leap_month=False)
print(result.pillars.year)  # 丙午
print(get_lunar_date(2033, 12, 22).is_leap_month)  # True
print(format_calendar(2026, 10, 4, 9, 30))
```

Python 模块名为 `chinese_calendar_core`，避免与标准库 `calendar` 及其他同名日历模块混淆。

### JavaScript / Node.js 18+

```sh
node javascript/examples/basic.cjs
node javascript/examples/basic.mjs
node javascript/bin/calendar.cjs 2026 10 4 9 30
```

从仓库根目录使用 CommonJS：

```javascript
const { getCalendar, formatCalendar } = require("./javascript");

const result = getCalendar(2026, 10, 4, 9, 30);
console.log(result.lunar);        // { year: 2026, month: 8, day: 24, isLeapMonth: false }
console.log(result.pillars.year); // 丙午
console.log(formatCalendar(2026, 10, 4, 9, 30));
```

也支持 ESM 和 TypeScript 类型声明。导入模块不会运行演示或打印内容。在其他 Node 项目中，可通过 `npm install ../calendar/javascript` 安装本地源码包，随后使用包名 `chinese-calendar-core`。浏览器项目需要构建工具处理模块格式。

### Java

使用 JDK 17+ 构建，生成的类兼容 Java 8+：

```sh
python scripts/test_all.py --languages java
javac -encoding UTF-8 -cp .build/tests/java -d .build/tests/java java/examples/CalendarExample.java
java '-Dfile.encoding=UTF-8' -cp .build/tests/java CalendarExample
```

```java
import calendar.ChineseCalendar;

ChineseCalendar.CalendarResult result = ChineseCalendar.getCalendar(2026, 10, 4, 9, 30);
System.out.println(result.lunar.month); // 8
System.out.println(ChineseCalendar.formatCalendar(2026, 10, 4, 9, 30));
```

Maven 用户可运行 `mvn -f java/pom.xml package`，产物为 `java/target/chinese-calendar-core-0.1.0.jar`。PowerShell 中请把 JVM 参数写成 `'-Dfile.encoding=UTF-8'`。

### C++17

```sh
cmake -S cpp -B .build/cpp -DCMAKE_BUILD_TYPE=Release
cmake --build .build/cpp --config Release
ctest --test-dir .build/cpp -C Release --output-on-failure
```

```cpp
#include <chinese_calendar/calendar.hpp>

auto result = chinese_calendar::get_calendar(2026, 10, 4, 9, 30);
auto text = chinese_calendar::format_calendar(2026, 10, 4, 9, 30);
```

通过 CMake `add_subdirectory` 或安装后的 `find_package(ChineseCalendar CONFIG REQUIRED)` 集成，链接目标为 `ChineseCalendar::chinese_calendar`。具体步骤见 [C++ 说明](cpp/README.md)。

## 统一返回含义

四种实现提供同样的三类操作；命名遵循各语言惯例。

| 操作 | Python / C++ | JavaScript / Java |
|---|---|---|
| 完整结构化结果 | `get_calendar` | `getCalendar` |
| 只查农历 | `get_lunar_date` | `getLunarDate` |
| 中文文本 | `format_calendar` | `formatCalendar` |

省略时间时默认 **12:00**。农历年以正月初一切换，年柱以立春切换，因此二者可能处于不同年份。`solar_term` / `solarTerm` 表示这一天的节气，不代表输入时刻已经交节；无节气时返回空值。完整字段与错误类型见 [API 文档](docs/API.md)。

格式化示例：

```text
丙午 丁酉 辛亥 癸巳 | 巳时 09:30 | 2026年八月廿四
```

## 测试

安装好 Python、Node、JDK 17 和 g++ 后运行：

```sh
python scripts/test_all.py
```

可用 `--languages python javascript` 选择实现，用 `--java-home` 指定 JDK，或用 `--cxx` 指定编译器。测试使用标准库，无需下载测试框架。仓库包含 1,008 条共享回归样例、非法输入、导入行为及结构化接口测试；GitHub Actions 提供持续集成配置。

历史审核还覆盖了香港天文台 73,049 个日期、四语言各 165,257 个时刻及 NASA、中央气象署的天文数据。**算法正确性核验与重构回归不是同一件事**，数据范围、结论及限制见 [VALIDATION.md](docs/VALIDATION.md)。

## 目录

```text
python/       Python 包、命令行入口、示例与测试
javascript/   CommonJS / ESM 包、类型声明、示例与测试
java/         标准 Java 源码目录、Maven 配置与示例
cpp/          公共头文件、实现、CMake 配置与示例
tests/        四种实现共用的回归样例
scripts/      跨语言测试入口
docs/         API、算法、核验记录与迁移说明
.github/      持续集成配置
```

## 许可与参考

代码采用 [MIT License](LICENSE)。算法参考和校验资料出处见 [REFERENCES.md](docs/REFERENCES.md)。参考网站提供的数据不因本项目采用 MIT 而改变其原有许可。
