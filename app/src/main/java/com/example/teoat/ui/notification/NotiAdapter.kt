package com.example.teoat.ui.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.data.model.NotificationModel
import com.example.teoat.databinding.ItemNotiBinding
import java.text.SimpleDateFormat
import java.util.Locale

class NotiAdapter (
    private val items : List<NotificationModel>
) : RecyclerView.Adapter<NotiAdapter.NotiViewHolder>() {

    inner class NotiViewHolder(private val binding: ItemNotiBinding) : RecyclerView.ViewHolder(binding.root)  {
        fun bind(item : NotificationModel) {
            binding.tvTitle.text = item.title
            binding.tvMessage.text = item.message

            // 날짜 포맷팅
            val sdf = SimpleDateFormat("MM.dd HH:mm", Locale.getDefault())
            binding.tvDate.text = sdf.format(item.timestamp.toDate())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotiViewHolder {
        val binding = ItemNotiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotiViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}