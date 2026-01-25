package com.example.teoat.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TodayEventAdapter(
    private val items: List<TodayEvent>
) : RecyclerView.Adapter<TodayEventAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val t1: TextView = itemView.findViewById(android.R.id.text1)
        private val t2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(item: TodayEvent) {
            t1.text = item.title
            t2.text = "${item.location}  ${item.date}  ${item.status}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
