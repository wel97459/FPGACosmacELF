cmake_minimum_required(VERSION 3.10)

SET(CMAKE_BUILD_TYPE Relese)
SET(CMAKE_CXX_STANDARD 11)
project(cosmacELF)

find_package(verilator HINTS $ENV{VERILATOR_ROOT})
find_package(SDL2 REQUIRED)

include_directories(${SDL2_INCLUDE_DIRS})

include_directories(obj_dir /usr/local/share/verilator/include)

add_library(cdp1802_lib SHARED
  /usr/local/share/verilator/include/verilated.cpp
)
verilate(cdp1802_lib SOURCES ../../../CDP1802.v TRACE)

add_executable(${CMAKE_PROJECT_NAME}
  CosmacELF.cpp
  Elements.cpp
)

target_link_libraries(${CMAKE_PROJECT_NAME} cdp1802_lib -lSDL2_ttf ${SDL2_LIBRARIES})