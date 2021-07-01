package com.ceslab.firemesh.presentation.subnet_list

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet

class SubnetListRecyclerViewAdapter(context: Context) :
    BaseRecyclerViewAdapter<Subnet, SubnetListRecyclerViewAdapter.ViewHolder>(context) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.item_subnet, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val network = dataList[position]
        holder.renderUI(network)
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener,View.OnLongClickListener {
        var tvNetworkName: TextView = view.findViewById(R.id.tv_subnet_name)
        var tvNetworkNodeCount: TextView = view.findViewById(R.id.tv_subnet_nodes_count)
        var tvNetworkGroupsCount: TextView = view.findViewById(R.id.tv_subnet_groups_count)

        init {
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            itemClickListener.onClick(adapterPosition, dataList[adapterPosition])
        }

        override fun onLongClick(p0: View?): Boolean {
            itemClickListener.onLongClick(adapterPosition,dataList[adapterPosition])
            return true
        }

        fun renderUI(subnet: Subnet) {
            tvNetworkName.text = subnet.name
            tvNetworkNodeCount.text = String.format("%d Nodes", subnet.nodes.size)
            tvNetworkGroupsCount.text = String.format("%d Groups", subnet.groups.size)
        }
    }
}