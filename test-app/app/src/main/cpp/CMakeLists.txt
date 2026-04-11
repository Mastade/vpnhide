cmake_minimum_required(VERSION 3.22.1)
project("vpnhide_test")

add_library(vpnhide_test SHARED native-lib.cpp)

target_link_libraries(vpnhide_test
    android
    log
)
