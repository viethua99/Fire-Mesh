package com.ceslab.firemesh.presentation.scan

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import timber.log.Timber

class ScanRecyclerViewAdapter(context: Context) :
    BaseRecyclerViewAdapter<String, ScanRecyclerViewAdapter.ViewHolder>(context) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.item_scanned_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val connectableDeviceDescription = dataList[position]
        holder.renderUI(connectableDeviceDescription)
    }

    fun replaceDeviceByIndex(connectableDeviceDescription: String) {
        Timber.d("replaceDeviceByIndex")
        val pastDeviceIndex = getDeviceIndexByAddress(connectableDeviceDescription)
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
        return -1
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        var tvDeviceName: TextView = view.findViewById(R.id.tv_device_name)
        var tvDeviceAddress: TextView = view.findViewById(R.id.tv_node_address)
        var tvRssi: TextView = view.findViewById(R.id.tv_device_rssi)
        var btnProvision: Button = view.findViewById(R.id.btn_provision)



        fun renderUI(connectableDeviceDescription: String) {
            btnProvision.setOnClickListener {
                itemClickListener.onClick(adapterPosition,"Test")
            }
        }
    }
}