package com.ceslab.firemesh.background_service

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNodeManager
import com.ceslab.firemesh.myapp.COMPANY_ID
import com.ceslab.firemesh.ota.utils.Converters
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import dagger.android.AndroidInjection
import timber.log.Timber
import java.util.*
import javax.inject.Inject


/**
 * Created by Viet Hua on 04/01/2021.
 */


//****For some Chinese device brands like : OPPO , XIAOMI ,... you need to lock the app in order to keep the service running on background or when screen turned off ****//
class FireMeshService : Service() {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ceslab.firemesh"
        const val FIRE_MESH_SERVICE_KEY = "FIRE_MESH_SERVICE_KEY"
    }

    @Inject
    lateinit var meshNetworkManager: MeshNetworkManager

    @Inject
    lateinit var meshNodeManager: MeshNodeManager

    private var counter = 0

    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            return bluetoothAdapter.bluetoothLeScanner
        }

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")
        AndroidInjection.inject(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.d("Larger")
            startMyOwnForeGround()
        } else {
            Timber.d("Smaller")
            startForeground(1, Notification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        startScanBle()
        // startTimer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        stopScanBle()
        //stopTimerTask()
//        val broadcastIntent = Intent()
//        broadcastIntent.action = "restartService"
//        broadcastIntent.setClass(this, ScanRestartReceiver::class.java)
//        this.sendBroadcast(broadcastIntent)

    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
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

    private fun checkFireAlarmSignalFromUnicastAddress(unicastAddress: ByteArray) {
        Timber.d("unicastAddress size = ${unicastAddress.size}")
        val hexUnicastAddress = Converters.bytesToHexReversed(unicastAddress)
        Timber.d("checkFireAlarmSignalFromUnicastAddress: $hexUnicastAddress")

        val network = meshNetworkManager.network
        for (subnet in network!!.subnets) {
            val nodeList = meshNodeManager.getMeshNodeList(subnet)
            for (node in nodeList) {
                if (Integer.toHexString(node.node.primaryElementAddress!!) == hexUnicastAddress) {
                    node.fireSignal = 1
                    vibratePhone(1000)
                    showEmergencyNotification(subnet)
                }
            }
        }
    }

    private fun vibratePhone(millisecond: Long) {
        Timber.d("vibratePhone: $millisecond")
        val vibrator: Vibrator =
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(millisecond)
        }
    }

    private fun showEmergencyNotification(subnet: Subnet) {
        Timber.d("showEmergencyNotification: ${Converters.bytesToHex(subnet.netKey.key)}")
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(FIRE_MESH_SERVICE_KEY, subnet.netKey.key)


        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.img_app)
            .setDefaults(Notification.DEFAULT_SOUND)
            .setContentTitle("Fire Mesh (EMERGENCY)")
            .setContentText("We detected fire signal from ${subnet.name}, please check immediately!!!")
            .setPriority(Notification.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setAutoCancel(true)
            .build()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(3, notification)
    }


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
//            val rawData = result?.scanRecord?.bytes
//            if (rawData != null) {
//                Timber.d("Raw: " + Converters.bytesToHexWhitespaceDelimited(rawData))
//            }

            val dataList = result?.scanRecord?.getManufacturerSpecificData(COMPANY_ID)
            if (dataList != null) {
                Timber.d("onScanResult: ${Converters.bytesToHexReversed(dataList)}")
                checkFireAlarmSignalFromUnicastAddress(dataList)
            }
        }
    }


    //************TIMER TASK JUST FOR TEST BACKGROUND SERVICE********//
    fun startTimer() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                Timber.i("Count: --- ${counter++}")
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