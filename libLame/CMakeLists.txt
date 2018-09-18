# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.


#指定源码路径，即将src/main/jni/libmp3lame路径赋值给SRC_DIR
set(SRC_DIR src/main/jni/libmp3lame)
# 指定头文件路径
include_directories(src/main/jni/libmp3lame)
# 将src/main/jni/libmp3lame目录下的所有文件名赋值给SRC_LIST
aux_source_directory(src/main/jni/libmp3lame SRC_LIST)


add_library( # 指定库的名称
             mp3lame

             # 将库设置为共享库
             SHARED

             # 提供源文件的相对路径.
             ${SRC_LIST}
             src/main/jni/wrapper.c)


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       mp3lame
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )