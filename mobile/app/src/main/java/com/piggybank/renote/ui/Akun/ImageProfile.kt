package com.piggybank.renote.ui.Akun

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class ImageProfile(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    private val paint = Paint()
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        val radius = Math.min(width, height) / 2f
        path.reset()
        path.addCircle(width / 2f, height / 2f, radius, Path.Direction.CCW)
        canvas.clipPath(path)
        super.onDraw(canvas)
    }
}