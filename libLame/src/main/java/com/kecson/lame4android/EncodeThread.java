package com.kecson.lame4android;

import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;

public class EncodeThread extends Thread implements AudioRecord.OnRecordPositionUpdateListener {

    private static final String TAG = EncodeThread.class.getSimpleName();

    public static final int PROCESS_STOP = 1;

    private StopHandler mStopHandler;

    private byte[] mBuffer;

    private byte[] mMp3Buffer;

    private RingBuffer mRingBuffer;

    private FileOutputStream mFos;

    private int mBufferSize;

    private CountDownLatch mHandlerInitLatch = new CountDownLatch(1);

    static class StopHandler extends Handler {

        WeakReference<EncodeThread> encodeThread;

        StopHandler(EncodeThread encodeThread) {
            this.encodeThread = new WeakReference<EncodeThread>(encodeThread);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == PROCESS_STOP) {
                EncodeThread threadRef = encodeThread.get();
                // Process all data in ring mBuffer and flush
                // left data to file
                while (threadRef.processData() > 0) ;

                // Cancel any event left in the queue
                removeCallbacksAndMessages(null);
                threadRef.flushAndRelease();
                getLooper().quit();
            }
            super.handleMessage(msg);
        }
    }


    /**
     * Constructor
     *
     * @param ringBuffer
     * @param os
     * @param bufferSize
     */
    public EncodeThread(RingBuffer ringBuffer, FileOutputStream os, int bufferSize) {
        this.mFos = os;
        this.mRingBuffer = ringBuffer;
        this.mBufferSize = bufferSize;
        mBuffer = new byte[bufferSize];
        mMp3Buffer = new byte[(int) (7200 + (mBuffer.length * 2 * 1.25))];
    }

    @Override
    public void run() {
        Looper.prepare();
        mStopHandler = new StopHandler(this);
        mHandlerInitLatch.countDown();
        Looper.loop();
    }

    /**
     * Return the mStopHandler attach to this thread
     *
     * @return
     */
    public Handler getStopHandler() {
        try {
            mHandlerInitLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, "Error when waiting handle to init");
        }
        return mStopHandler;
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {
        // Do nothing
    }

    @Override
    public void onPeriodicNotification(AudioRecord recorder) {
        processData();
    }

    /**
     * Get data from ring mBuffer
     * Encode it to mp3 frames using lame encoder
     *
     * @return Number of bytes read from ring mBuffer
     * 0 in case there is no data left
     */
    private int processData() {
        int bytes = mRingBuffer.read(mBuffer, mBufferSize);
        Log.d(TAG, "Read size: " + bytes);
        if (bytes > 0) {
            short[] innerBuf = new short[bytes / 2];
            ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(innerBuf);
            int encodedSize = Lame.encode(innerBuf, innerBuf, bytes / 2, mMp3Buffer);

            if (encodedSize < 0) {
                Log.e(TAG, "Lame encoded size: " + encodedSize);
            }

            try {
                mFos.write(mMp3Buffer, 0, encodedSize);
            } catch (IOException e) {
                Log.e(TAG, "Unable to write to file");
            }

            return bytes;
        }
        return 0;
    }

    /**
     * Flush all data left in lame mBuffer to file
     */
    private void flushAndRelease() {
        final int flushResult = Lame.flush(mMp3Buffer);

        if (flushResult > 0) {
            try {
                mFos.write(mMp3Buffer, 0, flushResult);
            } catch (final IOException e) {
                Log.e(TAG, "Lame flush error");
            }
        }
    }
}
