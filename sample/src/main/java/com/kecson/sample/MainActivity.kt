package com.kecson.sample

import android.content.Intent
import android.media.AudioRecord
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ToggleButton
import com.kecson.lame4android.Mp3Recorder
import com.kecson.lame4android.demo.R

class MainActivity : AppCompatActivity() {


    private var mRecorder: AudioRecord? = null
    private lateinit var button: ToggleButton


    private lateinit var mMp3Recorder: Mp3Recorder

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mMp3Recorder = Mp3Recorder(this)
        button = findViewById(R.id.button)
        button.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mMp3Recorder.startRecording()
            } else {
                mMp3Recorder.stopRecording()
                val mp3File = mMp3Recorder.mp3File
                Log.d(this@MainActivity.javaClass.simpleName, "mp3 file : $mp3File")

            }
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
