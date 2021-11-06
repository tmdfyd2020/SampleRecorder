package org.techtown.samplerecorder

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.provider.Settings
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.techtown.samplerecorder.Audio.Queue
import org.techtown.samplerecorder.Audio.RecordService
import org.techtown.samplerecorder.Audio.RecordService.Companion.CODE_FILE_NAME
import org.techtown.samplerecorder.Audio.RecordService.Companion.recordWave
import org.techtown.samplerecorder.Audio.TrackService
import org.techtown.samplerecorder.Audio.TrackService.Companion.playWave
import org.techtown.samplerecorder.Database.RoomHelper
import org.techtown.samplerecorder.Database.RoomItem
import org.techtown.samplerecorder.Database.RoomItemDao
import org.techtown.samplerecorder.FileNameActivity.Companion.KEY_FILE_NAME
import org.techtown.samplerecorder.List.ItemListActivity
import org.techtown.samplerecorder.Util.DialogService
import org.techtown.samplerecorder.Util.LogUtil
import org.techtown.samplerecorder.Util.VolumeObserver
import org.techtown.samplerecorder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val TAG = this.javaClass.simpleName
    private lateinit var binding: ActivityMainBinding

    private val audioRecord by lazy { RecordService(this) }
    private val audioTrack by lazy { TrackService() }
    private val waveform by lazy { binding.viewWaveForm }
    private val switchButton by lazy { binding.switchButton }
    private val sharedPreferences by lazy { getSharedPreferences(DATABASE, MODE_PRIVATE) }
    private val editor by lazy { sharedPreferences.edit() }
    private var queue: Queue? = null
    @Suppress("DEPRECATION")
    private val volumeObserver by lazy { VolumeObserver(this, Handler()) }
    private val dialogService by lazy { DialogService(this) }

    private val container by lazy { binding.containerMain }
    private val button by lazy { binding.ivMainStartWindow }
    private val logWindow by lazy { binding.layoutMainLogWindow }
    private var logWindowX: Float = 0f
    private var logWindowY: Float = 0f
    private var shortAnimationDuration: Int = 0
    private var currentAnimator: Animator? = null
    private var zoomState: Boolean = false

    private lateinit var helper: RoomHelper

    private var startTime: Long = 0
    private var fileDrop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        helper = Room.databaseBuilder(this, RoomHelper::class.java, "room_items").build()
        itemDAO = helper.roomItemDao()
        syncDatabase()

        permissionCheck()
        initUi()
        initState()
        initListener()
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

        // Log window 첫 시작 위치 설정
        with (binding.containerMain) {
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val containerWidth = width
                    with (binding.layoutMainLogWindow) {
                        viewTreeObserver.addOnGlobalLayoutListener(object :
                            ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                logWindowX = ((containerWidth - width) / 2).toFloat()
                                logWindowY = (height / 2).toFloat()
                                viewTreeObserver.removeOnGlobalLayoutListener(this)
                            }
                        })
                    }
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        var moveX = 0f
        var moveY = 0f
        binding.layoutMainLogWindow.setOnTouchListener { view: View, event: MotionEvent ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    moveX = view.x - event.rawX
                    moveY = view.y - event.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    view.animate()
                        .x(event.rawX + moveX)
                        .y(event.rawY + moveY)
                        .setDuration(0)
                        .start()
                }
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_toolbar, menu)
        return true
    }

    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_exit -> {
                dialogService.create(getString(R.string.exit), "")
            }
            R.id.list_play -> {
                val intent = Intent(this, ItemListActivity::class.java).apply {
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
            R.id.iv_main_start_window -> {
                zoomAnimation(view)
            }
            R.id.iv_main_minimize_popup -> {
                with (binding) {
                    layoutMainLogWindowBody.visibility = View.GONE
                    ivMainMinimizePopup.visibility = View.GONE
                    ivMainMaximizePopup.visibility = View.VISIBLE
                }
            }

            R.id.iv_main_maximize_popup -> {
                with (binding) {
                    layoutMainLogWindowBody.visibility = View.VISIBLE
                    ivMainMinimizePopup.visibility = View.VISIBLE
                    ivMainMaximizePopup.visibility = View.GONE
                }
            }

            R.id.iv_main_close_popup -> {
                zoomAnimation(button)
            }
        }
    }

    private fun record() {
        if (!isRecording) {  // 녹음 버튼 클릭 시
            queue = Queue()
            isRecording = true
            audioRecord.start(queue!!, fileDrop)
            startRecording()
        } else {  // 정지 버튼 클릭 시
            isRecording = false
            audioRecord.stop(this, fileDrop)
            stopRecording()
        }
    }

    @Suppress("DEPRECATION")
    private fun startRecording() {
        LogUtil.d(TAG, "")
        // Waveform
        waveform.recreate()
        waveform.chunkColor = resources.getColor(R.color.red_record)

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
        LogUtil.d(TAG, "")
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
            audioTrack.play(queue!!)
            startPlaying()
        } else {  // 정지 버튼 클릭 시
            isPlaying = false
            audioTrack.stop()
            stopPlaying()
        }
    }

    private fun startPlaying() {
        LogUtil.d(TAG, "")
        // Waveform
        waveform.recreate()
        waveform.chunkColor = resources.getColor(R.color.blue_play)

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
        LogUtil.d(TAG, "")
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
        @SuppressLint("HandlerLeak")
        override fun handleMessage(msg: Message) {
            if (!emptyQueue) {
                binding.textTimer.text = time
                waveform.update(playWave)
                sendEmptyMessage(0)
            } else {
                emptyQueue = false
                play()
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

    fun insertItem(item: RoomItem) {
        CoroutineScope(Dispatchers.IO).launch {
            itemDAO.insert(item)
            syncDatabase()
        }
    }

    private fun syncDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            itemList.clear()
            itemList.addAll(itemDAO.getList())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CODE_FILE_NAME -> {
                    val name = data?.getStringExtra(KEY_FILE_NAME)
                    audioRecord.addItem(name!!, this)
                }
            }
        } else {
            audioRecord.removeFile()
        }
    }

    fun writeLogWindow(msg: String) {
        val totalMsg = "$msg\n${binding.tvMainLogWindow.text}"
        binding.tvMainLogWindow.text = totalMsg
    }

    /**
     * View Expansion and Shrink animation.
     * Start from button x, y to current window's x, y
     * Up to window's original scale
     * @param button start animation view
     */
    private fun zoomAnimation(button: View) {
        currentAnimator?.cancel()

        val startBoundsInt = Rect()
        val finalBoundsInt = Rect()
        val globalOffset = Point()

        button.getGlobalVisibleRect(startBoundsInt)
        container.getGlobalVisibleRect(finalBoundsInt, globalOffset)
        startBoundsInt.offset(-globalOffset.x, -globalOffset.y)
        finalBoundsInt.offset(-globalOffset.x, -globalOffset.y)

        val startBounds = RectF(startBoundsInt)
        val finalBounds = RectF(finalBoundsInt)
        val startScale = startBounds.height() / finalBounds.height()

        logWindow.pivotX = 0f
        logWindow.pivotY = 0f

        when (zoomState) {
            false -> {
                LogUtil.i(TAG, "Animation Open")
                with (logWindow) {
                    visibility = View.VISIBLE
                    bringToFront()
                }
                currentAnimator = AnimatorSet().apply {
                    play(ObjectAnimator.ofFloat(logWindow, View.X, button.x, logWindowX)).apply {  // X 시작 위치, 마지막 위치
                        with(ObjectAnimator.ofFloat(logWindow, View.Y, button.y, logWindowY))  // Y 시작 위치, 마지막 위치
                        with(ObjectAnimator.ofFloat(logWindow, View.SCALE_X, startScale, 1f))  // X 크기
                        with(ObjectAnimator.ofFloat(logWindow, View.SCALE_Y, startScale, 1f))  // Y 크기
                    }
                    duration = shortAnimationDuration.toLong()
                    interpolator = DecelerateInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentAnimator = null
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            currentAnimator = null
                        }
                    })
                    start()
                }
                zoomState = true
            }
            true -> {
                LogUtil.i(TAG, "Animation Close")
                currentAnimator = AnimatorSet().apply {
                    play(ObjectAnimator.ofFloat(logWindow, View.X, button.x)).apply {
                        with(ObjectAnimator.ofFloat(logWindow, View.Y, button.y))
                        with(ObjectAnimator.ofFloat(logWindow, View.SCALE_X, startScale))
                        with(ObjectAnimator.ofFloat(logWindow, View.SCALE_Y, startScale))
                    }
                    duration = shortAnimationDuration.toLong()
                    interpolator = DecelerateInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            logWindow.visibility = View.GONE
                            currentAnimator = null
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            logWindow.visibility = View.GONE
                            currentAnimator = null
                        }
                    })
                    start()
                }
                zoomState = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.contentResolver.unregisterContentObserver(volumeObserver)
    }

    init {
        instance = this
    }

    companion object {
        private const val PERMISSION_CODE = 1
        private const val MESSAGE_RECORD = 1
        private const val MESSAGE_PLAY = 2
        private const val DATABASE = "database"
        private const val FILE_DROP = "fileDrop"

        var isRecording = false
        var isPlaying = false
        var emptyQueue = false

        var filePath = ""  // Internal Memory

        var source = MediaRecorder.AudioSource.MIC
        var type = AudioAttributes.USAGE_MEDIA
        var recordChannel = AudioFormat.CHANNEL_IN_MONO
        var playChannel = AudioFormat.CHANNEL_OUT_MONO
        var recordRate = 16000
        var playRate = 16000
        var bufferSize = 1024
        var volumeType = AudioManager.STREAM_MUSIC

        var itemList: MutableList<RoomItem> = mutableListOf()
        lateinit var itemDAO: RoomItemDao

        private var instance: MainActivity? = null
        fun instance(): MainActivity? { return instance }
    }
}