package com.ceslab.firemesh.presentation.node_list

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.util.ConverterUtil

class NodeListRecyclerViewAdapter(context: Context) :
    BaseRecyclerViewAdapter<MeshNode, NodeListRecyclerViewAdapter.ViewHolder>(context) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.item_node, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meshNode = dataList[position]
        holder.renderUI(meshNode)
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener,View.OnLongClickListener {
        var tvNodeName: TextView = view.findViewById(R.id.tv_node_name)
        var tvNodeAddress: TextView = view.findViewById(R.id.tv_node_address)
        var tvNodeStatus : TextView = view.findViewById(R.id.tv_node_status)
        var tvNodeBattery: TextView = view.findViewById(R.id.tv_node_battery)
        var tvNodeProxy : TextView = view.findViewById(R.id.tv_node_proxy)
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

        fun renderUI(meshNode: MeshNode) {
            val node = meshNode.node
            tvNodeName.text = node.name
            if(node.isConnectedAsProxy){
                tvNodeProxy.visibility = View.VISIBLE
            } else {
                tvNodeProxy.visibility = View.GONE
            }
            tvNodeAddress.text = node.primaryElementAddress?.toString()
            tvNodeStatus.text = "Disconnected"
            tvNodeBattery.text = "999%"

        }
    }
}