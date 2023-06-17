package com.example.unblockmesolver.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class Screenshoter(
    val context: Context,
    val data:Pair<Intent,Int>,
    val maxImages:Int = 5,
): DefaultLifecycleObserver {

    private val mMediaProjection: MediaProjection
    private val mImageReader: ImageReader
    private val mVirtualDisplay: VirtualDisplay
    private val screenSize:Pair<Int,Int>
    private val DPI:Int

    @SuppressLint("WrongConstant")
    private fun createImageReader() = ImageReader.newInstance(
        screenSize.first,
        screenSize.second,
        PixelFormat.RGBA_8888,
        maxImages
    )

    init {
        DPI = Resources.getSystem().configuration.densityDpi

        val windowManger =  ( context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        screenSize = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                Pair(
                    windowManger.currentWindowMetrics.bounds.width(),
                    windowManger.currentWindowMetrics.bounds.height()
                )
            else -> {
                DisplayMetrics().let {
                    windowManger.defaultDisplay.getRealMetrics(it)
                   Pair(it.widthPixels,it.heightPixels)
                }
            }
        }

        val mMediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val resultCode = data.second
        val resultData = data.first
        mMediaProjection  = mMediaProjectionManager.getMediaProjection(resultCode, resultData);
        mImageReader = createImageReader()
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("fakeit",
            mImageReader.width,
            mImageReader.height,
            DPI,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mImageReader.surface,
            null,
          null
        )
    }
    fun requestScreenshot(): Bitmap {

        return mImageReader.acquireLatestImage().let {
            val width = it.width
            val height = it.height
            val planes = it.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap: Bitmap =
                Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
            bitmap.copyPixelsFromBuffer(buffer)
            it.close()
            bitmap
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        mImageReader.close()
        mVirtualDisplay.release()
       mMediaProjection.stop()
    }

}