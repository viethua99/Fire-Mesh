package com.ceslab.firemesh.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNetworkManager
import com.ceslab.firemesh.meshmodule.bluetoothmesh.MeshNodeManager
import com.ceslab.firemesh.myapp.AES_KEY
import com.ceslab.firemesh.ota.utils.Converters
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import com.siliconlabs.bluetoothmesh.App.AESUtils
import dagger.android.AndroidInjection
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject


/**
 * Created by Viet Hua on 04/01/2021.
 */


//****For some Chinese device brands like : OPPO , XIAOMI ,... you need to lock the app in order to keep the service running on background or when screen turned off ****//
class FireMeshService : Service() {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ceslab.firemesh"
        private const val EMERGENCY_CHANNEL_ID = "ceslab.firemesh.emergency"

        const val FIRE_MESH_SERVICE_KEY = "FIRE_MESH_SERVICE_KEY"
    }

    @Inject
    lateinit var meshNetworkManager: MeshNetworkManager

    @Inject
    lateinit var meshNodeManager: MeshNodeManager


    private val fireMeshScanner = FireMeshScanner.instance


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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fireMeshScanner.addFireMeshScannerCallback(fireMeshScanResult)
            fireMeshScanner.startScanBle()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        fireMeshScanner.removeFireMeshScannerCallback(fireMeshScanResult)
        fireMeshScanner.stopScanBle()

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


    private fun checkFireAlarmSignalFromUnicastAddress(dataFlag: Byte, userData: ByteArray) {
        val network = meshNetworkManager.network
        val rightFlag = (dataFlag.toInt() and 0x0F)
        if (rightFlag == 0) { //Fire alarm signal
            val receivedUnicastAddress = Converters.bytesToHex(byteArrayOf(userData[1], userData[0]))
            for (subnet in network!!.subnets) {
                val nodeList = meshNodeManager.getMeshNodeList(subnet)
                for (node in nodeList) {
                    val unicastAddress = "%4x".format(node.node.primaryElementAddress!!)
                    if (unicastAddress == receivedUnicastAddress) {
                        node.fireSignal = 1
                        triggerEmergencyAlarm(subnet)
                    }
                }
            }
        }

    }

    private fun getDataFlag(rawData: ByteArray): Byte {
        return rawData[0]
    }

    private fun getUserData(rawData: ByteArray): ByteArray {
        var userData = byteArrayOf()
        for (i in 1 until rawData.size) {
            userData += rawData[i]
        }
        return userData
    }

    private fun vibratePhone(millisecond: Long) {
        Timber.d("vibratePhone: $millisecond")
        val vibrator: Vibrator =
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(millisecond)
        }
    }

    private fun triggerAlarmSound() {
        Timber.d("triggerAlarmSound")
        val alarmSoundUri: Uri =
            Uri.parse("android.resource://" + applicationContext.packageName.toString() + "/" + R.raw.sound_alarm)

        val ringTone: Ringtone = RingtoneManager.getRingtone(applicationContext, alarmSoundUri)
        ringTone.play()
    }

    private fun triggerEmergencyAlarm(subnet: Subnet) {
        Timber.d("triggerEmergencyAlarm: ${Converters.bytesToHex(subnet.netKey.key)}")
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(FIRE_MESH_SERVICE_KEY, subnet.netKey.key)

        vibratePhone(1000)
        triggerAlarmSound()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showNotificationInOreoDevice(intent, subnet)
        } else {
            Timber.d("Smaller")
            showNotificationInLegacyDevice(intent, subnet)
        }
    }

    private fun showNotificationInLegacyDevice(intent: Intent?, subnet: Subnet) {
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNotificationInOreoDevice(intent: Intent?, subnet: Subnet) {

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelName = "Emergency Channel"

        val existingChannel = notificationManager.getNotificationChannel(EMERGENCY_CHANNEL_ID)
        existingChannel?.let {
            notificationManager.deleteNotificationChannel(EMERGENCY_CHANNEL_ID)
        }

        val channel = NotificationChannel(
            EMERGENCY_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )

        notificationManager.createNotificationChannel(channel)
        val notificationBuilder =
            NotificationCompat.Builder(this@FireMeshService, EMERGENCY_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.img_app)
            .setContentTitle("Fire Mesh (EMERGENCY)")
            .setContentText("We detected fire signal from ${subnet.name}, please check immediately!!!")
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
        notificationManager.notify(10, notification)
    }

    private val fireMeshScanResult = object : FireMeshScanner.FireMeshScannerCallback {
        override fun onScanResult(rawData: ByteArray?) {
            rawData?.let {
                Timber.d("onScanResult: ${Converters.bytesToHexWhitespaceDelimited(it)}")
                try {
                    val dataFlag = getDataFlag(it)
                    val encryptedUserData = getUserData(it)

                    val decryptedData = AESUtils.decrypt(
                        AESUtils.ECB_ZERO_BYTE_NO_PADDING_ALGORITHM,
                        AES_KEY,
                        encryptedUserData
                    )
                    Timber.d("decryptedData=  ${Converters.bytesToHexWhitespaceDelimited(decryptedData)} --size={${decryptedData.size}}")

                    checkFireAlarmSignalFromUnicastAddress(dataFlag, decryptedData)
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }

            }

        }
    }
}