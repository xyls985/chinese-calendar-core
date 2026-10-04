# API

所有接口使用 **UTC+08:00 钟表时间**，有效公历年份为 **1813–2100**。日期必须真实存在；小时为 0–23，分钟为 0–59。不接受 24:00 表示次日，也不自动归一化 2 月 30 日等输入。

## 查询函数

| 用途 | Python | JavaScript / Java | C++ |
|---|---|---|---|
| 完整结果 | `get_calendar(y, m, d, hour=12, minute=0)` | `getCalendar(y, m, d, hour, minute)` | `get_calendar(y, m, d, hour=12, minute=0)` |
| 农历日期 | `get_lunar_date(y, m, d)` | `getLunarDate(y, m, d)` | `get_lunar_date(y, m, d)` |
| 格式化文本 | `format_calendar(y, m, d, hour=12, minute=0)` | `formatCalendar(y, m, d, hour, minute)` | `format_calendar(y, m, d, hour=12, minute=0)` |

JavaScript 的 `hour`、`minute` 可省略，默认 12、0。Java 的完整查询和格式化各提供 **3 参数**与 **5 参数**重载，3 参数版本默认 12:00。

Python 从 `chinese_calendar_core` 导入；JavaScript 从本地包 `chinese-calendar-core` 导入；Java 调用 `calendar.ChineseCalendar` 静态方法；C++ 使用 `<chinese_calendar/calendar.hpp>` 和 `chinese_calendar` 命名空间。

## 结构化结果

`get_calendar` / `getCalendar` 返回以下四部分：

| 字段 | 含义 |
|---|---|
| `solar` | 输入的 `year`、`month`、`day`、`hour`、`minute` |
| `lunar` | 农历 `year`、`month`（1–12）、`day`（1–30）、闰月标志 |
| `pillars` | `year`、`month`、`day`、`hour` 四个干支字符串 |
| `solar_term` / `solarTerm` | 当天的节气对象，包含 `name` 与近似 `time`（`HH:MM`） |

命名差异：

| 含义 | Python / C++ | JavaScript / Java |
|---|---|---|
| 闰月标志 | `is_leap_month` | `isLeapMonth` |
| 当天节气 | `solar_term` | `solarTerm` |

没有节气时，Python 返回 `None`，JavaScript 和 Java 返回 `null`，C++ 返回空的 `std::optional<SolarTerm>`。

Python 数据类不可变；Java 对象字段为 `final`；JavaScript 返回对象及嵌套对象均冻结；C++ 返回独立的值对象。修改 C++ 返回对象不会改动内部缓存。

`solarTerm` 是**当天的事件**，即使输入时间尚未交节也会返回。节气时间四舍五入到分钟，换柱使用内部未取整的近似瞬时，因此不能将显示字符串作为精确判定边界。

农历年的变化以正月初一为界；`pillars.year` 则以立春时刻为界。二者在春节附近不一致是预期行为。

## 示例

```python
from dataclasses import asdict
from chinese_calendar_core import get_calendar

result = get_calendar(2026, 10, 4, 9, 30)
data = asdict(result)  # 可继续交给 json.dumps
assert data["lunar"] == {
    "year": 2026, "month": 8, "day": 24, "is_leap_month": False,
}
```

安装本地 JavaScript 包后：

```javascript
import { getCalendar } from "chinese-calendar-core";

const result = getCalendar(2026, 10, 4, 9, 30);
console.log(JSON.stringify(result));
```

```java
import calendar.ChineseCalendar;

ChineseCalendar.LunarDate lunar = ChineseCalendar.getLunarDate(2033, 12, 22);
boolean leapEleventhMonth = lunar.month == 11 && lunar.isLeapMonth;
```

```cpp
#include <chinese_calendar/calendar.hpp>

auto result = chinese_calendar::get_calendar(2024, 2, 4);
if (result.solar_term) {
    auto name = result.solar_term->name;
}
```

## 错误处理

| 语言 | 非整数输入 | 无效日期、时间或范围 |
|---|---|---|
| Python | `TypeError`，包括布尔值 | `ValueError` |
| JavaScript | `TypeError`，包括字符串、布尔值、NaN、Infinity | `RangeError` |
| Java | 参数类型为 `int`，由编译器检查 | `IllegalArgumentException` |
| C++ | 参数类型为 `int`，调用方避免有损隐式转换 | `std::invalid_argument` |

库函数不会调用 `exit` 或终止宿主程序。Python 和 JavaScript 的命令行入口对输入错误返回退出码 2。

## 内部实现

Python 的 `_core.py`、JavaScript 的 `core.cjs` 和 Java 的 `AstronomicalCalendar` 都是内部实现。C++ 的内部函数保留在源文件匿名命名空间中。它们的名称、数据结构和缓存不属于公共兼容性约定，请使用上面的三个公共操作。
