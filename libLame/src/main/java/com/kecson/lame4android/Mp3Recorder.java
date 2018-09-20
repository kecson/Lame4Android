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
import java.io.IOException;

public class Mp3Recorder {
    private static final String TAG = "Mp3Recorder";
    //=======================AudioRecord Default Settings=======================
    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    /**
     * 以下三项为默认配置参数。Google Android文档明确表明只有以下3个参数是可以在所有设备上保证支持的。
     * 44100Hz是当前唯一能保证在所有设备上工作的采样率，在一些设备上还有22050, 16000或11025。
     */
    private static final int DEFAULT_SAMPLING_RATE = 44100;//模拟器仅支持从麦克风输入8kHz采样率
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 下面是对此的封装
     * private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
     */
    private static final PCMFormat DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT;

    //======================Lame Default Settings=====================
    private static final int DEFAULT_LAME_MP3_QUALITY = 7;
    /**
     * 与DEFAULT_CHANNEL_CONFIG相关，因为是mono单声，所以是1
     */
    private static final int DEFAULT_LAME_IN_CHANNEL = 1;
    /**
     * Encoded bit rate. MP3 file will be encoded with bit rate 32kbps
     */
    private static final int DEFAULT_LAME_MP3_BIT_RATE = 32;

    //==================================================================

    /**
     * 自定义 每160帧作为一个周期，通知一下需要进行编码
     */
    private static final int FRAME_COUNT = 160;
    private AudioRecord mAudioRecord = null;
    private int mBufferSize;
    private short[] mPCMBuffer;
    private EncodeThread mEncodeThread;
    private Context mContext;
    private int mSamplingRate;
    private int mChannelConfig;
    private PCMFormat mPCMFormat;
    private int mMaxDurationSecond;
    private File mMp3File;
    private Thread mAudioThread;
    private OnMaxDurationListener mMaxDurationListener;
    private int mVolume;
    private final Object mRecordingStateLock = new Object();

    public Mp3Recorder(Context context, int samplingRate, int channelConfig, PCMFormat audioFormat) {
        mContext = context.getApplicationContext();
        mSamplingRate = samplingRate;
        mChannelConfig = channelConfig;
        mPCMFormat = audioFormat;
    }

    /**
     * Default constructor. Setup recorder with default sampling rate 1 channel,
     * 16 bits pcm
     */
    public Mp3Recorder(Context context) {
        this(context, DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT);
    }

    public PCMFormat getPCMFormat() {
        return mPCMFormat;
    }

    public int getChannelConfig() {
        return mChannelConfig;
    }

    public int getSamplingRate() {
        return mSamplingRate;
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

        synchronized (mRecordingStateLock) {
            if (mAudioRecord != null && isRecording())
                return;

            mMaxDurationSecond = max_duration_second;
            Log.i(TAG, "Start recording, BufferSize = " + mBufferSize);
            // Initialize mAudioRecord if it's null.
            if (mAudioRecord == null) {
                initAudioRecorder(mp3File);
            } else {
                mMp3File = mp3File;
            }
            mAudioRecord.startRecording();
            mAudioThread = new Thread() {
                @Override
                public void run() {
                    synchronized (mRecordingStateLock) {
                        if (mMaxDurationSecond > 0) {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (isRecording()) {
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
                            }, mMaxDurationSecond * 1000 + 100);//delay more 100milli finish
                        }
                    }

                    //设置线程权限
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                    while (isRecording()) {
                        if (mAudioRecord != null) {
                            if (isRecording()) {
                                int readSize = mAudioRecord.read(mPCMBuffer, 0, mBufferSize);
                                if (readSize > 0) {
                                    mEncodeThread.addTask(mPCMBuffer, readSize);
                                    mVolume = calculateRealVolume(mPCMBuffer, readSize);
                                }
                            }
                        }

                    }

                    release();
                }

                /**
                 * 此计算方法来自samsung开发范例
                 *
                 * @see {@literal https://developer.samsung.com/technical-doc/view.do?v=T000000086}
                 */
                private int calculateRealVolume(short[] buffer, int readSize) {
                    int sum = 0;
                    int volume = 0;
                    if (buffer == null) {
                        return volume;
                    }
                    for (int i = 0; i < readSize; i++) {
                        sum += buffer[i] * buffer[i];
                    }
                    if (readSize > 0) {
                        double amplitude = sum / readSize;
                        volume = (int) Math.sqrt(amplitude);
                    }
                    return volume;
                }

            };
            mAudioThread.start();
        }


    }


    private void release() {
        synchronized (mRecordingStateLock) {
            try {
                if (isRecording()) {
                    mAudioRecord.setRecordPositionUpdateListener(null);
                    mAudioRecord.stop();
                    mAudioRecord.release();
                }
                Handler handler = mEncodeThread != null ? mEncodeThread.getHandler() : null;
                if (handler != null) {
                    Message msg = new Message();
                    msg.what = EncodeThread.PROCESS_STOP;
                    handler.handleMessage(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mAudioRecord = null;
            }
        }

    }

    /**
     * 获取相对音量。 超过最大值时取最大值。
     *
     * @return 音量
     */
    public int getVolume() {
        return Math.min(mVolume, MAX_VOLUME);
    }

    private static final int MAX_VOLUME = 2000;

    /**
     * 根据资料假定的最大值。 实测时有时超过此值。
     *
     * @return 最大音量值。
     */
    public int getMaxVolume() {
        return MAX_VOLUME;
    }


    /**
     * @throws IOException
     */
    public void stopRecording() throws IOException {
        Log.d(TAG, "stop recording");
        release();
    }

    /**
     * Initialize audio recorder
     *
     * @param mp3File
     */
    private void initAudioRecorder(File mp3File) throws IOException {

        mBufferSize = AudioRecord.getMinBufferSize(mSamplingRate,
                mChannelConfig, mPCMFormat.getAudioFormat());

        int bytesPerFrame = DEFAULT_AUDIO_FORMAT.getBytesPerFrame();
        /* Get number of samples. Calculate the buffer size
         * (round up to the factor of given frame size)
         * 使能被整除，方便下面的周期性通知
         * */
        int frameSize = mBufferSize / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
            mBufferSize = frameSize * bytesPerFrame;
        }

        /* Setup audio recorder */
        mAudioRecord = new AudioRecord(DEFAULT_AUDIO_SOURCE,
                DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat(),
                mBufferSize);

        mPCMBuffer = new short[mBufferSize];
        /*
         * Initialize lame buffer
         * mp3 sampling rate is the same as the recorded pcm sampling rate
         * The bit rate is 32kbps
         *
         */
        Lame.init(mSamplingRate, mChannelConfig, mSamplingRate, DEFAULT_LAME_MP3_BIT_RATE, DEFAULT_LAME_MP3_QUALITY);
        // Create and run thread used to encode data
        // The thread will

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

        mEncodeThread = new EncodeThread(mMp3File, mBufferSize);
        mEncodeThread.start();
        mAudioRecord.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread.getHandler());
        mAudioRecord.setPositionNotificationPeriod(FRAME_COUNT);
    }

    public File getMp3File() {
        return mMp3File;
    }

    public boolean isRecording() {
        return mAudioRecord != null && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }

    public interface OnMaxDurationListener {
        void onMaxDuration(int max_duration_second);
    }

    public void setOnMaxDurationListener(OnMaxDurationListener listener) {
        mMaxDurationListener = listener;
    }
}