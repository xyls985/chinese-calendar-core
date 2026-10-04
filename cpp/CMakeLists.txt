cmake_minimum_required(VERSION 3.16)
project(ChineseCalendar VERSION 0.1.0 LANGUAGES CXX)

include(GNUInstallDirs)
include(CMakePackageConfigHelpers)
include(CTest)

add_library(chinese_calendar src/calendar.cpp)
set_target_properties(chinese_calendar PROPERTIES WINDOWS_EXPORT_ALL_SYMBOLS ON)
add_library(ChineseCalendar::chinese_calendar ALIAS chinese_calendar)
target_compile_features(chinese_calendar PUBLIC cxx_std_17)
target_include_directories(chinese_calendar PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
    $<INSTALL_INTERFACE:${CMAKE_INSTALL_INCLUDEDIR}>)
if(MSVC)
    target_compile_options(chinese_calendar PUBLIC /utf-8 PRIVATE /W4)
else()
    target_compile_options(chinese_calendar PRIVATE -Wall -Wextra -Wpedantic)
endif()

option(CHINESE_CALENDAR_BUILD_EXAMPLES "Build the basic example" ON)
if(CHINESE_CALENDAR_BUILD_EXAMPLES)
    add_executable(calendar_example examples/basic.cpp)
    target_link_libraries(calendar_example PRIVATE ChineseCalendar::chinese_calendar)
endif()

if(BUILD_TESTING)
    add_executable(calendar_test tests/calendar_test.cpp)
    target_link_libraries(calendar_test PRIVATE ChineseCalendar::chinese_calendar)
    add_test(NAME calendar_contracts COMMAND calendar_test "${CMAKE_CURRENT_SOURCE_DIR}/..")
endif()

install(TARGETS chinese_calendar EXPORT ChineseCalendarTargets
    ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR})
install(DIRECTORY include/ DESTINATION ${CMAKE_INSTALL_INCLUDEDIR})
install(FILES "${CMAKE_CURRENT_SOURCE_DIR}/../LICENSE"
    DESTINATION ${CMAKE_INSTALL_DATADIR}/licenses/ChineseCalendar)
install(EXPORT ChineseCalendarTargets NAMESPACE ChineseCalendar::
    DESTINATION ${CMAKE_INSTALL_LIBDIR}/cmake/ChineseCalendar)
configure_package_config_file(cmake/ChineseCalendarConfig.cmake.in
    "${CMAKE_CURRENT_BINARY_DIR}/ChineseCalendarConfig.cmake"
    INSTALL_DESTINATION ${CMAKE_INSTALL_LIBDIR}/cmake/ChineseCalendar)
write_basic_package_version_file("${CMAKE_CURRENT_BINARY_DIR}/ChineseCalendarConfigVersion.cmake"
    VERSION ${PROJECT_VERSION} COMPATIBILITY SameMajorVersion)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/ChineseCalendarConfig.cmake"
    "${CMAKE_CURRENT_BINARY_DIR}/ChineseCalendarConfigVersion.cmake"
    DESTINATION ${CMAKE_INSTALL_LIBDIR}/cmake/ChineseCalendar)
