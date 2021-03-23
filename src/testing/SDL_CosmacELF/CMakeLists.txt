cmake_minimum_required(VERSION 3.10)

if (MINGW32)
	set(CMAKE_SYSTEM_NAME Windows)
	SET(CMAKE_C_COMPILER x86_64-w64-mingw32-gcc)
	SET(CMAKE_CXX_COMPILER x86_64-w64-mingw32-g++)
	SET(CMAKE_RC_COMPILER x86_64-w64-mingw32-windres)
	set(CMAKE_RANLIB x86_64-w64-mingw32-ranlib)
	set(SDL2_INCLUDE_DIR /usr/x86_64-w64-mingw32/include)
	set(SDL2_LIBRARY /usr/x86_64-w64-mingw32/lib)
endif()

SET(CMAKE_CXX_STANDARD 11)
project(cosmacELF)

execute_process(COMMAND sbt "runMain Spinal1802.CDP1802Gen" WORKING_DIRECTORY ../../../)

execute_process(COMMAND sbt "runMain Spinal1802.CDP1861Gen" WORKING_DIRECTORY ../../../)

find_package(verilator HINTS $ENV{VERILATOR_ROOT})
find_package(SDL2 REQUIRED)

include_directories(${SDL2_INCLUDE_DIRS})

include_directories(obj_dir /usr/local/share/verilator/include)

add_library(cdp1802_lib SHARED
  /usr/local/share/verilator/include/verilated.cpp
)
verilate(cdp1802_lib SOURCES ../../../CDP1802.v)

add_library(cdp1861_lib SHARED
  /usr/local/share/verilator/include/verilated.cpp
)
verilate(cdp1861_lib SOURCES ../../../CDP1861.v)

add_executable(${CMAKE_PROJECT_NAME}
  CosmacELF.cpp
  Elements.cpp
)

target_link_libraries(${CMAKE_PROJECT_NAME} cdp1802_lib cdp1861_lib -lSDL2_ttf ${SDL2_LIBRARIES})
