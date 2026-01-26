package com.example.teoat.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class GoogleEventAdapter2(
    private val items: List<CalendarEventItem>
) : RecyclerView.Adapter<GoogleEventAdapter2.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        // ✅ 수정됨: item_event.xml에 있는 실제 ID로 변경
        val title: TextView = v.findViewById(R.id.tv_event_title)
        val time: TextView = v.findViewById(R.id.tv_event_date)

        // item_event.xml에는 location 전용 뷰가 없으므로,
        // 일단 'host'(주최자) 자리에 location을 표시하도록 연결합니다.
        val location: TextView = v.findViewById(R.id.tv_event_host)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // 여기서 item_event 레이아웃을 사용하고 있음
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
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
        val s = e.start ?: return ""
        val dt = s.dateTime
        val d = s.date

        // 종일 일정
        if (dt == null && d != null) return "$d (종일)"

        // 시간 일정 (RFC3339)
        if (dt != null) {
            val millis = parseRfc3339ToMillis(dt) ?: return dt
            val out = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
            out.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            return out.format(millis)
        }

        return ""
    }

    private fun parseRfc3339ToMillis(value: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )

        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val t = sdf.parse(value)?.time
                if (t != null) return t
            } catch (_: ParseException) {
            }
        }
        return null
    }
}