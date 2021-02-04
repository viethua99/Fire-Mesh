package com.ceslab.firemesh.presentation.scan

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.util.ConverterUtil
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet

class NetworkListRecyclerViewAdapter(context: Context) :
    BaseRecyclerViewAdapter<Subnet, NetworkListRecyclerViewAdapter.ViewHolder>(context) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.item_network, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val network = dataList[position]
        holder.renderUI(network)
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        var tvNetworkName: TextView = view.findViewById(R.id.tv_network_name)
        var tvNetworkNodeCount: TextView = view.findViewById(R.id.tv_network_nodes_count)
        var tvNetworkGroupsCount: TextView = view.findViewById(R.id.tv_network_groups_count)
        var tvNetworkKeyIndex: TextView = view.findViewById(R.id.tv_network_key_index)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            itemClickListener.onClick(adapterPosition, dataList[adapterPosition])
        }

        fun renderUI(subnet: Subnet) {
            tvNetworkName.text = subnet.name
            tvNetworkNodeCount.text = String.format("%d Nodes", subnet.nodes.size)
            tvNetworkGroupsCount.text = String.format("%d Groups", subnet.groups.size)
            tvNetworkKeyIndex.text = String.format("... %s", ConverterUtil.getHexValue(subnet.netKey.key).takeLast(12))

        }
    }
}