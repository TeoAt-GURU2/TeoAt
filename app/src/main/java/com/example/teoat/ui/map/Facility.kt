package com.example.teoat.ui.map

data class Facility(
    val id: String,
    val name: String,
    val address: String,
    val phone: String, // 전화번호 추가
    val latitude: Double,
    val longitude: Double,
    var isFavorite: Boolean = false
)