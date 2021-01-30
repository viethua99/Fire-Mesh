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

class NetworkRecyclerViewAdapter(context: Context) :
    BaseRecyclerViewAdapter<String, NetworkRecyclerViewAdapter.ViewHolder>(context) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.item_network, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val connectableDeviceDescription = dataList[position]
        holder.renderUI(connectableDeviceDescription)
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        fun renderUI(connectableDeviceDescription: String) {

        }
    }
}