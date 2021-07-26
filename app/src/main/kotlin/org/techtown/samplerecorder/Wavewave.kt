package org.techtown.samplerecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.util.*

class Wavewave(context: Context?, attrs: AttributeSet?): View(context,attrs) {
    private var paint = Paint()
    private var amplitudes = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()

    private var radius=6f
    private var w=9f
    private var d=6f

    private var sw=0f
    private var sh=400f

    private var maxSpikes=0

    init{
        paint.color = Color.rgb(244,81,30)

        sw = resources.displayMetrics.widthPixels.toFloat()

        maxSpikes = (sw / (w + d)).toInt()
    }

    fun addAmplitude(amp:Float) {
        Log.d("[MZ]", "addAmplitude 진입")
        var norm = Math.min(amp.toInt() / 7, 400).toFloat()
        amplitudes.add(norm)

        spikes.clear()
        var amps = amplitudes.takeLast(maxSpikes)
        for(i in amplitudes.indices) {

            var left = sw - i * (w + d) // 0f에서 sw - w로 건드렸더니 왼쪽 시작에서 오른쪽 시작으로 바뀜
            var top = sh / 2 - amps[i] / 2
            var right = left + w
            var bottom = top + amps[i]

            spikes.add(RectF(left, top, right, bottom))
        }
        invalidate()
    }

    override fun draw(canvas : Canvas?){
        super.draw(canvas)
        spikes.forEach {
            canvas?.drawRoundRect(it, radius, radius, paint)
        }
    }

}
