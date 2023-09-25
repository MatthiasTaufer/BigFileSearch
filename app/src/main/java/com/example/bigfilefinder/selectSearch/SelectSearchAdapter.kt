package com.example.bigfilefinder.selectSearch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.example.bigfilefinder.R

class SelectSearchAdapter(private val list:MutableList<DocumentFile>):
RecyclerView.Adapter<SelectSearchAdapter.SelectSearchViewHolder>(){
    class SelectSearchViewHolder(view: View):RecyclerView.ViewHolder(view) {

        val text:TextView = view.findViewById(R.id.directoryName)

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SelectSearchViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_directory,parent,false)
        return SelectSearchViewHolder(itemView)
    }

    override fun onBindViewHolder(
        holder: SelectSearchViewHolder,
        position: Int
    ) {
        val currentItem = list[position]
        holder.text.text = currentItem.name
    }

    override fun getItemCount(): Int {
        return list.size
    }
}