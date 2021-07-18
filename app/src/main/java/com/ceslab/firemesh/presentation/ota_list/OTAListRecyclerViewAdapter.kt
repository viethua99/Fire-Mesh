package com.ceslab.firemesh.presentation.node_list

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ceslab.firemesh.R
import com.ceslab.firemesh.ota.model.BluetoothDeviceInfo
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import timber.log.Timber

class OTAListRecyclerViewAdapter(context: Context) :
    BaseRecyclerViewAdapter<BluetoothDeviceInfo, OTAListRecyclerViewAdapter.ViewHolder>(context) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.item_ota, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = dataList[position]
        holder.renderUI(device)
    }

    fun replaceDeviceByIndex(bluetoothDevice: BluetoothDeviceInfo) {
        Timber.d("replaceDeviceByIndex")
        val pastDeviceIndex = getDeviceIndexByAddress(bluetoothDevice.address)
        if (pastDeviceIndex >= 0) {
            val pastDevice = dataList[pastDeviceIndex]
            dataList[pastDeviceIndex] = bluetoothDevice
            if (pastDevice != bluetoothDevice) {
                notifyDataSetChanged()
            }
        } else {
            dataList.add(bluetoothDevice)
            notifyDataSetChanged()
        }
    }

    private fun getDeviceIndexByAddress(deviceAddress: String?): Int {
        return dataList.indexOfFirst { device -> device.address == deviceAddress }
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),View.OnClickListener {
        var tvDeviceName: TextView = view.findViewById(R.id.tv_device_name)
        var tvDeviceAddress: TextView = view.findViewById(R.id.tv_device_address)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            itemClickListener.onClick(adapterPosition, dataList[adapterPosition])
        }

        fun renderUI(bluetoothDevice: BluetoothDeviceInfo) {
            if(bluetoothDevice.name == null){
                tvDeviceName.text = "Unknown Device"
            } else {
                tvDeviceName.text = bluetoothDevice.name
            }

            tvDeviceAddress.text = bluetoothDevice.address
        }
    }
}