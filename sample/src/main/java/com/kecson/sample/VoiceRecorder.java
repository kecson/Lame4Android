package com.kecson.sample;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED;

public class VoiceRecorder {
    private final Context mContext;
    MediaRecorder mRecorder;
    public static final int FILE_NOT_FOUND = 400;
    public static final int FILE_INVALID = 401;
    public static final int FILE_UPLOAD_FAILED = 402;
    public static final int FILE_DOWNLOAD_FAILED = 403;
    public static final int FILE_DELETE_FAILED = 404;
    public static final int FILE_TOO_LARGE = 405;
    private static final String PREFIX = "voice";
    private static final String EXTENSION = ".mp3";
    private static final String TAG = "VoiceRecorder";
    private boolean mIsRecording = false;
    private long startTime;
    private String mVoiceFilePath = null;
    private String mVoiceFileName = null;
    private File mVoiceFile;
    private Handler mHandler;
    private OnMaxDurationListener mMaxDurationListener;

    public VoiceRecorder(Context context, Handler handler) {
        mContext = context.getApplicationContext();
        this.mHandler = handler;
    }

    public interface OnMaxDurationListener {
        void onMaxDuration(int max_duration_second);
    }

    public void setOnMaxDurationListener(OnMaxDurationListener listener) {
        mMaxDurationListener = listener;
    }

    /**
     * 开始录音
     *
     * @param max_duration_second 录音最大时间(秒) 1. <=0：不限  2.>0：最大时间(秒)
     * @return
     */
    public String startRecording(int max_duration_second) {
        mVoiceFile = null;
        try {
            // need to create recorder every time, otherwise, will got exception
            // from setOutputFile when try to reuse
            if (mRecorder != null) {
                mRecorder.release();
                mRecorder = null;
            }
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//            mRecorder.setAudioChannels(1); // MONO
//            mRecorder.setAudioSamplingRate(8000); // 8000Hz
//            mRecorder.setAudioEncodingBitRate(64); // seems if change this to
            if (max_duration_second > 0) {
                mRecorder.setMaxDuration(max_duration_second * 1000);
            }
            mRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    Log.w(TAG, "onInfo() called with: mr = [" + mr + "], what = [" + what + "], extra = [" + extra + "]");
                    if (what == MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.d(getClass().getSimpleName(), "onInfo() called with: mr = [" + mr + "], what = MEDIA_RECORDER_INFO_MAX_DURATION_REACHED" + ", extra = [" + extra + "]");
                        int duration = stopRecoding();
                        if (mMaxDurationListener != null) {
                            mMaxDurationListener.onMaxDuration(duration);
                        }
                    }
                }
            });
            // 128, still got same file
            // size.
            // one easy way is to use temp file
            // file = File.createTempFile(PREFIX + userId, EXTENSION,
            // User.getVoicePath());
            //录音文件名
            mVoiceFileName = PREFIX + "_" + DateFormat.format("yyyy_MM_dd_HH:mm:ss", System.currentTimeMillis()) + EXTENSION;
            //录音文件路径
            mVoiceFilePath = new File(mContext.getExternalCacheDir(), mVoiceFileName).getPath();
            mVoiceFile = new File(mVoiceFilePath);
            mRecorder.setOutputFile(mVoiceFile.getAbsolutePath());
            mRecorder.prepare();
            mIsRecording = true;
            mRecorder.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (mIsRecording) {
                        android.os.Message msg = new android.os.Message();
                        msg.what = mRecorder.getMaxAmplitude() * 13 / 0x7FFF;
                        mHandler.sendMessage(msg);
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    // from the crash report website, found one NPE crash from
                    // one android 4.0.4 htc phone
                    // maybe handler is null for some reason
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
        startTime = new Date().getTime();
        Log.d(TAG, "start voice recording to VoiceFile:" + mVoiceFile.getAbsolutePath());
        return mVoiceFile == null ? null : mVoiceFile.getAbsolutePath();
    }

    /**
     * stop the recoding
     *
     * @return seconds of the voice recorded
     */

    public void cancelRecording() {
        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
                if (mVoiceFile != null && mVoiceFile.exists() && !mVoiceFile.isDirectory()) {
                    mVoiceFile.delete();
                }
            } catch (RuntimeException e) {
            }
            mIsRecording = false;
        }
    }

    public int stopRecoding() {
        if (mRecorder != null) {
            mIsRecording = false;
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;

            if (mVoiceFile == null || !mVoiceFile.exists() || !mVoiceFile.isFile()) {
                return FILE_INVALID;
            }
            if (mVoiceFile.length() == 0) {
                mVoiceFile.delete();
                return FILE_INVALID;
            }
            int seconds = (int) (new Date().getTime() - startTime) / 1000;
            Log.d(TAG, "voice recording finished. seconds:" + seconds + " VoiceFile length:" + mVoiceFile.length());
            return seconds;
        }
        return 0;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (mRecorder != null) {
            mRecorder.release();
        }
    }


    public boolean isRecording() {
        return mIsRecording;
    }


    public String getVoiceFilePath() {
        return mVoiceFilePath;
    }

    public String getVoiceFileName() {
        return mVoiceFileName;
    }
}
