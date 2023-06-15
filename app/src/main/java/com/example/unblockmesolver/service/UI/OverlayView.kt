package com.example.unblockmesolver.service.UI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class OverlayView(context: Context, private val matrix:Matrix): View(context) {
    private var func: ((Canvas) -> Unit)? = null



    fun drawCanvas(f:((Canvas) -> Unit)) {
        func = f
        invalidate()
    }

    fun clear() {
        func = null
        invalidate()
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        func?.let { it(canvas!!) }
    }



}