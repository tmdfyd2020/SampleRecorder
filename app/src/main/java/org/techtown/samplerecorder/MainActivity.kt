package org.techtown.samplerecorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import org.techtown.samplerecorder.List.ListActivity
import org.techtown.samplerecorder.Main.AudioRecord
import org.techtown.samplerecorder.Main.AudioRecord.Companion.recordWave
import org.techtown.samplerecorder.Main.AudioTrack
import org.techtown.samplerecorder.Main.AudioTrack.Companion.playWave
import org.techtown.samplerecorder.Main.Queue
import org.techtown.samplerecorder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val TAG = this.javaClass.simpleName
    private lateinit var binding: ActivityMainBinding

    private val mAudioRecord by lazy { AudioRecord() }
    private val mAudioTrack by lazy { AudioTrack() }
    private val waveform by lazy { binding.viewWaveForm }
    private val switchButton by lazy { binding.switchButton }
    private val sharedPreferences by lazy { getSharedPreferences(DATABASE, MODE_PRIVATE) }
    private val editor by lazy { sharedPreferences.edit() }
    private var queue: Queue? = null
    private val volumeObserver by lazy { VolumeObserver(this, Handler()) }
    private val dialogService by lazy { DialogService(this) }

    private var startTime: Long = 0
    private var fileDrop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        permissionCheck()
        initUi()
        initState()
    }

    private fun permissionCheck() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_CODE)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initUi() {
        val toolbar = binding.toolbarMain
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val nCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        with (binding) {
            btnRecordSource.text = "${getString(R.string.source)}\n${getString(R.string.mic)}"
            btnPlayType.text = "${getString(R.string.type)}\n${getString(R.string.media)}"
            btnRecordChannel.text = "${getString(R.string.channel)}\n${getString(R.string.mono)}"
            btnPlayChannel.text = "${getString(R.string.channel)}\n${getString(R.string.mono)}"
            btnRecordSampleRate.text = "${getString(R.string.rate)}\n${getString(R.string.rate_16000)}"
            btnPlaySampleRate.text = "${getString(R.string.rate)}\n${getString(R.string.rate_16000)}"
            btnRecordBufferSize.text = "${getString(R.string.buffer_size)}\n${getString(R.string.buffer_size_1024)}"
            btnPlay.isEnabled = false
            btnPlayVolume.text = "${getString(R.string.volume)}\n${nCurrentVolume}"
        }
    }

    private fun initState() {
        filePath = filesDir.absolutePath

        volumeControlStream = volumeType

        // Switch Button 초기화
        fileDrop = sharedPreferences.getBoolean(FILE_DROP, false)
        if (fileDrop) switchButton.selectedTab = 0
        else switchButton.selectedTab = 1
        switchButton.setOnSwitchListener { position, _ ->
            fileDrop = position == 0
            editor.putBoolean(FILE_DROP, fileDrop)
            editor.commit()
        }

        // Volume Content Observer 초기화
        this.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.menu_main_toolbar, menu)
        return true
    }

    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_exit -> dialogService.create("", getString(R.string.exit))
            R.id.list_play -> {
                val intent = Intent(this, ListActivity::class.java).apply {
                    putExtra(getString(R.string.rate), playRate)
                    putExtra(getString(R.string.buffer_size), bufferSize)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }
        return true
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.btn_record -> record()
            R.id.btn_play -> play()
            R.id.btn_record_source -> dialogService.create(getString(R.string.source))
            R.id.btn_record_channel -> dialogService.create(getString(R.string.channel), getString(R.string.record))
            R.id.btn_record_sampleRate -> dialogService.create(getString(R.string.rate), getString(R.string.record))
            R.id.btn_record_bufferSize -> dialogService.create(getString(R.string.buffer_size))
            R.id.btn_play_type -> dialogService.create(getString(R.string.type))
            R.id.btn_play_channel -> dialogService.create(getString(R.string.channel), getString(R.string.play))
            R.id.btn_play_sampleRate -> dialogService.create(getString(R.string.rate), getString(R.string.play))
            R.id.btn_play_volume -> dialogService.create(getString(R.string.volume))
        }
    }

    private fun record() {
        if (!isRecording) {  // 녹음 버튼 클릭 시
            queue = Queue()
            isRecording = true
            mAudioRecord.start(queue!!, fileDrop)
            startRecording()
        } else {  // 정지 버튼 클릭 시
            isRecording = false
            mAudioRecord.stop(this, fileDrop)
            stopRecording()
        }
    }

    private fun startRecording() {
        // Waveform
        waveform.recreate()
        waveform.chunkColor = resources.getColor(R.color.record_red)

        // Record time
        startTime = SystemClock.elapsedRealtime()
        val recordMsg = recordHandler.obtainMessage().apply {
            what = MESSAGE_RECORD
        }
        recordHandler.sendMessage(recordMsg)

        // Ui
        with (binding) {
            textTimer.visibility = View.VISIBLE
            btnRecord.text = getString(R.string.stop)
            imgRecording.visibility = View.VISIBLE
            setAnimation(imgRecording, btnRecord)
            btnPlay.isEnabled = false
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun stopRecording() {
        // Record time
        recordHandler.removeMessages(0)

        // Ui
        with (binding) {
            with (imgRecording) {
                clearAnimation()
                visibility = View.INVISIBLE
            }
            with (btnRecord) {
                clearAnimation()
                text = getString(R.string.record)
            }
            btnPlay.isEnabled = true
        }
    }

    private fun play() {
        if (!isPlaying) {  // 재생 버튼 클릭 시
            isPlaying = true
            mAudioTrack.play(queue!!)
            startPlaying()
        } else {  // 정지 버튼 클릭 시
            isPlaying = false
            mAudioTrack.stop()
            stopPlaying()
        }
    }

    private fun startPlaying() {
        // Waveform
        waveform.recreate()
        waveform.chunkColor = resources.getColor(R.color.play_blue)

        // Play time
        startTime = SystemClock.elapsedRealtime()
        val playMsg = playHandler.obtainMessage().apply {
            what = MESSAGE_PLAY
        }
        playHandler.sendMessage(playMsg)

        // Ui
        with (binding) {
            imgPlaying.visibility = View.VISIBLE
            btnPlay.text = getString(R.string.stop)
            setAnimation(imgPlaying, btnPlay)
            btnRecord.isEnabled = false
        }
    }

    private fun stopPlaying() {
        playHandler.removeMessages(0)

        with (binding) {
            with (imgPlaying) {
                clearAnimation()
                visibility = View.INVISIBLE
            }
            with (btnPlay) {
                clearAnimation()
                text = getString(R.string.play)
            }
            btnRecord.isEnabled = true
        }
    }

    private fun setAnimation(imageView: ImageView, button: Button) {
        val animation: Animation = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 500
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        imageView.startAnimation(animation)
        button.startAnimation(animation)
    }

    val time: String
        get() {
            val nowTime = SystemClock.elapsedRealtime()
            val overTime = nowTime - startTime
            val min = overTime / 1000 / 60
            val sec = overTime / 1000 % 60
            val mSec = overTime % 1000 / 10
            return String.format("%02d : %02d : %02d", min, sec, mSec)
        }

    private var recordHandler: Handler = object : Handler() {
        @SuppressLint("HandlerLeak")
        override fun handleMessage(msg: Message) {
            binding.textTimer.text = time
            waveform.update(recordWave)
            sendEmptyMessage(0)
        }
    }

    private var playHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (!autoStop) {
                binding.textTimer.text = time
                waveform.update(playWave)
                sendEmptyMessage(0)
            } else {
//                myLog.d("autoStop 발생!");
                autoStop = false
                isPlaying = false
                mAudioTrack.stop()

                with (binding) {
                    imgPlaying.clearAnimation()
                    imgPlaying.visibility = View.INVISIBLE
                    btnRecord.isEnabled = true
                    btnPlay.clearAnimation()
                    btnPlay.text = "Play"
                }
                this.removeMessages(0)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun changeTextUi(setting: String, value: String, mode: String = "") {
        with (binding) {
            when (mode) {
                getString(R.string.record) -> {
                    when (setting) {
                        getString(R.string.channel) -> { btnRecordChannel.text = "$setting\n$value" }
                        getString(R.string.rate) -> { btnRecordSampleRate.text = "$setting\n$value" }
                    }
                }
                getString(R.string.play) -> {
                    when (setting) {
                        getString(R.string.channel) -> { btnPlayChannel.text = "$setting\n$value" }
                        getString(R.string.rate) -> { btnPlaySampleRate.text = "$setting\n$value" }
                    }
                }
                else -> {
                    when (setting) {
                        getString(R.string.source) -> { btnRecordSource.text = "$setting\n$value" }
                        getString(R.string.buffer_size) -> { btnRecordBufferSize.text = "$setting\n$value" }
                        getString(R.string.type) -> { btnPlayType.text = "$setting\n$value" }
                        getString(R.string.volume) -> { btnPlayVolume.text = "$setting\n$value" }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.contentResolver.unregisterContentObserver(volumeObserver!!)
    }

    companion object {
        private const val PERMISSION_CODE = 1
        private const val MESSAGE_RECORD = 1
        private const val MESSAGE_PLAY = 2
        private const val DATABASE = "database"
        private const val FILE_DROP = "fileDrop"

        var isRecording = false
        var isPlaying = false
        var autoStop = false

        var filePath = ""

        var source = MediaRecorder.AudioSource.MIC
        var type = AudioAttributes.USAGE_MEDIA
        var recordChannel = AudioFormat.CHANNEL_IN_MONO
        var playChannel = AudioFormat.CHANNEL_OUT_MONO
        var recordRate = 16000
        var playRate = 16000
        var bufferSize = 1024
        var volumeType = AudioManager.STREAM_MUSIC
    }
}