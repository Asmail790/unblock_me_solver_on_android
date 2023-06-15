package com.example.unblockmesolver.ML
import android.graphics.RectF

data class Result( @JvmField val classIndex: Int,@JvmField val score: Float,@JvmField val rect: RectF)