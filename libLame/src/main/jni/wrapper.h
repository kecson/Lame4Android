/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <android/log.h>
#include "lame.h"
/* Header for class com_kecson_lame4android_Lame */

#ifndef _Included_com_kecson_lame4android_Lame
#define _Included_com_kecson_lame4android_Lame
#ifdef __cplusplus
extern "C" {
#endif

#define LOG_TAG "mp3lame"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    init
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_com_kecson_lame4android_Lame_init
        (JNIEnv *, jclass, jint, jint, jint, jint, jint);

/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_kecson_lame4android_Lame_close
        (JNIEnv *, jclass);

/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    encode
 * Signature: ([S[SI[B)I
 */
JNIEXPORT jint JNICALL Java_com_kecson_lame4android_Lame_encode
        (JNIEnv *, jclass, jshortArray, jshortArray, jint, jbyteArray);

/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    flush
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_com_kecson_lame4android_Lame_flush
        (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    getLameVersion
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_kecson_lame4android_Lame_getLameVersion
        (JNIEnv *, jclass);

/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    encodeFile
 * Signature: (Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT void JNICALL Java_com_kecson_lame4android_Lame_encodeFile
        (JNIEnv *, jclass, jstring, jstring);

#ifdef __cplusplus
}
#endif
#endif
