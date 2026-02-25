# CMakeLists.txt — Ableton Link JNI Bridge
#
# Builds the JNI glue library (libableton_link_jni.so) that bridges
# the Kotlin LinkSession class to the Ableton Link C++ SDK.
#
# ## Setup
#
# 1. Clone the Ableton Link SDK into this directory:
#    git clone https://github.com/Ableton/link.git link_sdk
#
# 2. Wire this CMake file in the android build.gradle.kts:
#    android {
#        externalNativeBuild {
#            cmake {
#                path = file("shared/tempo/src/androidMain/cpp/CMakeLists.txt")
#            }
#        }
#    }
#
# 3. Build the app — NDK compiles Link + JNI glue for all ABIs.
#
# ## Dependencies
#
# - Android NDK (r25+ recommended)
# - Ableton Link SDK (header-only C++ library)
# - ASIO (bundled with Link SDK in link_sdk/third_party/asio)

cmake_minimum_required(VERSION 3.22)
project(ableton_link_jni CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---- Ableton Link SDK ----
# Uncomment the following when the Link SDK is cloned into link_sdk/:
#
# set(LINK_DIR ${CMAKE_CURRENT_SOURCE_DIR}/link_sdk)
#
# # Link SDK is header-only; we just need to add its include paths
# # and its ASIO dependency.
# add_library(AbletonLink INTERFACE)
# target_include_directories(AbletonLink INTERFACE
#     ${LINK_DIR}/include
#     ${LINK_DIR}/third_party/asio/asio/include
# )
# target_compile_definitions(AbletonLink INTERFACE
#     LINK_PLATFORM_LINUX=1    # Android uses Linux platform
#     ASIO_STANDALONE=1        # Use standalone ASIO (no Boost)
# )
# target_link_libraries(AbletonLink INTERFACE
#     log                      # Android logging
# )

# ---- JNI Glue Library ----
add_library(ableton_link_jni SHARED
    link_jni.cpp
)

# Link against Android system libraries
target_link_libraries(ableton_link_jni
    log          # __android_log_print
    # AbletonLink  # Uncomment when Link SDK is available
)

# JNI headers are provided by the NDK
target_include_directories(ableton_link_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)
