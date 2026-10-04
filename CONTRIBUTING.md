# 贡献指南

欢迎提交问题、文档改进、测试和代码修改。

## 开发与验证

1. 使用 Python 3.10+、Node.js 18+、JDK 17+ 和 C++17 编译器。
2. 修改前阅读 [API](docs/API.md) 和 [算法约定](docs/ALGORITHMS.md)。
3. 运行 `python scripts/test_all.py`；只改某种语言时可以先用 `--languages` 选择，但涉及算法或公共行为的修改需要验证四种实现。
4. 提交中说明问题、可复现输入、修改后的行为和实际执行的验证。

## 命名与代码风格

- Python 的函数、变量、模块使用 `snake_case`，公开数据类型使用 `PascalCase`。
- JavaScript 使用 `camelCase`，类和接口使用 `PascalCase`；保留 CommonJS 与 ESM 一致的接口。
- Java 使用 `camelCase` 方法及变量、`PascalCase` 类名，公共入口为 `calendar.ChineseCalendar`。
- C++ 函数及变量使用 `snake_case`、数据结构使用 `PascalCase`，公共接口位于 `chinese_calendar` 命名空间。
- 常量使用 `UPPER_SNAKE_CASE`。天文公式中 `t`、`k` 等标准数学符号可以保留，但应说明单位和含义。
- 文件使用 UTF-8、LF 换行；遵循 `.editorconfig`。

## 报告日期或节气差异

请给出公历日期、小时分钟、时区、语言与运行版本，以及实际和预期结果。预期结果应附可核查的资料出处。区分农历年和年柱、节气日期和精确交接时刻、现代规则重算和历史历书。

更改天文系数、ΔT、闰月规则或边界处理时，请提供独立参考与精度对比。共享回归文件记录既有行为；不要仅为让测试变绿而批量重写期望值。先说明行为为什么应改变，再同步四种实现及相关文档。

## 文档及版本

新增或改变公共接口时，更新 README、API、示例和 CHANGELOG。破坏性调整需要补充迁移说明。提交源码和必要测试数据，不提交构建产物、虚拟环境、IDE 配置或本机凭据。

贡献内容按仓库的 MIT 许可证提供；引用的外部内容应保留其来源和必要说明。
