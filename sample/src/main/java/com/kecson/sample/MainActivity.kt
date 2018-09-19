package com.kecson.sample

import android.content.Intent
import android.media.AudioRecord
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import com.kecson.lame4android.Mp3Recorder
import com.kecson.lame4android.demo.R
import java.io.File

class MainActivity : AppCompatActivity() {


    private var mRecorder: AudioRecord? = null
    private lateinit var button: ToggleButton
    private lateinit var mTvMp3Path: TextView
    private lateinit var mBtnPlayMp3: Button
    private lateinit var mMp3File: File
    private lateinit var mMp3Recorder: Mp3Recorder

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTitle(R.string.record_mp3)
        mMp3Recorder = Mp3Recorder(this)
        button = findViewById(R.id.button)
        mTvMp3Path = findViewById(R.id.tv_mp3_path)
        mBtnPlayMp3 = findViewById(R.id.btn_open_mp3)

        button.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mTvMp3Path.text = null
                mBtnPlayMp3.visibility = View.GONE
                //录音时间最大限制监听
                mMp3Recorder.setOnMaxDurationListener { max_duration_second ->
                    Log.e(this@MainActivity.javaClass.simpleName, "max_duration_second : $max_duration_second")

                    button.isEnabled = false
                    button.isChecked = false
                    mMp3File = mMp3Recorder.mp3File
                    Log.d(this@MainActivity.javaClass.simpleName, "mp3 file : $mMp3File")
                    mTvMp3Path.text = mMp3File.path
                    mBtnPlayMp3.visibility = View.VISIBLE
                    button.isEnabled = true
                }
                //限制录音最大10s
                mMp3Recorder.startRecording()

            } else {
                mMp3Recorder.stopRecording()
                mMp3File = mMp3Recorder.mp3File
                Log.d(this@MainActivity.javaClass.simpleName, "mp3 file : $mMp3File")
                mTvMp3Path.text = mMp3File.path
                mBtnPlayMp3.visibility = View.VISIBLE
            }
        }

        mBtnPlayMp3.setOnClickListener {
            val intent = Intent()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.action = android.content.Intent.ACTION_VIEW
            intent.setDataAndType(Uri.fromFile(mMp3File), "audio/*")
            startActivity(intent)
        }
    }


    public override fun onDestroy() {
        mRecorder!!.release()
        button.isChecked = false
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menu?.removeItem(R.id.record_mp3)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.convert_mp3) {
            if (button.isChecked) {
                button.isChecked = false
            }

            //先录音后转码
            startActivity(Intent(this, LameActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
