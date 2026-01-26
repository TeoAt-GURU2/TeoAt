package com.example.teoat.data.model

data class ChatMessage(
    val id      : String = java.util.UUID.randomUUID().toString(),
    val text    : String,
    val isUser  : Boolean,          // true=사용자가 보낸 메세지(오른쪽에 표시), false= AI(왼쪽에 표시)
    val isPending : Boolean = false, // 로딩 중 표시용
    val action : String?= null      // 이동할 명령어 받는용
)
