package com.example.teoat.data.model

data class ChatMessage(
    val id      : String = java.util.UUID.randomUUID().toString(),
    val text    : String,
    val isUser  : Boolean,          
    val isPending : Boolean = false,  
    val action : String?= null     
)
