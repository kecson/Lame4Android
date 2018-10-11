#include <stdbool.h>
#include "wrapper.h"


lame_t lame;

bool bigEndian;

/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_kecson_lame4android_Lame_init(
        JNIEnv *env, jclass cls, jint inSampleRate, jint outChannel,
        jint outSampleRate, jint outBitrate, jint quality) {
    if (lame != NULL) {
        lame_close(lame);
        lame = NULL;
    }


    LOGD("Init lame version is %s", get_lame_version());
    LOGD("Init parameters: inSampleRate= %d, outChannel=%d,  outSampleRate=%d, outBitrate=%d, quality=%d",
         inSampleRate, outChannel, outSampleRate, outBitrate, quality);

    lame = lame_init();
    lame_set_in_samplerate(lame, inSampleRate);
    lame_set_num_channels(lame, outChannel);
    lame_set_out_samplerate(lame, outSampleRate);
    lame_set_brate(lame, outBitrate);
//    lame_set_mode(lame,MONO);
//    lame_set_VBR(lame, vbr_default);
    lame_set_quality(lame, quality);
    lame_init_params(lame);
}

/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_kecson_lame4android_Lame_close(
        JNIEnv *env, jclass cls) {
    lame_close(lame);
    lame = NULL;
}

short swap_bytes(short w) {
    return (0xff00u & (w << 8)) | (0x00ffu & (w >> 8));
}

/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    encode
 * Signature: ([S[SI[B)I
 */
short toLittleEndian(bool bigEndian, short c) {
    if (bigEndian) {
        return swap_bytes(c);
    }
    return c;
}

JNIEXPORT jint JNICALL Java_com_kecson_lame4android_Lame_encode(
        JNIEnv *env, jclass cls, jshortArray buffer_l, jshortArray buffer_r,
        jint samples, jbyteArray mp3buf) {
    jshort *j_buffer_l = (*env)->GetShortArrayElements(env, buffer_l, NULL);

    *j_buffer_l = toLittleEndian(bigEndian, *j_buffer_l);

    jshort *j_buffer_r = (*env)->GetShortArrayElements(env, buffer_r, NULL);

    const jsize mp3buf_size = (*env)->GetArrayLength(env, mp3buf);
    jbyte *j_mp3buf = (*env)->GetByteArrayElements(env, mp3buf, NULL);

    int result = lame_encode_buffer(lame, j_buffer_l, j_buffer_r,
                                    samples, j_mp3buf, mp3buf_size);

    (*env)->ReleaseShortArrayElements(env, buffer_l, j_buffer_l, 0);
    (*env)->ReleaseShortArrayElements(env, buffer_r, j_buffer_r, 0);
    (*env)->ReleaseByteArrayElements(env, mp3buf, j_mp3buf, 0);

    return result;
}

/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    flush
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_com_kecson_lame4android_Lame_flush(
        JNIEnv *env, jclass cls, jbyteArray mp3buf) {
    const jsize mp3buf_size = (*env)->GetArrayLength(env, mp3buf);
    jbyte *j_mp3buf = (*env)->GetByteArrayElements(env, mp3buf, NULL);

    int result = lame_encode_flush(lame, j_mp3buf, mp3buf_size);

    (*env)->ReleaseByteArrayElements(env, mp3buf, j_mp3buf, 0);

    return result;
}


/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    getLameVersion
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_kecson_lame4android_Lame_getLameVersion(JNIEnv *env, jclass cls) {
    return get_lame_version();
}

#define BUFFER_SIZE 8192
#define be_short(s) ((short) ((unsigned short) (s) << 8) | ((unsigned short) (s) >> 8))

JNIEXPORT void JNICALL Java_com_kecson_lame4android_Lame_setRawBigEndian(JNIEnv *env, jclass cls,
                                                                         jboolean isBigEndian) {
    bigEndian = isBigEndian;
}

int read_samples(FILE *input_file, short *input) {
    int nb_read = fread(input, 1, sizeof(short), input_file) / sizeof(short);

    int i = 0;
    while (i < nb_read) {
        input[i] = be_short(input[i]);
        input[i] = toLittleEndian(bigEndian, input[i]);
        i++;
    }

    return nb_read;
}

/*
 * Class:     com_kecson_lame4android_Lame
 * Method:    encodeFile
 * Signature: (Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT void JNICALL Java_com_kecson_lame4android_Lame_encodeFile(JNIEnv *env, jclass cls,
                                                                    jstring in_source_path,
                                                                    jstring in_target_path) {
    const char *source_path, *target_path;
    source_path = (*env)->GetStringUTFChars(env, in_source_path, NULL);
    target_path = (*env)->GetStringUTFChars(env, in_target_path, NULL);

    FILE *input_file, *output_file;
    input_file = fopen(source_path, "rb");
    output_file = fopen(target_path, "wb");

    short input[BUFFER_SIZE];
    char output[BUFFER_SIZE];
    int nb_read = 0;
    int nb_write = 0;
    int nb_total = 0;

    LOGD("Encoding started");
    while ((nb_read = read_samples(input_file, input))) {
        nb_write = lame_encode_buffer(lame, input, input, nb_read, output, BUFFER_SIZE);
        fwrite(output, nb_write, 1, output_file);
        nb_total += nb_write;
    }
    LOGD("Encoded %d bytes", nb_total);

    nb_write = lame_encode_flush(lame, output, BUFFER_SIZE);
    fwrite(output, nb_write, 1, output_file);
    LOGD("Flushed %d bytes", nb_write);

    fclose(input_file);
    fclose(output_file);
}

