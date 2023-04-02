package com.example.chatapp_chatify.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.chatapp_chatify.DataClass.MessagesModel
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.databinding.IncomingMessageLayoutBinding
import com.example.chatapp_chatify.databinding.LayoutOutgoingMessageBinding
import com.example.chatapp_chatify.utils.Constant
import com.example.chatapp_chatify.utils.Constant.Companion.TAG
import com.github.pgreze.reactions.ReactionPopup
import com.github.pgreze.reactions.ReactionSelectedListener
import com.github.pgreze.reactions.ReactionsConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import java.util.*

class MessagesAdapter(
    val context: Context,
    val senderRoom: String,
    val receiverRoom: String,
    val db: DatabaseReference,
    val config: ReactionsConfig,
    val auth: FirebaseAuth,
) :
    ListAdapter<MessagesModel, RecyclerView.ViewHolder>(MessagesDiffUtil()) {

    companion object {
        private const val TYPE_RECEIVE_MESSAGE = 0
        private const val TYPE_SENT_MESSAGE = 1
        private const val TYPE_DATE_HEADER = 2
    }

    inner class OutGoingMessageViewHolder(val viewBinding: LayoutOutgoingMessageBinding) : RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(chatMessage: MessagesModel) {
            val displayTime = getTimeFromLong(chatMessage.timestamp)

            when (chatMessage.messageType) {
                Constant.MESSAGE_TYPE_TEXT -> {
                    viewBinding.outgoingMessageText.text = chatMessage.message.toString()
                    viewBinding.audioPlayerLayout.visibility = View.GONE
                    viewBinding.sentImageLayout.visibility = View.GONE
                }
                Constant.MESSAGE_TYPE_IMAGE -> {
                    viewBinding.outGoingMessageLayout.visibility = View.GONE
                    viewBinding.audioPlayerLayout.visibility = View.GONE
                    viewBinding.sentImageLayout.visibility = View.VISIBLE
                    Glide.with(context).load(Uri.parse(chatMessage.message)).centerCrop().into(viewBinding.sentImage)
                }
                Constant.MESSAGE_TYPE_AUDIO -> {
                    viewBinding.outGoingMessageLayout.visibility = View.GONE
                    viewBinding.sentImageLayout.visibility = View.GONE
                    viewBinding.audioPlayerLayout.visibility = View.VISIBLE
                }
                else -> {
                    Log.d(TAG,"Invalid Message Type exception ")
                }
            }

            viewBinding.outgoingMessageTime.text = displayTime


            if(chatMessage.messageReaction?.toInt()!! <=5 )
            {
                setTheAnimation(chatMessage.messageReaction!!, viewBinding.outgoingReaction)

            }

            val popup = ReactionPopup(
                context, config,
                (object : ReactionSelectedListener {
                    override fun invoke(position: Int): Boolean {
                        setTheAnimation(position, viewBinding.outgoingReaction)
                        //update message in firebase
                        updateMessageInFirebase(position,chatMessage)
                        return true
                    }
                })
            ) { pos ->
                true.also {
                    if (it) {
                        viewBinding.outgoingReaction.visibility = VISIBLE
                    } else {
                        viewBinding.outgoingReaction.visibility = INVISIBLE
                    }
                    // position = -1 if no selection
                }
            }
            viewBinding.outgoingMessageText.setOnTouchListener { v, event ->
                popup.onTouch(v, event)
            }
        }

        // UPDATE if message seen functionality
//            if(chatMessage.isSeen)
//            {
//
//            }
    }


    inner class IncomingMessageViewHolder(val binding: IncomingMessageLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun messageBind(chatMessage: MessagesModel) {
            //val date = Calendar.getInstance()



            val displayTime = getTimeFromLong(chatMessage.timestamp)
            binding.tvIncomingMessage.text = chatMessage.message
            binding.tvIncomingTime.text = displayTime



            //binding.tvIncomingTime.text= date.get(Calendar.HOUR_OF_DAY).toString() + date.get(Calendar.MINUTE).toString()
            if(chatMessage.messageReaction?.toInt()!! <=5 )
            {
                setTheAnimation(chatMessage.messageReaction!!.toInt(), binding.incomingReaction)

            }

            val popup = ReactionPopup(
                context, config,
                (object : ReactionSelectedListener {
                    override fun invoke(position: Int): Boolean {
                        setTheAnimation(position, binding.incomingReaction)
                        updateMessageInFirebase(position,chatMessage)
                        return true
                    }
                })
            ) { pos ->
                true.also {
                    if (it) {
                        binding.incomingReaction.visibility = VISIBLE
                    } else {
                        binding.incomingReaction.visibility = INVISIBLE
                    }
                    // position = -1 if no selection
                }

            }
            binding.tvIncomingMessage.setOnTouchListener { v, event ->
                popup.onTouch(v, event)
            }


        }

//        inner class DateMessageViewHolder(private val binding : ):RecyclerView.ViewHolder(binding.root)
//        {
//            fun bind(context: Context)
//            {
//                binding.outgoingMessageText.text = chatMessage.message.toString()
//
//
//
//            }

    }

    override fun getItemViewType(position: Int): Int {
        return if (auth.currentUser!!.uid.toString() == getItem(position).senderId) {
            TYPE_SENT_MESSAGE
        }else {
            TYPE_RECEIVE_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_RECEIVE_MESSAGE -> {
                val binding = IncomingMessageLayoutBinding.inflate(inflater, parent, false)
                IncomingMessageViewHolder(binding)
            }
            TYPE_SENT_MESSAGE -> {
                val binding = LayoutOutgoingMessageBinding.inflate(inflater, parent, false)
                OutGoingMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {


        when (holder) {
            is IncomingMessageViewHolder -> {
                val item = getItem(position)
                holder.messageBind(item)


            }
            is OutGoingMessageViewHolder -> {
                val item = getItem(position)
                holder.bind(item)
            }
        }
    }

    private fun setTheAnimation(position: Int, incomingOutgoingReaction: LottieAnimationView) {

        when (position) {
            0 -> {
                incomingOutgoingReaction.visibility = VISIBLE
                incomingOutgoingReaction.setAnimation(R.raw.thumbs_up_like)
                incomingOutgoingReaction.playAnimation()
                // update message in database
                // updateMessageInFirebase(position,item)

            }
            1 -> {
                incomingOutgoingReaction.visibility = VISIBLE
                incomingOutgoingReaction.setAnimation(R.raw.laugh_emoji)
                incomingOutgoingReaction.playAnimation()
                // update message in database
            }
            2 -> {
                incomingOutgoingReaction.visibility = VISIBLE
                incomingOutgoingReaction.setAnimation(R.raw.love_emoji)
                incomingOutgoingReaction.playAnimation()
            }
            3 -> {
                incomingOutgoingReaction.visibility = VISIBLE
                incomingOutgoingReaction.setAnimation(R.raw.wow_emoji)
                incomingOutgoingReaction.playAnimation()
            }
            4 -> {
                incomingOutgoingReaction.visibility = VISIBLE
                incomingOutgoingReaction.setAnimation(R.raw.crying_emoji)
                incomingOutgoingReaction.playAnimation()
            }
            5 -> {
                incomingOutgoingReaction.visibility = VISIBLE
                incomingOutgoingReaction.setAnimation(R.raw.angry_emoji)
                incomingOutgoingReaction.playAnimation()
            }
            else -> {
                incomingOutgoingReaction.visibility = GONE
                incomingOutgoingReaction.setAnimation(R.raw.blank)

            }
        }

    }

    private fun getTimeFromLong(timestamp: Long?): String {

        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone(cal.timeZone.toZoneId())
        cal.timeInMillis = timestamp!!
        return String.format("%2d : %2d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

    }

    private fun updateMessageInFirebase(position: Int, item: MessagesModel) {
        val data = MessagesModel(
            item.messageId,
            item.message,
            item.senderId,
            item.timestamp,
            position,
            item.senderRoom
        )

        db.child(senderRoom).child("messages").child(item.messageId).setValue(data)
        db.child(receiverRoom).child("messages").child(item.messageId).setValue(data)

    }
    private fun updateMessageInFirebaseForReceiver(position: Int, item: MessagesModel) {
        val data = MessagesModel(
            item.messageId,
            item.message,
            item.senderId,
            item.timestamp,
            position
        )

        db.child(receiverRoom).child("messages").child(item.messageId).setValue(data)

    }


}

class MessagesDiffUtil : DiffUtil.ItemCallback<MessagesModel>() {

    override fun areItemsTheSame(oldItem: MessagesModel, newItem: MessagesModel): Boolean {
        return oldItem.message.toString() == newItem.message.toString() &&
                oldItem.messageId == newItem.messageId &&
                oldItem.timestamp == newItem.timestamp &&
                oldItem.senderId == newItem.senderId &&
                oldItem.messageReaction == newItem.messageReaction
//        return oldItem.message == newItem.message
    }


    override fun areContentsTheSame(oldItem: MessagesModel, newItem: MessagesModel): Boolean {
//        return oldItem.message == newItem.message
        return oldItem == newItem
    }

}
