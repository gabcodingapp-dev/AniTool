cmake_minimum_required(VERSION 3.22)
project(oprek_native C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

# Capstone disassembler
add_subdirectory(capstone)

# Main shared library
add_library(oprek_native SHARED
    jni_bridge.cpp
    elf_parser.c
    pe_parser.c
    dex_parser.c
    obfuscate.c
    patch_utils.c
)

target_include_directories(oprek_native PRIVATE
    capstone/include
)

target_link_libraries(oprek_native
    capstone-static
    log
    android
)
