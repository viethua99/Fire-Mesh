package com.ceslab.firemesh.presentation.node_list

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener,
        View.OnLongClickListener {
        var tvNodeName: TextView = view.findViewById(R.id.tv_node_name)
        var tvNodeAddress: TextView = view.findViewById(R.id.tv_node_address)
        var tvNodeStatus: TextView = view.findViewById(R.id.tv_node_status)
        var tvNodeBattery: TextView = view.findViewById(R.id.tv_node_battery)
        var tvNodeProxy: TextView = view.findViewById(R.id.tv_node_proxy)
        var imgFireSignal: ImageView = view.findViewById(R.id.img_flame_signal)
        var imgNodeFeature: ImageView = view.findViewById(R.id.img_node_feature)

        init {
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)

        }

        override fun onClick(p0: View?) {
            itemClickListener.onClick(adapterPosition, dataList[adapterPosition])
        }

        override fun onLongClick(p0: View?): Boolean {
            itemClickListener.onLongClick(adapterPosition, dataList[adapterPosition])
            return true
        }

        fun renderUI(meshNode: MeshNode?) {
            meshNode?.let {
                Timber.d("$meshNode -- ${meshNode.batteryPercent} -- ${meshNode.heartBeat} -- ${meshNode.gatewayType}")
                Timber.d("friendsupport=${meshNode.node.deviceCompositionData.supportsFriend()}")
                Timber.d("lpn=${meshNode.node.deviceCompositionData.supportsLowPower()}")
                //Initialize
                val node = it.node
                val shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake_animation)
                imgNodeFeature.visibility = View.VISIBLE
                imgFireSignal.visibility = View.VISIBLE
                tvNodeName.text = node.name
                tvNodeAddress.text =
                    "0x" + Integer.toHexString(node.primaryElementAddress).toUpperCase()

                //Main proxy render
                if (node.isConnectedAsProxy) {
                    tvNodeProxy.visibility = View.VISIBLE
                } else {
                    tvNodeProxy.visibility = View.GONE
                }

                //Heartbeat render
                if (it.heartBeat == 1) {
                    tvNodeStatus.text = "Alive"
                    tvNodeStatus.setTextColor(Color.parseColor("#4CAF50"))

                } else {
                    tvNodeStatus.text = "Death"
                    tvNodeStatus.setTextColor(Color.parseColor("#F44336"))

                }

                //Node feature render
                if (it.gatewayType == MeshNode.GatewayType.MAIN_GATEWAY) {
                    imgFireSignal.visibility = View.GONE
                    imgNodeFeature.setImageResource(R.drawable.img_proxy)
                    tvNodeBattery.text = "Plugging"
                } else if (it.gatewayType == MeshNode.GatewayType.BACKUP_GATEWAY) {
                    imgFireSignal.visibility = View.GONE
                    imgNodeFeature.setImageResource(R.drawable.img_proxy)
                    tvNodeBattery.text = "Plugging"
                    val nodeName = node.name + " (BU)"
                    tvNodeName.text = nodeName

                } else if (it.gatewayType == MeshNode.GatewayType.NOT_GATEWAY) {

                    if (it.node.deviceCompositionData.supportsLowPower()) {  //lPN node feature render
                        if (it.batteryPercent == 0xFF) { //dead node
                            tvNodeBattery.text = "???"
                        } else {
                            if(it.batteryPercent >= 100){
                                tvNodeBattery.text = "100%"
                            } else {
                                tvNodeBattery.text = "${it.batteryPercent}%"
                            }
                        }
                        imgNodeFeature.setImageResource(R.drawable.img_lpn)
                        imgFireSignal.visibility = View.VISIBLE
                        if (it.fireSignal == 1) {
                            imgFireSignal.setImageResource(R.drawable.img_flame)
                            imgFireSignal.startAnimation(shakeAnimation)
                        } else {
                            imgFireSignal.setImageResource(R.drawable.img_flame_background)
                            imgFireSignal.clearAnimation()

                        }
                    } else if (it.node.deviceCompositionData.supportsFriend()) {  //Friend node feature render
                        tvNodeBattery.text = "Plugging"
                        imgNodeFeature.setImageResource(R.drawable.img_friend)
                        imgFireSignal.visibility = View.VISIBLE
                        if (it.fireSignal == 1) {
                            imgFireSignal.setImageResource(R.drawable.img_flame)
                            imgFireSignal.startAnimation(shakeAnimation)
                        } else {
                            imgFireSignal.setImageResource(R.drawable.img_flame_background)
                            imgFireSignal.clearAnimation()
                        }
                    } else {
                        tvNodeStatus.text = "???"
                        tvNodeBattery.text = "???"
                        imgNodeFeature.visibility = View.GONE
                        imgFireSignal.visibility = View.GONE
                    }
                }
            }

        }
    }
}