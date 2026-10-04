# Chinese Calendar Core for Python

Python 3.10+。运行时仅使用标准库。

```sh
python -m pip install .
python -m chinese_calendar_core 2026 10 4 9 30
```

```python
from chinese_calendar_core import get_calendar, get_lunar_date, format_calendar

calendar = get_calendar(2026, 10, 4, 9, 30)
print(calendar.lunar.month)  # 8
print(calendar.pillars.year)  # 丙午
print(format_calendar(2026, 10, 4, 9, 30))
```

三个接口分别返回完整数据、农历日期和中文文本。时间默认 12:00，固定 UTC+08:00，输入范围 1813–2100。非整数抛出 TypeError，非法日期/时间或超范围年份抛出 ValueError。

节气时刻为近似值，交节附近的分钟级四柱可能不同于高精度星历；1929 年前日期可能不同于历史历书。详见源码仓库的 `docs/ALGORITHMS.md`、`docs/API.md` 和 `docs/VALIDATION.md`。

MIT License。源码安装为准，未假定此包名已在 PyPI 发布。
