# Chinese Calendar Core for Java

公共入口：`calendar.ChineseCalendar`。使用 JDK 17+ 构建，目标字节码兼容 Java 8+，运行时无第三方依赖。

从仓库根目录运行测试和示例：

```sh
python scripts/test_all.py --languages java
javac -encoding UTF-8 -cp .build/tests/java -d .build/tests/java java/examples/CalendarExample.java
java '-Dfile.encoding=UTF-8' -cp .build/tests/java CalendarExample
```

JDK 不在 PATH 时，可设置 `JAVA_HOME` 或向测试脚本传入 `--java-home`。PowerShell 中使用 `'-Dfile.encoding=UTF-8'` 作为单个参数。

Maven 构建：

```sh
mvn -f java/pom.xml package
```

产物为 `java/target/chinese-calendar-core-0.1.0.jar`。Maven 的 test 阶段会运行与本地测试入口相同的 `CalendarTest`，无需 JUnit。

```java
import calendar.ChineseCalendar;

ChineseCalendar.CalendarResult result = ChineseCalendar.getCalendar(2026, 10, 4, 9, 30);
int lunarMonth = result.lunar.month;
boolean leapMonth = result.lunar.isLeapMonth;
String text = ChineseCalendar.formatCalendar(2026, 10, 4, 9, 30);
```

完整查询和格式化有三参数、五参数重载；省略时间时默认 12:00。非法日期、时间或超范围年份抛出 `IllegalArgumentException`。

固定 UTC+08:00，范围 1813–2100；节气是近似时刻，历史历法口径和精度限制见 [算法说明](../docs/ALGORITHMS.md)。
