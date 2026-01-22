package com.example.teoat.ui.map

data class Facility(
    val id: String,
    val name: String,
    val address: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    var isFavorite: Boolean = false
)