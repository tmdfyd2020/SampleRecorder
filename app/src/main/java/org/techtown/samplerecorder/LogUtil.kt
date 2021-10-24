package org.techtown.samplerecorder

import android.util.Log

class LogUtil {

    companion object {
        private const val TAG = "SampleRecorder"

        fun v(tag: String, msg: String) {
            val message = buildLogMsg(tag, msg)
            Log.v(TAG, message)
        }

        fun i(tag: String, msg: String) {
            val message = buildLogMsg(tag, msg)
            Log.i(TAG, message)
        }

        fun d(tag: String, msg: String) {
            val message = buildLogMsg(tag, msg)
            Log.d(TAG, message)
        }

        fun w(tag: String, msg: String) {
            val message = buildLogMsg(tag, msg)
            Log.w(TAG, message)
        }

        fun e(tag: String, msg: String) {
            val message = buildLogMsg(tag, msg)
            Log.e(TAG, message)
        }

        private val mStringBuffer = StringBuffer()
        private fun buildLogMsg(tag: String, message: String): String {
            val stackTrace = Thread.currentThread().stackTrace[4]
            mStringBuffer.setLength(0)
            mStringBuffer.append("[ $tag ] ")
            mStringBuffer.append("[ ${stackTrace.methodName}() ] ")
            mStringBuffer.append("[ line ${stackTrace.lineNumber} ] ")
            mStringBuffer.append(": $message")
            return mStringBuffer.toString()
        }
    }
}