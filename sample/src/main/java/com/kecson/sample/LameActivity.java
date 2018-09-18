package com.kecson.sample;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.kecson.lame4android.Lame;
import com.kecson.lame4android.demo.R;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.kecson.lame4android.Lame.encodeFile;

public class LameActivity extends AppCompatActivity {


    public static final int NUM_CHANNELS = 1;
    public static final int SAMPLE_RATE = 16000;
    public static final int BITRATE = 128;
    public static final int QUALITY = 2;
    private AudioRecord mRecorder;
    private short[] mBuffer;
    private final String startRecordingLabel = "开始录音";
    private final String stopRecordingLabel = "停止录音";
    private boolean mIsRecording = false;
    private File mRawFile;
    private File mEncodedFile;
    private Button mButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lame);

        initRecorder();
        Lame.init(SAMPLE_RATE, NUM_CHANNELS, SAMPLE_RATE, BITRATE, QUALITY);

        mButton = (Button) findViewById(R.id.button);
        mButton.setText(startRecordingLabel);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (!mIsRecording) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });
    }

    private void stopRecord() {
        mButton.setText(startRecordingLabel);
        mIsRecording = false;
        mRecorder.stop();
        mEncodedFile = getFile("mp3");
        /* int result =*/ encodeFile(mRawFile.getAbsolutePath(), mEncodedFile.getAbsolutePath());
//        if (result == 0) {
            Toast.makeText(LameActivity.this, "Encoded to " + mEncodedFile.getName(), Toast.LENGTH_SHORT)
                    .show();
//        }
    }

    private void startRecord() {
        mButton.setText(stopRecordingLabel);
        mIsRecording = true;
        mRecorder.startRecording();
        mRawFile = getFile("raw");
        startBufferedWrite(mRawFile);
    }


    @Override
    public void onDestroy() {
        mRecorder.release();
        Lame.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.removeItem(R.id.convert_mp3);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.record_mp3) {
            if (mIsRecording) {
                stopRecord();
            }
            startActivity(new Intent(this, MainActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initRecorder() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        mBuffer = new short[bufferSize];
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }

    private void startBufferedWrite(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream output = null;
                try {
                    output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                    while (mIsRecording) {
                        int readSize = mRecorder.read(mBuffer, 0, mBuffer.length);
                        for (int i = 0; i < readSize; i++) {
                            output.writeShort(mBuffer[i]);
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(LameActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {
                            Toast.makeText(LameActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Toast.makeText(LameActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    private File getFile(final String suffix) {
        Time time = new Time();
        time.setToNow();
        return new File(Environment.getExternalStorageDirectory(), time.format("%Y%m%d%H%M%S") + "." + suffix);
    }
}
