package org.techtown.samplerecorder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import org.techtown.samplerecorder.util.AppModule.clearFocusAndHideKeyboard
import org.techtown.samplerecorder.util.AppModule.setFocusAndShowKeyboard
import org.techtown.samplerecorder.databinding.ActivityFileNameBinding

class FileNameActivity : AppCompatActivity() {

    private val binding by lazy { ActivityFileNameBinding.inflate(layoutInflater) }
    private val editText by lazy { binding.etFileName }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        editText.setFocusAndShowKeyboard(this)
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
        private const val TAG = "FileNameActivity"
        const val KEY_FILE_NAME = "name"
    }
}