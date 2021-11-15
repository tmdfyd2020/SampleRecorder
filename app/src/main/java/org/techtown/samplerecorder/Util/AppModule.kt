package org.techtown.samplerecorder.util

import android.annotation.SuppressLint
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import kotlin.math.abs

object AppModule {
    fun dataToShort(byte: ByteArray?): Int {
        val buffer = ByteBuffer.wrap(byte!!)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return abs(buffer.short.toInt()) * 10
    }

    @SuppressLint("SimpleDateFormat")
    fun currentTimeName(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = System.currentTimeMillis()
        return sdf.format(date)
    }

    fun EditText.setFocusAndShowKeyboard(context: Context) {
        this.requestFocus()
        setSelection(this.text.length)
        this.postDelayed({
            val inputMethodManager =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_FORCED)
        }, 100)
    }

    fun EditText.clearFocusAndHideKeyboard(context: Context) {
        this.clearFocus()
        this.postDelayed({
            val inputMethodManager =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
        }, 0)
    }
}