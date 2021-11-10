package org.techtown.samplerecorder.home

import android.annotation.SuppressLint
import android.content.Context.AUDIO_SERVICE
import android.content.Context.MODE_PRIVATE
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.techtown.samplerecorder.audio.Queue
import org.techtown.samplerecorder.audio.RecordService
import org.techtown.samplerecorder.audio.TrackService
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.util.DialogService.Companion.dialogs
import org.techtown.samplerecorder.util.LogUtil
import org.techtown.samplerecorder.util.VolumeObserver
import org.techtown.samplerecorder.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val waveform     by lazy { binding.viewWaveForm }
    private val switchButton by lazy { binding.switchButton }

    @Suppress("DEPRECATION")
    private val volumeObserver by lazy { VolumeObserver(requireContext(), Handler()) }
    private val audioRecord    by lazy { RecordService(requireContext()) }
    private val audioTrack     by lazy { TrackService() }
    private var queue: Queue? = null
    private var fileDrop = false
    private var startTime: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initState()
        initUi()
        setOnClickListener()
    }

    private fun initState() {
        // View Model initialization
        binding.lifecycleOwner = this
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        binding.vm = viewModel

        activity?.volumeControlStream = volumeType

        // Switch Button initialization
        val sharedPreferences = requireContext().getSharedPreferences(KEY_DATABASE, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        fileDrop = sharedPreferences.getBoolean(KEY_FILE_DROP, false)
        LogUtil.i(TAG, "Current File drop state : $fileDrop")
        if (fileDrop) switchButton.selectedTab = 0
        else switchButton.selectedTab = 1
        switchButton.setOnSwitchListener { position, _ ->
            fileDrop = position == 0
            with (editor) {
                putBoolean(KEY_FILE_DROP, fileDrop)
                apply()
            }
            LogUtil.i(TAG, "Change file drop state : $fileDrop")
        }

        // Volume Content Observer 초기화
        requireContext().contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver
        )
    }

    @SuppressLint("SetTextI18n")
    private fun initUi() {
        val audioManager = activity?.getSystemService(AUDIO_SERVICE) as AudioManager
        val nCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        with (viewModel) {
            loadInfo(getString(R.string.source), getString(R.string.mic))
            loadInfo(getString(R.string.channel), getString(R.string.mono), getString(R.string.record))
            loadInfo(getString(R.string.rate), getString(R.string.rate_16000), getString(R.string.record))
            loadInfo(getString(R.string.buffer_size), getString(R.string.buffer_size_1024))
            loadInfo(getString(R.string.type), getString(R.string.media))
            loadInfo(getString(R.string.channel), getString(R.string.mono), getString(R.string.play))
            loadInfo(getString(R.string.rate), getString(R.string.rate_16000), getString(R.string.play))
            loadInfo(getString(R.string.source), getString(R.string.mic))
            loadInfo(getString(R.string.volume), nCurrentVolume.toString())
        }
        binding.btnPlay.isEnabled = false
    }

    private fun setOnClickListener() {
        val context = this
        with (binding) {
            containerRecordSettings.children.forEach { btn ->
                btn.setOnClickListener(context)
            }
            containerPlaySettings.children.forEach { btn ->
                btn.setOnClickListener(context)
            }
            btnRecord.setOnClickListener(context)
            btnPlay.setOnClickListener(context)
        }
    }

    override fun onClick(view: View) {
        childFragmentManager.let {
            when (view.id) {
                R.id.btn_record -> record()
                R.id.btn_play -> play()
                R.id.btn_record_source -> dialogs(getString(R.string.source)).show(it, getString(R.string.source))
                R.id.btn_record_channel -> dialogs(getString(R.string.channel), getString(R.string.record)).show(it, getString(
                    R.string.channel))
                R.id.btn_record_sampleRate -> dialogs(getString(R.string.rate), getString(R.string.record)).show(it, getString(
                    R.string.rate))
                R.id.btn_record_bufferSize -> dialogs(getString(R.string.buffer_size)).show(it, getString(
                    R.string.buffer_size))
                R.id.btn_play_type -> dialogs(getString(R.string.type)).show(it, getString(R.string.type))
                R.id.btn_play_channel -> dialogs(getString(R.string.channel), getString(R.string.play)).show(it, getString(
                    R.string.channel))
                R.id.btn_play_sampleRate -> dialogs(getString(R.string.rate), getString(R.string.play)).show(it, getString(
                    R.string.rate))
                R.id.btn_play_volume -> dialogs(getString(R.string.volume)).show(it, getString(R.string.volume))
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
            audioRecord.stop()
            stopRecording()
        }
    }

    @Suppress("DEPRECATION")
    private fun startRecording() {
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
            imgRecording.visibility = View.VISIBLE
            setAnimation(imgRecording, btnRecord)
            textTimer.visibility = View.VISIBLE
            btnRecord.text = getString(R.string.stop)
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
            audioTrack.play(queue!!)
            startPlaying()
        } else {  // 정지 버튼 클릭 시
            isPlaying = false
            audioTrack.stop()
            stopPlaying()
        }
    }

    private fun startPlaying() {
        // Play time
        startTime = SystemClock.elapsedRealtime()
        val playMsg = playHandler.obtainMessage().apply {
            what = MESSAGE_PLAY
        }
        playHandler.sendMessage(playMsg)

        // Waveform
        waveform.recreate()
        waveform.chunkColor = resources.getColor(R.color.blue_play)

        // Ui
        with (binding) {
            imgPlaying.visibility = View.VISIBLE
            btnRecord.isEnabled = false
            btnPlay.text = getString(R.string.stop)
            setAnimation(imgPlaying, btnPlay)
        }
    }

    private fun stopPlaying() {
        // Play time
        playHandler.removeMessages(0)

        // Ui
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

    @SuppressLint("SetTextI18n")
    fun changeTextUi(setting: String, value: String, mode: String = "") {
        viewModel.loadInfo(setting, value, mode)
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
            waveform.update(RecordService.recordWave)
            sendEmptyMessage(0)
        }
    }

    private var playHandler: Handler = object : Handler() {
        @SuppressLint("HandlerLeak")
        override fun handleMessage(msg: Message) {
            if (!emptyQueue) {
                binding.textTimer.text = time
                waveform.update(TrackService.playWave)
                sendEmptyMessage(0)
            } else {
                emptyQueue = false
                play()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
        fun instance() = HomeFragment()
        lateinit var viewModel: HomeViewModel

        private const val MESSAGE_RECORD = 1
        private const val MESSAGE_PLAY   = 2
        private const val KEY_DATABASE   = "database"
        private const val KEY_FILE_DROP  = "fileDrop"

        var isRecording = false
        var isPlaying = false
        var emptyQueue = false

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