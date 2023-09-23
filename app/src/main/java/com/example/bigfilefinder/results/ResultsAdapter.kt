package com.example.bigfilefinder.results

import android.animation.ObjectAnimator
import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.example.bigfilefinder.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultsAdapter(private val list: MutableList<DocumentFile>)
    : RecyclerView.Adapter<ResultsAdapter.ResultHolder>() {
    class ResultHolder(view: View):RecyclerView.ViewHolder(view) {
        val name:TextView = view.findViewById(R.id.fileName)
        val size:TextView = view.findViewById(R.id.sizeOfFile)
        val lastMod:TextView = view.findViewById(R.id.lastModified)
        val parent:TextView = view.findViewById(R.id.parentDir)
        val imageButton:ImageView = view.findViewById(R.id.ArrowImmage)

        //extra var for keeping track of toggling the extra information
        var toggled:Boolean = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
    : ResultHolder {
        val item = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_result,parent,false)
        return ResultHolder(item)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ResultHolder, position: Int) {
        val currentFile = list[position]
        holder.name.text = currentFile.name
        holder.size.text = "Size: ${formatFileSize(currentFile.length())}"
        holder.lastMod.text = "Last modiefied:\n${convertTimestampToDate(currentFile.lastModified())}"

        holder.parent.text = "Parent dir:\n${currentFile.parentFile?.name}"

        holder.itemView.setOnClickListener{
            holder.toggled = !holder.toggled
            if (holder.toggled){
                val rotationAnimator = ObjectAnimator.ofFloat(holder.imageButton, View.ROTATION, 0f, -90f)
                rotationAnimator.duration = 500
                rotationAnimator.start()
                holder.lastMod.visibility = View.VISIBLE
                holder.parent.visibility = View.VISIBLE
            }else{
                val rotationAnimator = ObjectAnimator.ofFloat(holder.imageButton, View.ROTATION, -90f, 0f)
                rotationAnimator.duration = 500
                rotationAnimator.start()
                holder.lastMod.visibility = View.GONE
                holder.parent.visibility = View.GONE
            }
        }
    }

    private fun convertTimestampToDate(timestamp: Long): String{
        val dateFormat = SimpleDateFormat("dd-MM-YYYY \n HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return dateFormat.format(date)
    }

    private fun formatFileSize(sizeInBytes:Long):String{
        val kiloByte = 1024.0
        val megaByte = kiloByte * kiloByte
        val gigaByte = megaByte * kiloByte
        val teravByte = gigaByte * kiloByte

        return when{
            sizeInBytes < kiloByte -> "$sizeInBytes B"
            sizeInBytes < megaByte -> String.format("%.2f KB", sizeInBytes/kiloByte)
            sizeInBytes < gigaByte -> String.format("%.2f MB", sizeInBytes/megaByte)
            sizeInBytes < teravByte -> String.format("%.2f GB", sizeInBytes/gigaByte)
            else -> String.format("%.2f TB", sizeInBytes/teravByte)
        }
    }

}