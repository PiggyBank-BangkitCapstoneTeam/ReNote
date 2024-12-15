package com.piggybank.renotes.ui.Akun

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.piggybank.renotes.R

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val senderName = view.findViewById<TextView>(R.id.sender_name)
        private val messageText = view.findViewById<TextView>(R.id.message_text)
        private val messageDate = view.findViewById<TextView>(R.id.message_date)

        fun bind(message: Message) {
            senderName.text = message.senderName
            messageText.text = message.messageText
            messageDate.text = message.date
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message_bantuan, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size
}