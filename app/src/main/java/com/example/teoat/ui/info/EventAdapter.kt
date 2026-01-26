package com.example.teoat.ui.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R
import java.text.SimpleDateFormat
import java.util.Locale

class EventAdapter(
    private var events: List<Event>,
    private val onScrapClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_event_title)
        val host: TextView = view.findViewById(R.id.tv_event_host)
        val date : TextView = view.findViewById(R.id.tv_event_date)
        val imgScrap : ImageView = view.findViewById(R.id.iv_event_scrap)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]

        holder.title.text = event.title
        holder.host.text = event.host

        // 👇 [수정됨] endDate 대신 startDate(시작일)를 보여주도록 변경
        if (event.startDate != null) {
            val sdf = SimpleDateFormat("MM.dd", Locale.KOREA)
            // Timestamp 타입이므로 .toDate()를 붙여서 Date 객체로 변환해야 합니다.
            holder.date.text = sdf.format(event.startDate.toDate())
        } else {
            holder.date.text = "-"
        }

        // 스크랩 상태 아이콘 설정
        val iconRes = if (event.isScrapped) R.drawable.baseline_bookmark_24 else R.drawable.outline_bookmark_24
        holder.imgScrap.setImageResource(iconRes)

        holder.imgScrap.setOnClickListener {
            onScrapClick(event)
        }
    }

    override fun getItemCount() = events.size

    // (선택 사항) 데이터 갱신용 함수가 필요하면 사용
    // fun updateData(newEvents: List<Event>) {
    //     this.events = newEvents
    //     notifyDataSetChanged()
    // }
}