package com.ceslab.firemesh.presentation.group_list

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ceslab.firemesh.R
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import com.ceslab.firemesh.util.ConverterUtil
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group

class GroupListRecyclerViewAdapter(context: Context) :
    BaseRecyclerViewAdapter<Group, GroupListRecyclerViewAdapter.ViewHolder>(context) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.item_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = dataList[position]
        holder.renderUI(group)
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener,
        View.OnLongClickListener {
        var tvGroupName: TextView = view.findViewById(R.id.tv_group_name)
        var tvNodeCount: TextView = view.findViewById(R.id.tv_group_nodes_count)
        var tvGroupAddress: TextView = view.findViewById(R.id.tv_group_address)

        init {
            view.setOnLongClickListener(this)
            view.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            itemClickListener.onClick(adapterPosition, dataList[adapterPosition])

        }

        override fun onLongClick(p0: View?): Boolean {
            itemClickListener.onLongClick(adapterPosition, dataList[adapterPosition])
            return true
        }

        fun renderUI(group: Group) {
            tvGroupName.text = group.name
            tvNodeCount.text = String.format("%d Nodes", group.nodes.size)
            tvGroupAddress.text = "0x" + Integer.toHexString(group.address).toUpperCase()
        }
    }
}