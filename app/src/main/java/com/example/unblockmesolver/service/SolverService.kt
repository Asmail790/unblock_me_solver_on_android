package com.example.unblockmesolver.service

import android.app.Activity
import android.app.Notification
import  com.example.unblockmesolver.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.unblockmesolver.ml.ObjectDector
import com.example.unblockmesolver.service.Solvers.CPPSolver
import com.example.unblockmesolver.service.Solvers.PythonSolver
import com.example.unblockmesolver.service.Solvers.Solver
import com.example.unblockmesolver.service.nextstepInformation.NextStep
import com.example.unblockmesolver.service.UI.UI
import kotlin.math.roundToInt
import kotlin.streams.toList


class SolverService : LifecycleService() {

    companion object {
        const val RESULT_CODE = "RESULT_CODE"
        const val DATA = "DATA"
        const val CHANNEL_ID = "UnBlockMe Solver Service"
        const val notificationId = 1
        const val  TAG = "SERVICE"
        fun startServiceIntent(context: Context,data:Intent, resultCode:Int):Intent  = Intent(context,SolverService::class.java).apply {
                putExtra(RESULT_CODE, resultCode);
                putExtra(DATA, data);
        }
    }

    private  var   _screenshoter: Screenshoter? = null
    private lateinit var _ui: UI
    private lateinit var _dector:ObjectDector

    private val screenshoter:Screenshoter
        get() = this._screenshoter!!

    private val ui:UI
        get() = this._ui

    private val dector:ObjectDector
        get() = this._dector

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate() {
        super.onCreate()
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                channelName = R.string.channelName,
                channelDescription =  R.string.channelDescription
            )
        }

        val notification = createNotification(
            title = R.string.notificationTitle,
            text =  R.string.notificationText,
            icon = R.drawable.service_notification_icon
        )

        startForeground(notificationId,notification)
        val classIds = assets.open("classes.txt")
           .bufferedReader(Charsets.UTF_8).lines()
          .toList()


        PythonSolver.setClassIdMapping(classIds)
        CPPSolver.setClassIdMapping(classIds)

        _dector = ObjectDector(this)
        _ui = UI(this, onGuide =  {
            ui.hide()
            ui.overlayView.postDelayed( {
                val screenshot =  screenshoter.requestScreenshot()
                val results = dector.infer(screenshot)
                val solver:Solver = when(resources.getString(R.string.chosen)) {
                    resources.getString(R.string.py_solver) -> PythonSolver
                    resources.getString(R.string.cpp_solver) -> CPPSolver
                    else -> throw  Exception("solver not found")
                }

                if (ui.isRooted()) {
                    val command = solver.solve(results);
                    val p = Runtime.getRuntime().exec(command)
                    p.waitFor()
                } else {
                    val nextStep = solver.guide(results);
                    ui.show()
                    ui.draw(nextStep)
                }
            },100
            )

        }, onExit = {
            stopSelf()
        })
        lifecycle.addObserver(ui)
    }
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
      return null;
    }

    fun getCenters(nextStep: NextStep):Pair<Point,Point> {
        val oldCenter = Point(nextStep.currentBlockPosition.centerX().roundToInt(),nextStep.currentBlockPosition.centerY().roundToInt())
        val newCenter =  Point(nextStep.newBlockPosition.centerX().roundToInt(),nextStep.newBlockPosition.centerY().roundToInt())
        return Pair(oldCenter,newCenter)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (_screenshoter != null) {
            return START_NOT_STICKY
        }
        val result =readIntent(intent!!)
        _screenshoter = Screenshoter(this,result)
        lifecycle.addObserver(screenshoter)

        return START_NOT_STICKY;
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        channelId:String = CHANNEL_ID,
        @StringRes channelName:Int,
        @StringRes channelDescription:Int,
        importanceLevel: Int = NotificationManager.IMPORTANCE_DEFAULT
    ) {
        getString(channelName)
        val mChannel = NotificationChannel(
            channelId,
            getString(channelName),
            importanceLevel
        ).apply {
            description = getString(channelDescription)
        }


        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun NotificationAPI26OrAbove() = NotificationCompat.Builder(this,  CHANNEL_ID)

    private fun NotificationAPI26OrBelow() = NotificationCompat.Builder(this)

    private  fun createNotification(
        @StringRes title:Int,
        @StringRes text:Int,
        @DrawableRes icon:Int
    ):Notification {

        val builder =  when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> NotificationAPI26OrAbove()
            else -> NotificationAPI26OrBelow()
        }

        val notification = builder
            .setContentTitle(getString(title))
            .setContentText(getString(text))
            .setSmallIcon(icon)
            .build()

        return notification
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun readIntentAPIA33OrAbove(intent: Intent):Pair<Intent,Int> {
        val resultCode = intent.getParcelableExtra(RESULT_CODE,Int::class.java)!!
        val data = intent.getParcelableExtra(DATA,Intent::class.java)!!

        return Pair(data,resultCode)
    }

    private fun readIntentAPI32OrLower(intent: Intent):Pair<Intent,Int>  {
        val resultCode = intent.getIntExtra(RESULT_CODE,Activity.RESULT_CANCELED)
        val data = intent.getParcelableExtra<Intent>(DATA)!!

        if (resultCode ==Activity.RESULT_CANCELED ){
            throw IllegalArgumentException("resultCode is ${Activity.RESULT_CANCELED}.")
        }
        return Pair(data,resultCode)
    }

    private fun readIntent(intent: Intent):Pair<Intent,Int> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU  -> readIntentAPIA33OrAbove(intent)
            else -> readIntentAPI32OrLower(intent)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
    }
}