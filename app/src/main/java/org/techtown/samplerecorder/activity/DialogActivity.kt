package org.techtown.samplerecorder.activity

import android.app.Activity
import android.content.Intent
import android.media.AudioFormat
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.github.angads25.toggle.interfaces.OnToggledListener
import com.github.angads25.toggle.model.ToggleableView
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.databinding.ActivityDialogBinding
import org.techtown.samplerecorder.home.HomeFragment
import org.techtown.samplerecorder.home.HomeFragment.Companion.playChannel
import org.techtown.samplerecorder.home.HomeFragment.Companion.playRate
import org.techtown.samplerecorder.home.HomeViewModel.Companion.MODE_PLAY
import org.techtown.samplerecorder.util.AppModule.clearFocusAndHideKeyboard
import org.techtown.samplerecorder.util.AppModule.setFocusAndShowKeyboard
import org.techtown.samplerecorder.util.LogUtil

class DialogActivity : AppCompatActivity() {

    private val binding       by lazy { ActivityDialogBinding.inflate(layoutInflater) }
    private val editText      by lazy { binding.layoutFileName.etFileName }
    private val layoutFile    by lazy { binding.layoutFileName.root }
    private val layoutSetting by lazy { binding.layoutListSetting.root }
    private val toggle        by lazy { binding.layoutListSetting.toggleListSettingChannel }
    private val picker        by lazy { binding.layoutListSetting.pickerListSettingRate }
    private val homeFragment  by lazy { HomeFragment.instance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        intent?.let {
            when(it.getStringExtra(KEY_MODE_DIALOG)) {
                MODE_FILE_NAME -> {
                    editText.setFocusAndShowKeyboard(this)
                    layoutSetting.visibility = View.GONE
                    layoutFile.visibility = View.VISIBLE
                }
                MODE_LIST_SETTING -> {
                    layoutSetting.visibility = View.VISIBLE
                    layoutFile.visibility = View.GONE
                }
            }
        }
        initialize()
    }

    private fun initialize() {
        // Toggle button initialization
        when (playChannel) {
            AudioFormat.CHANNEL_OUT_MONO -> {
                toggle.isOn = true
            }
            AudioFormat.CHANNEL_OUT_STEREO -> {
                toggle.isOn = false
            }
        }

        toggle.setOnToggledListener { _, isOn ->
            when (isOn) {
                true -> {  // stereo -> mono
                    playChannel = AudioFormat.CHANNEL_OUT_MONO
                    LogUtil.d(TAG, "Change play channel to mono $playChannel")
                }
                false -> {  // mono -> stereo
                    playChannel = AudioFormat.CHANNEL_OUT_STEREO
                    LogUtil.d(TAG, "Change play channel to stereo $playChannel")
                }
            }
        }

        // String picker initialization
        when (playRate) {
            resources.getInteger(R.integer.rate_8000) -> picker.value = VALUE_8000
            resources.getInteger(R.integer.rate_11025) -> picker.value = VALUE_11025
            resources.getInteger(R.integer.rate_16000) -> picker.value = VALUE_16000
            resources.getInteger(R.integer.rate_22050) -> picker.value = VALUE_22050
            resources.getInteger(R.integer.rate_44100) -> picker.value = VALUE_44100
        }

        val list = resources.getStringArray(R.array.rate)
        with (picker) {
            minValue = 1
            maxValue = list.size
            displayedValues = list
            wrapSelectorWheel = false
            setOnValueChangedListener { _, _, newVal ->
                with (resources) {
                    when (newVal) {
                        VALUE_8000 -> playRate = getInteger(R.integer.rate_8000)
                        VALUE_11025 -> playRate = getInteger(R.integer.rate_11025)
                        VALUE_16000 -> playRate = getInteger(R.integer.rate_16000)
                        VALUE_22050 -> playRate = getInteger(R.integer.rate_22050)
                        VALUE_44100 -> playRate = getInteger(R.integer.rate_44100)
                    }
                }
            }
        }
    }

    fun onClick(button: View) {
        editText.clearFocusAndHideKeyboard(this)
        when (button.id) {
            R.id.btn_file_name_save -> {
                val name = editText.text.toString()
                val intent = Intent()
                intent.putExtra(KEY_FILE_NAME, name)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            R.id.btn_file_name_back -> {
                finish()
            }
        }
    }

    companion object {
        private const val TAG         = "FileNameActivity"
        const val KEY_FILE_NAME       = "name"
        const val KEY_MODE_DIALOG     = "mode"
        const val MODE_FILE_NAME      = "fileName"
        const val MODE_LIST_SETTING   = "setting"

        private const val VALUE_8000  = 1
        private const val VALUE_11025 = 2
        private const val VALUE_16000 = 3
        private const val VALUE_22050 = 4
        private const val VALUE_44100 = 5
    }
}