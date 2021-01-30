package com.ceslab.firemesh.presentation.scan

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter

class NetworkListRecyclerViewAdapter(context: Context) :
    BaseRecyclerViewAdapter<String, NetworkListRecyclerViewAdapter.ViewHolder>(context) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.item_network, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val connectableDeviceDescription = dataList[position]
        holder.renderUI(connectableDeviceDescription)
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        init {
            view.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            itemClickListener.onClick(adapterPosition, "test")
        }

        fun renderUI(connectableDeviceDescription: String) {

        }
    }
}