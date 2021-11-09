package org.techtown.samplerecorder.util

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Bundle
import android.text.Html.fromHtml
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.DialogFragment
import org.techtown.samplerecorder.home.HomeFragment
import org.techtown.samplerecorder.home.HomeFragment.Companion.bufferSize
import org.techtown.samplerecorder.home.HomeFragment.Companion.playChannel
import org.techtown.samplerecorder.home.HomeFragment.Companion.playRate
import org.techtown.samplerecorder.home.HomeFragment.Companion.recordChannel
import org.techtown.samplerecorder.home.HomeFragment.Companion.recordRate
import org.techtown.samplerecorder.home.HomeFragment.Companion.source
import org.techtown.samplerecorder.home.HomeFragment.Companion.type
import org.techtown.samplerecorder.home.HomeFragment.Companion.volumeType
import org.techtown.samplerecorder.MainActivity
import org.techtown.samplerecorder.R

class DialogService private constructor(
    private val setting: String,
    private val mode: String) : DialogFragment() {

    private val homeFragment by lazy { HomeFragment.instance() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(
                it,
                android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
            )
            classificationDialog(builder).apply {
                window?.setGravity(Gravity.CENTER)
            }
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun classificationDialog(builder: AlertDialog.Builder) : Dialog {
        return when (setting) {
            getString(R.string.exit) -> {
                return exitDialog(builder)
            }
            getString(R.string.source) -> {
                return sourceDialog(builder)
            }
            getString(R.string.channel) -> {
                return channelDialog(mode, builder)
            }
            getString(R.string.rate) -> {
                return rateDialog(mode, builder)
            }
            getString(R.string.buffer_size) -> {
                return bufferSizeDialog(builder)
            }
            getString(R.string.type) -> {
                return typeDialog(builder)
            }
            getString(R.string.volume) -> {
                return volumeDialog(builder)
            }
            else -> {
                return exitDialog(builder)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun exitDialog(builder: AlertDialog.Builder) : Dialog {
        builder.setTitle(getString(R.string.exit))
            .setMessage(getString(R.string.exit_message))
            .setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_exit))
            .setPositiveButton(fromHtml(
                "<font color='${getColor(requireContext(), R.color.blue_exit_yes)}'>${getString(R.string.yes)}</font>")
            ) { _, _ -> ActivityCompat.finishAffinity(context as MainActivity) }
            .setNegativeButton(fromHtml(
                "<font color='${getColor(requireContext(), R.color.red_exit_no)}'>${getString(R.string.no)}</font>")
            ) { dialog, _ -> dialog.cancel() }
        return builder.create()
    }

    @SuppressLint("SetTextI18n")
    private fun sourceDialog(builder: AlertDialog.Builder) : Dialog {
        val array = resources.getStringArray(R.array.source)
        builder.setTitle(getString(R.string.source))
            .setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_source))
            .setSingleChoiceItems(array, sourceIndex) { _, which ->
                when (array[which]) {
                    getString(R.string.defaults) -> {
                        source = MediaRecorder.AudioSource.DEFAULT
                        homeFragment.changeTextUi(getString(R.string.source), getString(R.string.defaults))
                    }
                    getString(R.string.mic) -> {
                        source = MediaRecorder.AudioSource.MIC
                        homeFragment.changeTextUi(getString(R.string.source), getString(R.string.mic))
                    }
                    getString(R.string.voice_communication) -> {
                        source = MediaRecorder.AudioSource.VOICE_COMMUNICATION
                        homeFragment.changeTextUi(getString(R.string.source), getString(R.string.voice_communication))
                    }
                    getString(R.string.voice_performance) -> {
                        source = MediaRecorder.AudioSource.VOICE_PERFORMANCE
                        homeFragment.changeTextUi(getString(R.string.source), getString(R.string.voice_performance))
                    }
                    getString(R.string.voice_recognition) -> {
                        source = MediaRecorder.AudioSource.VOICE_RECOGNITION
                        homeFragment.changeTextUi(getString(R.string.source), getString(R.string.voice_recognition))
                    }
                    getString(R.string.unprocessed) -> {
                        source = MediaRecorder.AudioSource.UNPROCESSED
                        homeFragment.changeTextUi(getString(R.string.source), getString(R.string.unprocessed))
                    }
                }
                sourceIndex = which
            }
        return builder.create()
    }

    @SuppressLint("SetTextI18n")
    private fun channelDialog(mode: String, builder: AlertDialog.Builder) : Dialog {
        val array = resources.getStringArray(R.array.channel)
        builder.setTitle(getString(R.string.channel))
        when (mode) {
            getString(R.string.record) -> {
                builder.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_record_channel))
                    .setSingleChoiceItems(array, recordChannelIndex) { _, which ->
                        when (array[which]) {
                            getString(R.string.mono) -> {
                                recordChannel = AudioFormat.CHANNEL_IN_MONO
                                homeFragment.changeTextUi(getString(R.string.channel), getString(R.string.mono), mode)
                            }
                            getString(R.string.stereo) -> {
                                recordChannel = AudioFormat.CHANNEL_IN_STEREO
                                homeFragment.changeTextUi(getString(R.string.channel), getString(R.string.stereo), mode)
                            }
                        }
                        recordChannelIndex = which
                    }
            }
            getString(R.string.play) -> {
                builder.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_channel))
                    .setSingleChoiceItems(array, playChannelIndex) { _, which ->
                        when (array[which]) {
                            getString(R.string.mono) -> {
                                playChannel = AudioFormat.CHANNEL_OUT_MONO
                                homeFragment.changeTextUi(getString(R.string.channel), getString(R.string.mono), mode)
                            }
                            getString(R.string.stereo) -> {
                                playChannel = AudioFormat.CHANNEL_OUT_STEREO
                                homeFragment.changeTextUi(getString(R.string.channel), getString(R.string.stereo), mode)
                            }
                        }
                        playChannelIndex = which
                    }
            }
        }
        return builder.create()
    }

    @SuppressLint("SetTextI18n")
    private fun rateDialog(mode: String, builder: AlertDialog.Builder) : Dialog {
        val array = resources.getStringArray(R.array.rate)
        builder.setTitle(getString(R.string.rate))
        when (mode) {
            getString(R.string.record) -> {
                builder.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_record_samplerate))
                    .setSingleChoiceItems(array, recordRateIndex) { _, which ->
                        when (array[which]) {
                            getString(R.string.rate_8000) -> {
                                recordRate = resources.getInteger(R.integer.rate_8000)
                                homeFragment.changeTextUi(getString(R.string.rate), getString(R.string.rate_8000), mode)
                            }
                            getString(R.string.rate_11025) -> {
                                recordRate = resources.getInteger(R.integer.rate_11025)
                                homeFragment.changeTextUi(getString(R.string.rate), getString(R.string.rate_11025), mode)
                            }
                            getString(R.string.rate_16000) -> {
                                recordRate = resources.getInteger(R.integer.rate_16000)
                                homeFragment.changeTextUi(getString(R.string.rate), getString(R.string.rate_16000), mode)
                            }
                            getString(R.string.rate_22050) -> {
                                recordRate = resources.getInteger(R.integer.rate_22050)
                                homeFragment.changeTextUi(getString(R.string.rate), getString(R.string.rate_22050), mode)
                            }
                            getString(R.string.rate_44100) -> {
                                recordRate = resources.getInteger(R.integer.rate_44100)
                                homeFragment.changeTextUi(getString(R.string.rate), getString(R.string.rate_44100), mode)
                            }
                        }
                        recordRateIndex = which
                    }
            }
            getString(R.string.play) -> {
                builder.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_samplerate))
                    .setSingleChoiceItems(array, playRateIndex) { _, which ->
                        when (array[which]) {
                            getString(R.string.rate_8000) -> {
                                playRate = resources.getInteger(R.integer.rate_8000)
                                homeFragment.changeTextUi(getString(R.string.rate), getString(R.string.rate_8000), mode)
                            }
                            getString(R.string.rate_11025) -> {
                                playRate = resources.getInteger(R.integer.rate_11025)
                                homeFragment.changeTextUi(getString(R.string.rate), getString(R.string.rate_11025), mode)
                            }
                            getString(R.string.rate_16000) -> {
                                playRate = resources.getInteger(R.integer.rate_16000)
                                homeFragment.changeTextUi(getString(R.string.rate), getString(R.string.rate_16000), mode)
                            }
                            getString(R.string.rate_22050) -> {
                                playRate = resources.getInteger(R.integer.rate_22050)
                                homeFragment.changeTextUi(getString(R.string.rate), getString(R.string.rate_22050), mode)
                            }
                            getString(R.string.rate_44100) -> {
                                playRate = resources.getInteger(R.integer.rate_44100)
                                homeFragment.changeTextUi(getString(R.string.rate), getString(R.string.rate_44100), mode)
                            }
                        }
                        playRateIndex = which
                    }
            }
        }
        return builder.create()
    }

    @SuppressLint("SetTextI18n")
    private fun bufferSizeDialog(builder: AlertDialog.Builder) : Dialog {
        val array = resources.getStringArray(R.array.buffer)
        builder.setTitle(getString(R.string.buffer_size))
            .setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_buffersize))
            .setSingleChoiceItems(array, bufferSizeIndex) { _, which ->
                when (array[which]) {
                    getString(R.string.buffer_size_512) -> {
                        bufferSize = resources.getInteger(R.integer.buffer_512)
                        homeFragment.changeTextUi(getString(R.string.buffer_size), getString(R.string.buffer_size_512))
                    }
                    getString(R.string.buffer_size_1024) -> {
                        bufferSize = resources.getInteger(R.integer.buffer_1024)
                        homeFragment.changeTextUi(getString(R.string.buffer_size), getString(R.string.buffer_size_1024))
                    }
                    getString(R.string.buffer_size_2048) -> {
                        bufferSize = resources.getInteger(R.integer.buffer_2048)
                        homeFragment.changeTextUi(getString(R.string.buffer_size), getString(R.string.buffer_size_2048))
                    }
                }
                bufferSizeIndex = which
            }
        return builder.create()
    }

    @SuppressLint("SetTextI18n")
    private fun typeDialog(builder: AlertDialog.Builder) : Dialog {
        val array = resources.getStringArray(R.array.type)
        builder.setTitle(getString(R.string.type))
            .setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_type))
            .setSingleChoiceItems(array, typeIndex) { _, which ->
                when (array[which]) {
                    getString(R.string.ring) -> {
                        type = AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                        volumeType = AudioManager.STREAM_RING
                        homeFragment.changeTextUi(getString(R.string.type), getString(R.string.ring))
                    }
                    getString(R.string.media) -> {
                        type = AudioAttributes.USAGE_MEDIA
                        volumeType = AudioManager.STREAM_MUSIC
                        homeFragment.changeTextUi(getString(R.string.type), getString(R.string.media))
                    }
                    getString(R.string.alarm) -> {
                        type = AudioAttributes.USAGE_ALARM
                        volumeType = AudioManager.STREAM_ALARM
                        homeFragment.changeTextUi(getString(R.string.type), getString(R.string.alarm))
                    }
                    getString(R.string.notification) -> {
                        type = AudioAttributes.USAGE_NOTIFICATION
                        volumeType = AudioManager.STREAM_NOTIFICATION
                        homeFragment.changeTextUi(getString(R.string.type), getString(R.string.notification))
                    }
                    getString(R.string.system) -> {
                        type = AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
                        volumeType = AudioManager.STREAM_SYSTEM
                        homeFragment.changeTextUi(getString(R.string.type), getString(R.string.system))
                    }
                }
                typeIndex = which
                activity?.volumeControlStream = volumeType
            }
        return builder.create()
    }

    @SuppressLint("InflateParams")
    private fun volumeDialog(builder: AlertDialog.Builder) : Dialog {
        val viewSeekbar = activity?.layoutInflater?.inflate(R.layout.seekbar_volume, null)?.apply {
            val seekBar = findViewById<View>(R.id.seekbar_volume) as SeekBar
            val textView = findViewById<View>(R.id.text_seekbar) as TextView
            val imageView = findViewById<View>(R.id.img_seekbar) as ImageView
            seekbarSetting(seekBar, textView, imageView)
        }
        builder.setTitle(getString(R.string.volume)).setView(viewSeekbar)
        return builder.create()
    }

    private fun seekbarSetting(seekBar: SeekBar, volumeText: TextView, image: ImageView) {
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(volumeType)
        seekBar.min = 1
        seekBar.max = audioManager.getStreamMaxVolume(volumeType)
        seekBar.progress = currentVolume
        seekBarUi(currentVolume, volumeText, image)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(volumeType, progress, 0)
                seekBarUi(progress, volumeText, image)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) { }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }
        })
    }

    private fun seekBarUi(volume: Int, volumeText: TextView, image: ImageView) {
        when {
            volume >= 13 -> {
                volumeText.setTextColor(requireContext().resources.getColor(R.color.red_record))
                image.setImageResource(R.drawable.ic_volume_loud)
            }
            volume in 10..12 -> {
                volumeText.setTextColor(requireContext().resources.getColor(R.color.blue_play))
                image.setImageResource(R.drawable.ic_volume_loud)
            }
            else -> {
                volumeText.setTextColor(requireContext().resources.getColor(R.color.blue_play))
                image.setImageResource(R.drawable.ic_volume_small)
            }
        }
        volumeText.text = volume.toString()
    }


    companion object {
        private const val TAG = "DialogService2"

        private var sourceIndex        = 1
        private var recordChannelIndex = 0
        private var playChannelIndex   = 0
        private var recordRateIndex    = 2
        private var playRateIndex      = 2
        private var bufferSizeIndex    = 1
        private var typeIndex          = 1

        fun dialogs(setting: String, mode: String = "") = DialogService(setting, mode)
    }

}

