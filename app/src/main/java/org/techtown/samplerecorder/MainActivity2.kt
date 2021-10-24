package org.techtown.samplerecorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.visualizer.amplitude.AudioRecordView
import lib.kingja.switchbutton.SwitchMultiButton
import lib.kingja.switchbutton.SwitchMultiButton.OnSwitchListener
import org.techtown.samplerecorder.List.List
import org.techtown.samplerecorder.Main.AudioRecord
import org.techtown.samplerecorder.Main.AudioTrack
import org.techtown.samplerecorder.Main.Queue
import org.techtown.samplerecorder.Main.myLog

class MainActivity : AppCompatActivity() {
//    private val TAG = this.javaClass.simpleName
//    val MESSAGE_RECORD = 1
//    val MESSAGE_PLAY = 2
//    private var mAudioRecord: AudioRecord? = null
//    private var mAudioTrack: AudioTrack? = null
//    private var view_waveform: AudioRecordView? = null
//    private var switchButton: SwitchMultiButton? = null
//    private var sharedPreferences: SharedPreferences? = null
//    private var editor: Editor? = null
//    private var context: Context? = null
//    private var queue: Queue? = null
//    var btn_record: Button? = null
//    var btnSource: Button? = null
//    var btnRecordChannel: Button? = null
//    var btnRecordRate: Button? = null
//    var btnBufferSize: Button? = null
//    var btn_play: Button? = null
//    var btnType: Button? = null
//    var btnPlayChannel: Button? = null
//    var btnPlayRate: Button? = null
//    var btnVolume: Button? = null
//    private var img_recording: ImageView? = null
//    private var img_playing: ImageView? = null
//    private var text_timer: TextView? = null
//    private var volumeObserver: VolumeContentObserver? = null
//    private var startTime: Long = 0
//    private var fileDrop = false
//    private var first_track = false
//    private var dialogService: DialogService? = null
//    private var mLog: LogUtil.Companion? = null
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        permissionCheck()
//        initState()
//        init()
//        dialogService = DialogService(this)
//        mLog = LogUtil
//    }
//
//    fun initState() {
//        source = MediaRecorder.AudioSource.MIC
//        recordChannel = AudioFormat.CHANNEL_IN_MONO
//        recordRate = 16000
//        bufferSize = 1024
//        type = AudioAttributes.USAGE_MEDIA
//        playChannel = AudioFormat.CHANNEL_OUT_MONO
//        playRate = 16000
//        volumeType = AudioManager.STREAM_MUSIC
//        volumeControlStream = volumeType
//    }
//
//    fun init() {
//        mAudioTrack = AudioTrack()
//        context = applicationContext
//        val toolbar_main = findViewById<Toolbar>(R.id.toolbar_main)
//        setSupportActionBar(toolbar_main)
//        supportActionBar!!.setTitle("음성 녹음")
//        img_recording = findViewById(R.id.img_recording)
//        view_waveform = findViewById(R.id.view_waveForm)
//        text_timer = findViewById(R.id.text__timer)
//        btnSource = findViewById(R.id.btn_record_source)
//        btnRecordChannel = findViewById(R.id.btn_record_channel)
//        btnRecordRate = findViewById(R.id.btn_record_sampleRate)
//        btnBufferSize = findViewById(R.id.btn_record_bufferSize)
//        btn_record = findViewById(R.id.btn_record)
//        img_playing = findViewById(R.id.img_playing)
//        btnType = findViewById(R.id.btn_play_type)
//        btnPlayChannel = findViewById(R.id.btn_play_channel)
//        btnPlayRate = findViewById(R.id.btn_play_sampleRate)
//        btnVolume = findViewById(R.id.btn_play_volume)
//        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
//        val nCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
//        btnVolume.setText("VOLUME\n$nCurrentVolume")
//        btn_play = findViewById(R.id.btn_play)
//        btn_play.setEnabled(false)
//        switchButton = findViewById(R.id.switchButton)
//        switchButton.setOnSwitchListener(OnSwitchListener { position, tabText ->
//            if (position == 0) {  // for file drop on
//                val permission_writePermission = ContextCompat.checkSelfPermission(
//                    this@MainActivity,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE
//                )
//                if (permission_writePermission == PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
//                    myLog.d("WRITE_EXTERNAL_STORAGE 권한 설정 완료")
//                } else {
//                    ActivityCompat.requestPermissions(
//                        this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
//                        201
//                    )
//                }
//                fileDrop = true
//                editor!!.putBoolean("fileState", fileDrop)
//                editor!!.commit()
//            } else if (position == 1) {  // for file drop off
//                fileDrop = false
//                editor!!.putBoolean("fileState", fileDrop)
//                editor!!.commit()
//            }
//        })
//        sharedPreferences = getSharedPreferences("fileDrop", MODE_PRIVATE)
//        editor = sharedPreferences.edit()
//        fileDrop = sharedPreferences.getBoolean("fileState", false)
//        if (fileDrop) {
//            switchButton.setSelectedTab(0)
//        } else {
//            switchButton.setSelectedTab(1)
//        }
//
//        // real time volume change listener
//        volumeObserver = VolumeContentObserver(this, Handler())
//        this.contentResolver.registerContentObserver(
//            Settings.System.CONTENT_URI,
//            true,
//            volumeObserver!!
//        )
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        applicationContext.contentResolver.unregisterContentObserver(volumeObserver!!)
//    }
//
//    fun permissionCheck() {
//        val permission_record =
//            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
//        if (permission_record == PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
//            myLog.d("RECORD_AUDIO 권한 설정 완료")
//        } else {
//            ActivityCompat.requestPermissions(
//                this, arrayOf(Manifest.permission.RECORD_AUDIO),
//                101
//            )
//        }
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        val menuInflater = menuInflater
//        menuInflater.inflate(R.menu.menu_main_toolbar, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            R.id.menu_exit -> dialogService!!.create("", getString(R.string.exit))
//            R.id.list_play -> {
//                val permission_readPermission = ContextCompat.checkSelfPermission(
//                    this@MainActivity,
//                    Manifest.permission.READ_EXTERNAL_STORAGE
//                )
//                if (permission_readPermission == PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
//                    myLog.d("READ_EXTERNAL_STORAGE 권한 설정 완료")
//                } else {
//                    ActivityCompat.requestPermissions(
//                        this@MainActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//                        301
//                    )
//                }
//                val intent = Intent(this, List::class.java)
//                intent.putExtra("sampleRate", playRate)
//                intent.putExtra("bufferSize", bufferSize)
//                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                startActivity(intent)
//            }
//        }
//        return true
//    }
//
//    fun onClick(view: View) {
//        when (view.id) {
//            R.id.btn_record -> record()
//            R.id.btn_play -> play()
//            R.id.btn_record_source -> dialogService!!.create("", getString(R.string.source))
//            R.id.btn_record_channel -> dialogService!!.create(
//                getString(R.string.record),
//                getString(R.string.channel)
//            )
//            R.id.btn_record_sampleRate -> dialogService!!.create(
//                getString(R.string.record),
//                getString(R.string.rate)
//            )
//            R.id.btn_record_bufferSize -> dialogService!!.create(
//                "",
//                getString(R.string.buffer_size)
//            )
//            R.id.btn_play_type -> dialogService!!.create("", getString(R.string.type))
//            R.id.btn_play_channel -> dialogService!!.create(
//                getString(R.string.play),
//                getString(R.string.channel)
//            )
//            R.id.btn_play_sampleRate -> dialogService!!.create(
//                getString(R.string.play),
//                getString(R.string.rate)
//            )
//            R.id.btn_play_volume -> dialogService!!.create("", getString(R.string.volume))
//        }
//    }
//
//    fun record() {
////        myLog.d("method activate");
//        if (isRecording) {  // if "STOP" button clicked,
//            isRecording = false // check : 함수 안으로 집어 넣으면 AudioRecord로 isRecording이 가끔씩 전달되지 않음
//            mAudioRecord!!.stop()
//            mAudioRecord!!.release(context, fileDrop)
//            stopRecording()
//        } else {  // if "RECORD" button clicked,
//            mAudioRecord = AudioRecord()
//            queue = Queue()
//            isRecording = true
//            mAudioRecord!!.init(bufferSize)
//            mAudioRecord!!.start(source, recordChannel, recordRate, queue, fileDrop)
//            LogUtil.i(TAG, source.toString())
//            startRecording()
//        }
//    }
//
//    fun stopRecording() {
////        myLog.d("method activate");
//        recordHandler.removeMessages(0)
//        img_recording!!.clearAnimation()
//        img_recording!!.visibility = View.INVISIBLE
//        btn_record!!.clearAnimation()
//        btn_record!!.text = "Record"
//        btn_record!!.isEnabled = true
//        btn_record!!.background = getDrawable(R.drawable.btn_record_active)
//        btn_play!!.isEnabled = true
//        first_track = true
//    }
//
//    fun startRecording() {
////        myLog.d("method activate");
//        view_waveform!!.recreate()
//        view_waveform!!.chunkColor = resources.getColor(R.color.record_red)
//        startTime = SystemClock.elapsedRealtime()
//        //        Message recordMsg = recordHandler.obtainMessage();
//////        Message recordMsg = new Message();
////        recordMsg.what = MESSAGE_RECORD;
//        val recordMsg = recordHandler.obtainMessage()
//        recordMsg.what = MESSAGE_RECORD
//        recordHandler.sendMessage(recordMsg)
//        btn_record!!.text = "Stop"
//        btnBufferSize!!.isEnabled = true
//        img_recording!!.visibility = View.VISIBLE
//        text_timer!!.visibility = View.VISIBLE
//
//        // animation set at recording image emerge
//        val animation: Animation = AlphaAnimation(1, 0)
//        animation.duration = 500
//        animation.interpolator = LinearInterpolator()
//        animation.repeatCount = Animation.INFINITE
//        animation.repeatMode = Animation.REVERSE
//        img_recording!!.startAnimation(animation)
//        btn_record!!.startAnimation(animation)
//    }
//
//    fun play() {
////        myLog.d("method activate");
//        if (isPlaying) {  // if "STOP" button clicked,
//            isPlaying = false
//            mAudioTrack!!.stop()
//            mAudioTrack!!.release()
//            stopPlaying()
//        } else {  // if "PLAY" button clicked,
//            mAudioTrack = AudioTrack()
//            isPlaying = true
//            mAudioTrack!!.init(bufferSize)
//            mAudioTrack!!.play(type, playChannel, playRate, queue)
//            startPlaying()
//        }
//    }
//
//    fun stopPlaying() {
////        myLog.d("method activate");
//        playHandler.removeMessages(0)
//        img_playing!!.clearAnimation()
//        img_playing!!.visibility = View.INVISIBLE
//        btn_record!!.isEnabled = true
//        btn_play!!.clearAnimation()
//        btn_play!!.text = "Play"
//    }
//
//    fun startPlaying() {
////        myLog.d("method activate");
//
//        // use this emerging bug like delay 300 at first play
////        if (first_track) {
////            try {
////                Thread.sleep(300);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
////            first_track = false;
////        }
//        view_waveform!!.recreate()
//        view_waveform!!.chunkColor = resources.getColor(R.color.play_blue)
//        startTime = SystemClock.elapsedRealtime()
//        val playMsg = playHandler.obtainMessage()
//        playMsg.what = MESSAGE_PLAY
//        playHandler.sendMessage(playMsg)
//        img_playing!!.visibility = View.VISIBLE
//        btn_record!!.isEnabled = false
//        btn_play!!.text = "Stop"
//        val animation: Animation = AlphaAnimation(1, 0)
//        animation.duration = 500
//        animation.interpolator = LinearInterpolator()
//        animation.repeatCount = Animation.INFINITE
//        animation.repeatMode = Animation.REVERSE
//        img_playing!!.startAnimation(animation)
//        btn_play!!.startAnimation(animation)
//    }
//
//    val time: String
//        get() {
//            val nowTime = SystemClock.elapsedRealtime()
//            val overTime = nowTime - startTime
//            val min = overTime / 1000 / 60
//            val sec = overTime / 1000 % 60
//            val mSec = overTime % 1000 / 10
//            return String.format("%02d : %02d : %02d", min, sec, mSec)
//        }
//    var recordHandler: Handler = object : Handler() {
//        @SuppressLint("HandlerLeak")
//        override fun handleMessage(msg: Message) {
//            text_timer!!.text = time
//            view_waveform!!.update(AudioRecord.dataMax)
//            sendEmptyMessage(0)
//        }
//    }
//    var playHandler: Handler = object : Handler() {
//        override fun handleMessage(msg: Message) {
//            if (!autoStop) {
//                text_timer!!.text = time
//                view_waveform!!.update(AudioTrack.dataMax)
//                sendEmptyMessage(0)
//            } else {
////                myLog.d("autoStop 발생!");
//                autoStop = false
//                isPlaying = false
//                mAudioTrack!!.stop()
//                mAudioTrack!!.release()
//                img_playing!!.clearAnimation()
//                img_playing!!.visibility = View.INVISIBLE
//                btn_record!!.isEnabled = true
//                btn_play!!.clearAnimation()
//                btn_play!!.text = "Play"
//                this.removeMessages(0)
//            }
//        }
//    }
//
//    companion object {
//        var isRecording = false
//        var isPlaying = false
//        var autoStop = false
//        var source = 0
//        var recordChannel = 0
//        var recordRate = 0
//        var bufferSize = 0
//        var type = 0
//        var playChannel = 0
//        var playRate = 0
//        var volumeType = 0
//    }
}