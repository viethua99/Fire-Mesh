package com.ceslab.firemesh.presentation.node_list

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.ConnectableDeviceDescription
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.ota.OTADevice
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.util.ConverterUtil
import timber.log.Timber

class OTAListRecyclerViewAdapter(context: Context) :
    BaseRecyclerViewAdapter<OTADevice, OTAListRecyclerViewAdapter.ViewHolder>(context) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.item_ota, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meshNode = dataList[position]
        holder.renderUI(meshNode)
    }

    fun replaceDeviceByIndex(otaDevice: OTADevice) {
        Timber.d("replaceDeviceByIndex")
        val pastDeviceIndex = getDeviceIndexByAddress(otaDevice.deviceAddress)
        if (pastDeviceIndex >= 0) {
            val pastDevice = dataList[pastDeviceIndex]
            dataList[pastDeviceIndex] = otaDevice
            if (pastDevice != otaDevice) {
                notifyDataSetChanged()
            }
        } else {
            dataList.add(otaDevice)
            notifyDataSetChanged()
        }
    }

    private fun getDeviceIndexByAddress(deviceAddress: String?): Int {
        return dataList.indexOfFirst { deviceInfo -> deviceInfo.deviceAddress.equals(deviceAddress) }
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvDeviceName: TextView = view.findViewById(R.id.tv_device_name)
        var tvDeviceAddress: TextView = view.findViewById(R.id.tv_node_address)
        var btnOta: Button = view.findViewById(R.id.btn_ota)

        fun renderUI(otaDevice: OTADevice) {
            if(otaDevice.deviceName == null){
                tvDeviceName.text = "Unknown Device"
            } else {
                tvDeviceName.text = otaDevice.deviceName
            }

            tvDeviceAddress.text = otaDevice.deviceAddress
            btnOta.setOnClickListener {
                itemClickListener.onClick(adapterPosition, dataList[adapterPosition])
            }
        }
    }
}