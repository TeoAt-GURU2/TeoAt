package com.example.teoat.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.R

class StoreAdapter(
    private var stores: List<Store>,
    private val onFavoriteClick: (Store) -> Unit,
    private val onItemClick: (Store) -> Unit
) : RecyclerView.Adapter<StoreAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_name)
        val address: TextView = view.findViewById(R.id.tv_address)
        val star: ImageView = view.findViewById(R.id.iv_star)

        init {
            view.setOnClickListener { onItemClick(stores[adapterPosition]) }
            star.setOnClickListener { onFavoriteClick(stores[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_store_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val store = stores[position]
        holder.name.text = store.name
        holder.address.text = store.address
        holder.star.setImageResource(
            if (store.isFavorite) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
    }

    override fun getItemCount() = stores.size

    fun updateData(newStores: List<Store>) {
        stores = newStores
        notifyDataSetChanged()
    }
}