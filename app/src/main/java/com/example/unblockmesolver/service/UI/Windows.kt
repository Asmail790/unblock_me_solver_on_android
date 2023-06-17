package com.example.unblockmesolver.service.UI

import android.content.Context
import android.graphics.*
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.unblockmesolver.R




sealed  class NextStepInformation()
data class NextStep( @JvmField val currentBlockPosition:RectF, @JvmField val toCurrentBlockPosition:RectF, @JvmField val explation:String): NextStepInformation()
data class DetectionFailure(@JvmField val cause:String): NextStepInformation()
data class SolverFailure( @JvmField val cause:String): NextStepInformation()




// action that return triangles,
class UI(
    val context: Context,
    val onExit:(View) -> Unit,
    val onGuide:(View) -> Unit
):DefaultLifecycleObserver {

    companion object {
        const val TAG = "UI"
    }

    private val controlPanel: View
         val overlayView:OverlayView
    private val controlPanelParams: WindowManager.LayoutParams
    private val ovelayViewParms:WindowManager.LayoutParams
    private val windowManager:WindowManager
    private val layoutInflater: LayoutInflater
    private val  systemType:Int

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        systemType = if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY

    }
    init {
        controlPanelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            systemType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        controlPanelParams.gravity = Gravity.BOTTOM

        // getting a LayoutInflater
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // inflating the view with the custom layout we created
        // inflating the view with the custom layout we created
        controlPanel = layoutInflater.inflate(R.layout.service_control_panel, null)
        windowManager.addView(controlPanel, controlPanelParams)
        controlPanel.findViewById<Button>(R.id.guide_button).setOnClickListener(onGuide)
        controlPanel.findViewById<Button>(R.id.exit_button).setOnClickListener(onExit)
    }

    init {
        ovelayViewParms = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            systemType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE  or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )


        overlayView = OverlayView(context, Matrix())

        windowManager.addView(overlayView, ovelayViewParms)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            overlayView.windowInsetsController!!.hide(WindowInsets.Type.statusBars())
        }
    }

    fun makeDark() {
        overlayView.drawCanvas { it.drawColor(Color.BLACK) }
    }


     fun draw(guide: NextStepInformation){
        overlayView.drawCanvas {
            val textPainter = Paint().apply {
                color = (Color.BLACK)
                style = (Paint.Style.FILL)
                textSize = 20F
            }

            val rectPainterFrom =  Paint().apply {
                color = (Color.RED)
                style = (Paint.Style.STROKE)
                strokeWidth = 3F
            }

            val rectPainterTo =  Paint().apply {
                color = (Color.GREEN)
                style = (Paint.Style.STROKE)
                strokeWidth = 3F
            }
            when(guide) {
                is  SolverFailure ->{
                    it.drawText( guide.cause,0F,0F,textPainter)
                }
                is DetectionFailure -> {
                    it.drawText( guide.cause,0F,0F,textPainter)
                }

                is NextStep -> {
                    it.drawRect(guide.currentBlockPosition,rectPainterFrom)
                    it.drawRect(guide.toCurrentBlockPosition,rectPainterTo)
                }
            }

            }
        }

    fun hide() {
        overlayView.visibility = View.GONE
        //controlPanel.visibility = View.GONE
    }

    fun show() {
        overlayView.visibility = View.VISIBLE
        //controlPanel.visibility = View.VISIBLE
    }

    fun dropOverlays(){
        overlayView.clear()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        windowManager.removeView(controlPanel)
        windowManager.removeView(overlayView)
    }
}