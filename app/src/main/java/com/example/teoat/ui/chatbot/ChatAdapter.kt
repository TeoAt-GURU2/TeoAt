package com.example.teoat.ui.chatbot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.data.model.ChatMessage
import com.example.teoat.databinding.ItemChatBinding

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder>(DiffCallback) {
    class ChatViewHolder(private val binding: ItemChatBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            if (message.isUser) {
                // 사용자가 보낸 메세지 : 오른쪽에 보이기, 왼쪽 숨기기
                binding.layoutUser.visibility = View.VISIBLE
                binding.layoutBot.visibility = View.GONE
                binding.tvUserMsg.text = message.text
            } else {
                // AI가 보낸 메세지 : 왼쪽에 보이기, 오른쪽 숨기기
                binding.layoutBot.visibility = View.VISIBLE
                binding.layoutUser.visibility = View.GONE

                // 로딩 중이면 "..." 표시하고, 아니면 텍스트 표시하기
                if (message.isPending) {
                    binding.tvBotMsg.text = "..."
                } else {
                    binding.tvBotMsg.text = message.text
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // 리스트 갱신 시에 변경된 부분만 효율적으로 찾음
    companion object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}