package com.example.teoat.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R

class FacilityAdapter(
    private var facilities: List<Facility>,
    private val onFavoriteClick: (Facility) -> Unit,
    private val onItemClick: (Facility) -> Unit
) : RecyclerView.Adapter<FacilityAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_facility_name)
        val address: TextView = view.findViewById(R.id.tv_facility_address)
        val phone: TextView = view.findViewById(R.id.tv_facility_phone)
        val star: ImageView = view.findViewById(R.id.iv_facility_star)

        init {
            view.setOnClickListener { onItemClick(facilities[adapterPosition]) }
            star.setOnClickListener { onFavoriteClick(facilities[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_facility_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val facility = facilities[position]
        holder.name.text = facility.name
        holder.address.text = facility.address
        holder.phone.text = facility.phone
        holder.star.setImageResource(
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