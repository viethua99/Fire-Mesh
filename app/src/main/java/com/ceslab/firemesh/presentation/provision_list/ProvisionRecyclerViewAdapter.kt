package com.ceslab.firemesh.presentation.provision_list

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.ConnectableDeviceDescription
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import timber.log.Timber

class ProvisionRecyclerViewAdapter(context: Context) :
    BaseRecyclerViewAdapter<ConnectableDeviceDescription, ProvisionRecyclerViewAdapter.ViewHolder>(
        context
    ) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.item_unprovisioned_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val connectableDeviceDescription = dataList[position]
        holder.renderUI(connectableDeviceDescription)
    }

    fun replaceDeviceByIndex(connectableDeviceDescription: ConnectableDeviceDescription) {
        Timber.d("replaceDeviceByIndex")
        val pastDeviceIndex = getDeviceIndexByAddress(connectableDeviceDescription.deviceAddress)
        if (pastDeviceIndex >= 0) {
            val pastDevice = dataList[pastDeviceIndex]
            dataList[pastDeviceIndex] = connectableDeviceDescription
            if (pastDevice != connectableDeviceDescription) {
                notifyDataSetChanged()
            }
        } else {
            dataList.add(connectableDeviceDescription)
            notifyDataSetChanged()
        }
    }

    private fun getDeviceIndexByAddress(deviceAddress: String?): Int {
        return dataList.indexOfFirst { deviceInfo -> deviceInfo.deviceAddress.equals(deviceAddress) }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        var tvDeviceName: TextView = view.findViewById(R.id.tv_device_name)
        var tvDeviceAddress: TextView = view.findViewById(R.id.tv_device_address)
        var tvRssi: TextView = view.findViewById(R.id.tv_device_rssi)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            itemClickListener.onClick(adapterPosition, dataList[adapterPosition])
        }

        fun renderUI(connectableDeviceDescription: ConnectableDeviceDescription) {
            if (connectableDeviceDescription.deviceName == null) {
                tvDeviceName.text = "Unknown Device"
            } else {
                tvDeviceName.text = connectableDeviceDescription.deviceName
            }

            tvDeviceAddress.text = connectableDeviceDescription.deviceAddress
            tvRssi.text = connectableDeviceDescription.rssi.toString() + " dBm"
        }
    }
}