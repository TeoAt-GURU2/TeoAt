package com.example.teoat.ui.Calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R

// 데이터 모델 클래스 (필드명이 Firestore와 같아야 함)
data class TodayEvent(val title: String = "")

class TodayEventAdapter(private val events: List<TodayEvent>) :
    RecyclerView.Adapter<TodayEventAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txt_banner_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.today_banner, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = events[position].title
    }

    override fun getItemCount() = events.size
}