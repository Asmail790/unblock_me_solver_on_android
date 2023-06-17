package com.example.unblockmesolver

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.example.unblockmesolver.databinding.ActivityMainBinding
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.unblockmesolver.service.SolverService
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    companion object{
        const val TAG = "MainActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        

        val startServiceRequest = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_CANCELED) {
                Snackbar.make(this,view,getString(R.string.media_projection_permission_denied_message),Snackbar.LENGTH_LONG).show()
                    return@registerForActivityResult
            }

            val startUnblockMeIntent =  packageManager.getLaunchIntentForPackage(getString(R.string.Unblock_Me_package_name))


            when (startUnblockMeIntent) {
                null -> {
                    val installUnblockMeFromPlayStore =  Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("market://details?id=${getString(R.string.Unblock_Me_package_name)}")
                    }
                    Snackbar.make(this,view,getString(R.string.Unblock_Me_package_not_found),Snackbar.LENGTH_LONG)
                        .setAction("Install") { startActivity(installUnblockMeFromPlayStore) }.show()
                }
                else ->  startActivity(startUnblockMeIntent)
            }

            val startServiceIntent = SolverService.startServiceIntent(this, it.data!!, it.resultCode)
            startForegroundServiceCompt(startServiceIntent)
        }

        binding.startServiceButton.setOnClickListener{
            val mMediaProjectionManager = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
            startServiceRequest.launch(mMediaProjectionManager.createScreenCaptureIntent())
       }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startForegroundServiceAPI26OrAbove(intent:Intent) {
        startForegroundService(intent)
    }
    fun startForegroundServiceAPI25OrLower(intent:Intent) {
        startService(intent)
    }
    fun startForegroundServiceCompt(intent:Intent) =  when {
        Build.VERSION.SDK_INT >=Build.VERSION_CODES.O -> startForegroundServiceAPI26OrAbove(intent)
        else -> startForegroundServiceAPI25OrLower(intent)
    }

}
