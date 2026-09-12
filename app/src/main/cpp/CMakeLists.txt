cmake_minimum_required(VERSION 3.22.1)

project("formsaathi_native")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)


set(GGML_FMA OFF CACHE BOOL "" FORCE)

set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

add_subdirectory(whisper.cpp)

add_library(
        formsaathi_native
        SHARED
        whisper_jni.cpp
)
target_include_directories(
        formsaathi_native
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/include
)

find_library(
        log-lib
        log
)

target_link_libraries(
        formsaathi_native
        whisper
        ${log-lib}
)
set(CMAKE_C_FLAGS " -O3")
set(CMAKE_CXX_FLAGS " -O3")
set(CMAKE_C_FLAGS_DEBUG "-O3 -g")
set(CMAKE_CXX_FLAGS_DEBUG "-O3 -g")
