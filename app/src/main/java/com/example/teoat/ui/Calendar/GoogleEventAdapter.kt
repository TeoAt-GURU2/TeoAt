package com.example.teoat.ui.Calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R
import com.google.api.services.calendar.model.Event

class GoogleEventAdapter(private val events: List<Event>) :
    RecyclerView.Adapter<GoogleEventAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.event_title) // item_google_event.xml의 ID
        val date: TextView = view.findViewById(R.id.event_date)   // item_google_event.xml의 ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.title.text = event.summary ?: "제목 없음"

        // 시작 시간 표시 (날짜만 추출)
        val start = event.start.dateTime ?: event.start.date
        holder.date.text = start.toString().substring(0, 10)
    }

    override fun getItemCount() = events.size
}