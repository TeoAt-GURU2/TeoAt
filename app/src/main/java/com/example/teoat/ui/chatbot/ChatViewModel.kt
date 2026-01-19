package com.example.teoat.ui.chatbot

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teoat.data.model.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    // 화면에 보여줄 채팅 목록 (항상 빈 리스트로 시작하도록 함)
    private val _chatUiState = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatUiState : StateFlow<List<ChatMessage>> = _chatUiState.asStateFlow()

    // 네비게이션 이벤트 (화면 이동 명령용)
    private val _navigationEvent = MutableStateFlow<String>("")
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    // Gemini 모델 초기화
    private val generativeModel = Firebase.ai.generativeModel(
        modelName = "gemini-2.5-flash",
        systemInstruction = content {
            text("""
                너는 'TeoAt' 앱의 AI 비서야.
                청소년인 사용자가 자신의 상황을 서술하면, 너는 그 상황에 적절하 지원을 받을 수 있도록 연관된
                시설이나 정책 정보를 알려주고 해당 정보를 담고 있는 앱 내 기능을 찾아서 아래 명령어만 출력해.
                그 외에는 2줄 내외로 짧은 한국어로 답해.
                [명령어]
                - 급식카드 가맹점 -> CMD_STORE
                - 복지시설 -> CMD_FACILITY
                - 정책 정보 -> CMD_POLICY
                - 행사 정보 -> CMD_EVENT
                - 로그인 -> CMD_LOGIN
                - 마이페이지 -> CMD_MYPAGE
            """.trimIndent())
        }
    )

    // 대화 세션 시작 (ViewModel이 생성될 때마다 새로 시작 == 화면 나갔다가 들어오면 새 Chat이 시작됨)
    private val chat = generativeModel.startChat()

    // 메세지 전송 로직
    fun sendMessage(userMessage: String) {
        // 내가 보낸 메세지 화면에 보이기
        addMessage(userMessage, isUser = true)

        // 로딩 메세지
        val loadingId = addMessage("...", isUser = false, isPending = true)

        viewModelScope.launch {
            try {
                val response = chat.sendMessage(userMessage)
                val reply = response.text?.trim() ?: ""

                removeMessage(loadingId) // 로딩 메세지 제거

                if (reply.startsWith("CMD_")) {
                    _navigationEvent.emit(reply)
                    addMessage("이동합니다", false)
                } else {
                    addMessage(reply, false)
                }
            } catch (e: Exception) {
                addMessage("오류: ${e.message}", false)
                Log.e("ChatViewModel", "AI error", e)
            }
        }
    }

    // 헬퍼 함수 : 메시지 추가
    private fun addMessage(text: String, isUser: Boolean, isPending: Boolean = false): String {
        val msg = ChatMessage(text = text, isUser = isUser, isPending = isPending)
        val currentList = _chatUiState.value.toMutableList()
        currentList.add(msg)
        _chatUiState.value = currentList
        return msg.id
    }

    // 헬퍼 함수 : 메시지 삭제 (로딩바 제거용)
    private fun removeMessage(id: String) {
        val currentList = _chatUiState.value.toMutableList()
        currentList.removeIf { it.id == id }
        _chatUiState.value = currentList
    }
}