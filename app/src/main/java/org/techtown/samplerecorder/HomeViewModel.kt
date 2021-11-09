package org.techtown.samplerecorder

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    val source: MutableLiveData<String> = MutableLiveData("")
    val recordChannel: MutableLiveData<String> = MutableLiveData("")
    val recordRate: MutableLiveData<String> = MutableLiveData("")
    val bufferSize: MutableLiveData<String> = MutableLiveData("")
    val type: MutableLiveData<String> = MutableLiveData("")
    val playChannel: MutableLiveData<String> = MutableLiveData("")
    val playRate: MutableLiveData<String> = MutableLiveData("")
    val volume: MutableLiveData<String> = MutableLiveData("")

    fun loadInfo(setting: String, value: String, mode: String = "") {
        when (mode) {
            MODE_RECORD -> {
                when (setting) {
                    SETTING_CHANNEL -> { recordChannel.postValue(value) }
                    SETTING_RATE -> { recordRate.postValue(value) }
                }
            }
            MODE_PLAY -> {
                when (setting) {
                    SETTING_CHANNEL -> { playChannel.postValue(value) }
                    SETTING_RATE -> { playRate.postValue(value) }
                }
            }
            else -> {
                when (setting) {
                    SETTING_SOURCE -> { source.postValue(value) }
                    SETTING_BUFFER_SIZE -> { bufferSize.postValue(value) }
                    SETTING_TYPE -> { type.postValue(value) }
                    SETTING_VOLUME -> { volume.postValue(value) }
                }
            }
        }
    }

    companion object {
        private const val MODE_RECORD         = "Record"
        private const val MODE_PLAY           = "Play"
        private const val SETTING_SOURCE      = "Source"
        private const val SETTING_CHANNEL     = "Channel"
        private const val SETTING_RATE        = "Sample Rate"
        private const val SETTING_BUFFER_SIZE = "Buffer Size"
        private const val SETTING_TYPE        = "Play Type"
        private const val SETTING_VOLUME      = "Volume"
    }
}