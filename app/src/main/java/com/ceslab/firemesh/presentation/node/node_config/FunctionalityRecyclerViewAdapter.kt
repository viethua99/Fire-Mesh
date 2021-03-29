package com.ceslab.firemesh.presentation.node.node_config

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ceslab.firemesh.R
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.ceslab.firemesh.presentation.base.BaseRecyclerViewAdapter
import timber.log.Timber

/**
 * Created by Viet Hua on 03/23/2021.
 */

class FunctionalityRecyclerViewAdapter(context: Context) :
    BaseRecyclerViewAdapter<NodeFunctionality.FunctionalityNamed, FunctionalityRecyclerViewAdapter.ViewHolder>(context) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.item_functionality, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val functionality = dataList[position]
        holder.renderUI(functionality)
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        var tvFunctionalityName: TextView = view.findViewById(R.id.tv_functionality_name)
        var tvFunctionalityBinded: TextView = view.findViewById(R.id.tv_functionality_binded)


        init {
            view.setOnClickListener(this)

        }

        override fun onClick(p0: View?) {
            itemClickListener.onClick(adapterPosition, dataList[adapterPosition])
        }


        fun renderUI(functionality: NodeFunctionality.FunctionalityNamed) {
            Timber.d("Adapter = $functionality")
            tvFunctionalityName.setText(functionality.functionalityName)
            if(functionality.functionality.isBinded){
                tvFunctionalityBinded.setText("Binded")
            } else {
                tvFunctionalityBinded.setText("Not Binded")

            }

        }
    }
}