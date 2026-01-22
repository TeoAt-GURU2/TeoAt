package com.example.teoat.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R
import com.example.teoat.databinding.ItemFacilityRowBinding

class FacilityAdapter(
    private var facilities: List<Facility>,
    private val onFavoriteClick: (Facility) -> Unit,
    private val onItemClick: (Facility) -> Unit
) : RecyclerView.Adapter<FacilityAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemFacilityRowBinding) : RecyclerView.ViewHolder(binding.root) {


        init {
            binding.root.setOnClickListener { onItemClick(facilities[adapterPosition]) }
            binding.ivFacilityStar.setOnClickListener { onFavoriteClick(facilities[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // LayoutInflater로 바인딩 inflate
        val binding = ItemFacilityRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val facility = facilities[position]
        holder.binding.tvFacilityName.text = facility.name
        holder.binding.tvFacilityAddress.text = facility.address
        holder.binding.tvFacilityPhone.text = facility.phone
        holder.binding.ivFacilityStar.setImageResource(
            if (facility.isFavorite) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
    }

    override fun getItemCount() = facilities.size

    fun updateData(newFacilities: List<Facility>) {
        facilities = newFacilities
        notifyDataSetChanged()
    }
}