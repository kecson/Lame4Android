package com.kecson.lame4android;

import android.media.AudioFormat;

public enum PCMFormat {
    PCM_8BIT(1, AudioFormat.ENCODING_PCM_8BIT),
    PCM_16BIT(2, AudioFormat.ENCODING_PCM_16BIT);

    private int mBytesPerFrame;
    private int mAudioFormat;

    PCMFormat(int bytesPerFrame, int audioFormat) {
        this.mBytesPerFrame = bytesPerFrame;
        this.mAudioFormat = audioFormat;
    }

    public int getBytesPerFrame() {
        return mBytesPerFrame;
    }

    public void setBytesPerFrame(int bytesPerFrame) {
        this.mBytesPerFrame = bytesPerFrame;
    }

    public int getAudioFormat() {
        return mAudioFormat;
    }

    public void setAudioFormat(int audioFormat) {
        this.mAudioFormat = audioFormat;
    }
}
