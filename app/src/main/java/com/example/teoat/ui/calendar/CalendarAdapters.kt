package com.example.teoat.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// 1. Google Event Adapter
class GoogleEventAdapter(
    private val items: List<CalendarEventItem>
) : RecyclerView.Adapter<GoogleEventAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tv_event_title)
        val time: TextView = v.findViewById(R.id.tv_event_date)
        val location: TextView = v.findViewById(R.id.tv_event_host)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_event, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.title.text = e.summary ?: "(제목 없음)"
        holder.time.text = formatTime(e)
        holder.location.text = e.location ?: ""
    }

    private fun formatTime(e: CalendarEventItem): String {
        val start = e.start ?: return ""
        // 종일 일정
        if (start.dateTime == null && start.date != null) return "${start.date} (종일)"

        // 시간 일정
        val millis = CalendarUtils.parseGoogleStartToMillis(start.dateTime, start.date) ?: return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return sdf.format(millis)
    }
}

// 2. Today Event Adapter (Firestore용)
class TodayEventAdapter(
    private val items: List<TodayEvent>
) : RecyclerView.Adapter<TodayEventAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // simple_list_item_2는 text1, text2 ID를 가짐
        private val t1: TextView = itemView.findViewById(android.R.id.text1)
        private val t2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(item: TodayEvent) {
            t1.text = item.title
            t2.text = "${item.location} | ${item.status}"
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