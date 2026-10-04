# Chinese Calendar Core for C++

需要 C++17 编译器。公共头文件为 `chinese_calendar/calendar.hpp`，所有公共函数位于 `chinese_calendar` 命名空间。

## 构建和测试

从仓库根目录：

```sh
cmake -S cpp -B .build/cpp -DCMAKE_BUILD_TYPE=Release
cmake --build .build/cpp --config Release
ctest --test-dir .build/cpp -C Release --output-on-failure
```

示例可执行文件名为 `calendar_example`；多配置生成器一般将其放在构建目录的 `Release/` 子目录。Windows 后缀为 `.exe`。

也可直接编译：

```sh
g++ -std=c++17 -O2 -I cpp/include cpp/src/calendar.cpp cpp/examples/basic.cpp -o calendar_example
```

MSVC 请启用 `/std:c++17 /utf-8`。CMake 配置会传递 UTF-8 选项。

## 作为子项目使用

```cmake
add_subdirectory(path/to/calendar/cpp calendar-build)
target_link_libraries(your_application PRIVATE ChineseCalendar::chinese_calendar)
```

## 安装后使用

```sh
cmake --install .build/cpp --config Release --prefix /your/install/prefix
```

消费者项目：

```cmake
find_package(ChineseCalendar CONFIG REQUIRED)
target_link_libraries(your_application PRIVATE ChineseCalendar::chinese_calendar)
```

配置消费者时设置 `-DCMAKE_PREFIX_PATH=/your/install/prefix`。默认构建静态库；构建共享库可设置 `BUILD_SHARED_LIBS=ON`。库和消费者应使用兼容的编译器与运行库。

```cpp
#include <chinese_calendar/calendar.hpp>

const auto result = chinese_calendar::get_calendar(2026, 10, 4, 9, 30);
const auto text = chinese_calendar::format_calendar(2026, 10, 4, 9, 30);
```

非法输入抛出 `std::invalid_argument`。返回值独立于内部缓存；缓存按线程隔离。默认时间 12:00，固定 UTC+08:00，输入范围 1813–2100。精度和历史规则见 [算法说明](../docs/ALGORITHMS.md)。
