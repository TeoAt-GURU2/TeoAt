package com.example.teoat.ui.map

data class Store(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    var isFavorite: Boolean = false
)