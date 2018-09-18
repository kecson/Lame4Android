package com.kecson.lame4android;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Mp3Recorder {

    private static final String TAG = Mp3Recorder.class.getSimpleName();

    //    44100Hz是当前唯一能保证在所有设备上工作的采样率，在一些设备上还有22050, 16000或11025。
    private static final int DEFAULT_SAMPLING_RATE = 44100;

    private static final int FRAME_COUNT = 160;

    /* Encoded bit rate. MP3 file will be encoded with bit rate 32kbps */
    private static final int BIT_RATE = 32;

    private AudioRecord mAudioRecord = null;

    private int mBufferSize;

    private File mMp3File;

    private RingBuffer mRingBuffer;

    private byte[] mBuffer;

    private FileOutputStream mFos = null;

    private EncodeThread mEncodeThread;

    private int mSamplingRate;

    private int mChannelConfig;

    private PCMFormat mAudioFormat;

    private boolean mIsRecording = false;
    private Context mContext;
    private OnMaxDurationListener mMaxDurationListener;
    private int mMaxDurationSecond;

    public Mp3Recorder(Context context, int samplingRate, int channelConfig, PCMFormat audioFormat) {
        mContext = context.getApplicationContext();
        mSamplingRate = samplingRate;
        mChannelConfig = channelConfig;
        mAudioFormat = audioFormat;
    }

    /**
     * Default constructor. Setup recorder with default sampling rate 1 channel,
     * 16 bits pcm
     */
    public Mp3Recorder(Context context) {
        this(context, DEFAULT_SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, PCMFormat.PCM_16BIT);
    }

    /**
     * Start recording. Create an encoding thread. Start record from this
     * thread.
     */
    public void startRecording() throws IOException {
        startRecording(-1);
    }

    public void startRecording(int max_duration_second) throws IOException {
        startRecording(max_duration_second, null);
    }

    /**
     * Start recording. Create an encoding thread. Start record from this
     * thread.
     */
    public void startRecording(int max_duration_second, File mp3File) throws IOException {
        if (mIsRecording) return;
        mMaxDurationSecond = max_duration_second;
        Log.i(TAG, "Start recording, BufferSize = " + mBufferSize);
        // Initialize mAudioRecord if it's null.
        if (mAudioRecord == null) {
            initAudioRecorder(mp3File);
        } else {
            mMp3File = mp3File;
        }
        mAudioRecord.startRecording();

        new Thread() {

            @Override
            public void run() {
                mIsRecording = true;
                if (mMaxDurationSecond > 0) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mIsRecording) {
                                try {
                                    stopRecording();
                                    if (mMaxDurationListener != null) {
                                        mMaxDurationListener.onMaxDuration(mMaxDurationSecond);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }, mMaxDurationSecond * 1000 + 100);
                }

                while (mIsRecording) {
                    int bytes = mAudioRecord.read(mBuffer, 0, mBufferSize);
                    if (bytes > 0) {
                        mRingBuffer.write(mBuffer, bytes);
                    }
                }

                // release and finalize mAudioRecord
                try {
                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;

                    // stop the encoding thread and try to wait
                    // until the thread finishes its job
                    Message msg = Message.obtain(mEncodeThread.getStopHandler(),
                            EncodeThread.PROCESS_STOP);
                    msg.sendToTarget();

//                    Log.d(TAG, "waiting for encoding thread");
                    mEncodeThread.join();
                    Log.d(TAG, "done encoding thread");
                } catch (InterruptedException e) {
                    Log.e(TAG, "Failed to join encode thread");
                } finally {
                    if (mFos != null) {
                        try {
                            mFos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }.start();

    }

    /**
     * @throws IOException
     */
    public void stopRecording() throws IOException {
        Log.d(TAG, "stop recording");
        mIsRecording = false;
    }

    /**
     * Initialize audio recorder
     *
     * @param mp3File
     */
    private void initAudioRecorder(File mp3File) throws IOException {

        int bytesPerFrame = mAudioFormat.getBytesPerFrame();
		/* Get number of samples. Calculate the buffer size (round up to the
		   factor of given frame size) */
        int frameSize = AudioRecord.getMinBufferSize(mSamplingRate,
                mChannelConfig, mAudioFormat.getAudioFormat()) / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize = frameSize + (FRAME_COUNT - frameSize % FRAME_COUNT);
//            Log.d(TAG, "Frame size: " + frameSize);
        }

        mBufferSize = frameSize * bytesPerFrame;

        /* Setup audio recorder */
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                mSamplingRate, mChannelConfig, mAudioFormat.getAudioFormat(),
                mBufferSize);
        // Setup RingBuffer. Currently is 10 times size of hardware buffer
        // Initialize buffer to hold data
        mRingBuffer = new RingBuffer(10 * mBufferSize);
        mBuffer = new byte[mBufferSize];

        // Initialize lame buffer
        // mp3 sampling rate is the same as the recorded pcm sampling rate
        // The bit rate is 32kbps
        Lame.init(mSamplingRate, 1, mSamplingRate, BIT_RATE, 7);

        if (mp3File == null) {

            File directory = mContext.getExternalCacheDir();
            if (!directory.exists()) {
                directory.mkdirs();
//            Log.d(TAG, "Created directory");
            }
            String date = DateFormat.format("yyyyMMdd_HHmmss", System.currentTimeMillis()).toString();
            String mp3FileName = String.format("rec_%s.mp3", date);
            mMp3File = new File(directory, mp3FileName);
        } else {
            mMp3File = mp3File;
        }
        mFos = new FileOutputStream(mMp3File);
        Log.i(TAG, "mp3 file: " + mMp3File.getPath());
        // Create and run thread used to encode data
        // The thread will
        mEncodeThread = new EncodeThread(mRingBuffer, mFos, mBufferSize);
        mEncodeThread.start();
        mAudioRecord.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread.getStopHandler());
        mAudioRecord.setPositionNotificationPeriod(FRAME_COUNT);
    }

    public File getMp3File() {
        return mMp3File;
    }

    public interface OnMaxDurationListener {
        void onMaxDuration(int max_duration_second);
    }

    public void setOnMaxDurationListener(OnMaxDurationListener listener) {
        mMaxDurationListener = listener;
    }
}