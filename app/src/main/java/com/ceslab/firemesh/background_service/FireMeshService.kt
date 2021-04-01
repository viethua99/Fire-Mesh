package com.ceslab.firemesh.background_service

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.ceslab.firemesh.R
import com.ceslab.firemesh.ota.utils.Converters
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import timber.log.Timber
import java.util.*


/**
 * Created by Viet Hua on 04/01/2021.
 */


//****For some Chinese devices brand like : OPPO , XIAOMI ,... you need to lock the app in order to keep the service running on background or when screen turned off ****//
class FireMeshService : Service() {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "ceslab.firemesh"
    }

    private var counter = 0
    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            return bluetoothAdapter.bluetoothLeScanner
        }

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.d("Larger")
            startMyOwnForeGround()
        } else {
            Timber.d("Smaller")
//            val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
//            val notification = notificationBuilder.setOngoing(true)
//                .setSmallIcon(R.drawable.img_app)
//                .setContentTitle("Fire Mesh")
//                .setContentText("App is running in background")
//                .setPriority(NotificationManager.IMPORTANCE_MIN)
//                .setCategory(Notification.CATEGORY_SERVICE)
//                .build()
            startForeground(1, Notification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        //    startScanBle()

        startTimer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        // stopScanBle()
        stopTimerTask()
        val broadcastIntent = Intent()
        broadcastIntent.action = "restartService"
        broadcastIntent.setClass(this, ScanRestartReceiver::class.java)
        this.sendBroadcast(broadcastIntent)

    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeGround() {
        val channelName = "FireMesh Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.img_app)
            .setContentTitle("Fire Mesh")
            .setContentText("App is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    private var timer: Timer? = null
    private lateinit var timerTask: TimerTask

    fun startScanBle() {
        Timber.d("startScanBle")
        val filterBuilder = ScanFilter.Builder()
        val filter = filterBuilder.build()
        val settingBuilder = ScanSettings.Builder()
        settingBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        val setting = settingBuilder.build()
        bluetoothLeScanner.startScan(listOf(filter), setting, scanCallback)
    }

    fun stopScanBle() {
        Timber.d("stopScanBle")
        bluetoothLeScanner.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val rawData = result?.scanRecord?.bytes
            if (rawData != null) {
                Timber.d("Raw: " + Converters.bytesToHexWhitespaceDelimited(rawData))
            }

//            val dataList = result?.scanRecord?.getManufacturerSpecificData(NodeListViewModel.COMPANY_ID)
//            if (dataList != null) {
//                Timber.d("onScanResult: ${Converters.bytesToHexReversed(dataList)}")
//                checkFireAlarmSignalFromUnicastAddress(dataList)
//            }
        }
    }


    //************TIMER TASK JUST FOR TEST BACKGROUND SERVICE********//
    fun startTimer() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                Timber.i("Count: --- ${counter++}")
//                if (counter % 10 == 0) {
//                    val vibrator: Vibrator =
//                        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//                    if (vibrator.hasVibrator()) {
//                        vibrator.vibrate(1000)
//                    }
//
//                    val notificationBuilder =
//                        NotificationCompat.Builder(this@FireMeshService, NOTIFICATION_CHANNEL_ID)
//                    val notification = notificationBuilder.setOngoing(true)
//                        .setSmallIcon(R.drawable.img_app)
//                        .setContentTitle("Fire Mesh (EMERGENCY)")
//                        .setContentText("We detected fire signal from your mesh, please check immediately!!!")
//                        .setPriority(NotificationManager.IMPORTANCE_HIGH)
//                        .setCategory(Notification.CATEGORY_SERVICE)
//                        .setContentIntent(
//                            PendingIntent.getActivity(
//                                this@FireMeshService,
//                                0,
//                                Intent(this@FireMeshService, MainActivity::class.java),
//                                0
//                            )
//                        )
//                        .setAutoCancel(true)
//                        .build()
//                    val notificationManager = getSystemService(NotificationManager::class.java)
//                    notificationManager.notify(3, notification)
//                }




            }
        }
        timer!!.schedule(timerTask, 1000, 1000)
    }

    fun stopTimerTask() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }
    //*****************************************************************//


}