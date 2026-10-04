# 算法参考与核验资料

## 算法背景

- P. Bretagnon 与 G. Francou，VSOP87 行星理论。当前实现使用截断的 VSOP87D 地球黄经级数。
- Jean Meeus，*Astronomical Algorithms*，第二版，1998。合朔计算采用相应的周期展开思路；当前实现并非该书完整高精度实现。
- NASA，ΔT 分段多项式说明：[Polynomial Expressions for Delta T](https://eclipse.gsfc.nasa.gov/SEcat5/deltatpoly.html)。

## 实际用于核验的官方资料

### 香港天文台

- [公历与农历对照表（1901–2100）](https://www.hko.gov.hk/tc/gts/time/conversion.htm)
- [2026 年文字历表](https://www.hko.gov.hk/tc/gts/time/calendar/text/files/T2026c.txt)
- [1916 年文字历表](https://www.hko.gov.hk/tc/gts/time/calendar/text/files/T1916c.txt)

核验日期：2026-10-04。逐年读取官方文字版本并核对日期完整性。天文台网页的远期预测说明也属于解释结果时的依据。

### 中央气象署

- [2026 年日历资料表](https://www.cwa.gov.tw/Data/astronomy/2026cal.pdf)

使用“一、节气”和“二、朔望两弦”的时间，包括表内列出的 2027 年初数据。该表明确按世界时加 8 小时列示，公布粒度为分钟。

### NASA / GSFC

- Fred Espenak，[Phases of the Moon: 1901 to 2000](https://eclipse.gsfc.nasa.gov/phase/phases1901.html)

感谢 Fred Espenak / NASA 提供月相表。该表采用世界时并列到分钟；以其新月条目核验本项目合朔计算。由于同样使用 Meeus 算法家族，它是独立发布的校验数据，但不是与本项目完全无关的天文建模方法。

## 辅助研究资料

- [中国历日的编算](https://ytliu0.github.io/ChineseCalendar/computation_simp.html)：研究者整理的历史历法与历书差异说明，引用紫金山天文台历书等资料。该网页不是官方机构网站。
- sxtwl 2.0.7：独立历算软件，用于辅助日柱、时柱与交节边界比较，不是本项目运行依赖。

本项目代码采用 MIT。外部资料的版权和使用条款仍归其来源所有；本仓库未包含完整官方历表、第三方商业软件数据或相关逆向对照材料。
