cmake_minimum_required(VERSION 3.22)
project(oprek_native C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

# C sources are #included inside jni_bridge.cpp — do NOT list them separately
add_library(oprek_native SHARED
    jni_bridge.cpp
)

target_link_libraries(oprek_native
    log
    android
)
