package com.example.teoat.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.teoat.databinding.ItemBannerBinding

data class BannerItem(val imageRes : Int)

class BannerAdapter(private val bannerList : List<BannerItem>)
    : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    inner class BannerViewHolder(private val binding: ItemBannerBinding):
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bannerItem: BannerItem) {
            Glide.with(binding.root.context)
                .load(bannerItem.imageRes)
                .into(binding.ivBannerImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : BannerViewHolder {
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        // 실제 배너 이미지 수만큼 바인딩
        holder.bind(bannerList[position % bannerList.size])
    }
    override fun getItemCount() : Int = bannerList.size
}
