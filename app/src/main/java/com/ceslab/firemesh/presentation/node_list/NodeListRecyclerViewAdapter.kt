package com.ceslab.firemesh.presentation.node_list

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.util.ConverterUtil
import timber.log.Timber

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
        var imgFireSignal : ImageView = view.findViewById(R.id.img_flame_signal)
        var imgNodeFeature : ImageView = view.findViewById(R.id.img_node_feature)

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

            if(meshNode.node.deviceCompositionData.supportsLowPower()){
                imgNodeFeature.setImageResource(R.drawable.img_lpn)
                if(meshNode.fireSignal == 1) {
                    imgFireSignal.setImageResource(R.drawable.img_flame)
                } else {
                    imgFireSignal.setImageResource(R.drawable.img_flame_background)
                }
            } else if(meshNode.node.deviceCompositionData.supportsFriend()){
                imgNodeFeature.setImageResource(R.drawable.img_friend)
            } else {
                imgNodeFeature.setImageResource(R.drawable.img_proxy)
            }

            if(node.isConnectedAsProxy){
                tvNodeProxy.visibility = View.VISIBLE
            } else {
                tvNodeProxy.visibility = View.GONE
            }
            tvNodeAddress.text = "0x" + Integer.toHexString(node.primaryElementAddress).toUpperCase()
            tvNodeStatus.text = "Death"
            tvNodeBattery.text = "999%"

        }
    }
}