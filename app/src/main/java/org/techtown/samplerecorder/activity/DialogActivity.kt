package org.techtown.samplerecorder.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.databinding.ActivityDialogBinding
import org.techtown.samplerecorder.util.AppModule.clearFocusAndHideKeyboard
import org.techtown.samplerecorder.util.AppModule.setFocusAndShowKeyboard

class DialogActivity : AppCompatActivity() {

    private val binding       by lazy { ActivityDialogBinding.inflate(layoutInflater) }
    private val editText      by lazy { binding.layoutFileName.etFileName }
    private val layoutFile    by lazy { binding.layoutFileName.root }
    private val layoutSetting by lazy { binding.layoutListSetting.root }

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
        private const val TAG       = "FileNameActivity"
        const val KEY_FILE_NAME     = "name"
        const val KEY_MODE_DIALOG   = "mode"
        const val MODE_FILE_NAME    = "fileName"
        const val MODE_LIST_SETTING = "setting"
    }
}