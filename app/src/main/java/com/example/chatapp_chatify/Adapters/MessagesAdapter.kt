package com.example.chatapp_chatify.Adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.chatapp_chatify.DataClass.MessagesModel
import com.example.chatapp_chatify.MapsFragment
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.databinding.IncomingMessageLayoutBinding
import com.example.chatapp_chatify.databinding.LayoutDeleteMessageBinding
import com.example.chatapp_chatify.databinding.LayoutOutgoingMessageBinding
import com.example.chatapp_chatify.utils.Constant
import com.example.chatapp_chatify.utils.Constant.Companion.TAG
import com.github.pgreze.reactions.ReactionPopup
import com.github.pgreze.reactions.ReactionSelectedListener
import com.github.pgreze.reactions.ReactionsConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException
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
        private val mediaPlayer = MediaPlayer()
    }

    inner class OutGoingMessageViewHolder(val viewBinding: LayoutOutgoingMessageBinding) : RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(chatMessage: MessagesModel) {
            val displayTime = getTimeFromLong(chatMessage.timestamp)
            val previousAudioDataSource : String

       if (chatMessage.message != "This message is removed.") {

            when (chatMessage.messageType) {
                Constant.MESSAGE_TYPE_TEXT -> {
                    viewBinding.outgoingMessageText.text = chatMessage.message.toString()
                    viewBinding.outGoingMessageLayout.visibility = View.VISIBLE
                    viewBinding.audioPlayerLayout.visibility = View.GONE
                    viewBinding.sentImageLayout.visibility = View.GONE
                    viewBinding.locationLayout.visibility = View.GONE
                }
                Constant.MESSAGE_TYPE_IMAGE -> {
                    viewBinding.outGoingMessageLayout.visibility = View.GONE
                    viewBinding.audioPlayerLayout.visibility = View.GONE
                    viewBinding.locationLayout.visibility = View.GONE
                    viewBinding.sentImageLayout.visibility = View.VISIBLE
                    Glide.with(context).load(Uri.parse(chatMessage.message)).placeholder(
                        AppCompatResources.getDrawable(context,R.drawable.gallary)).centerCrop()
                        .into(viewBinding.sentImage)
                }
                Constant.MESSAGE_TYPE_AUDIO -> {
                    viewBinding.outGoingMessageLayout.visibility = View.GONE
                    viewBinding.sentImageLayout.visibility = View.GONE
                    viewBinding.locationLayout.visibility = View.GONE
                    viewBinding.audioPlayerLayout.visibility = View.VISIBLE
                    viewBinding.audioPlayText.text = "Audio File"
                    previousAudioDataSource = chatMessage.message.toString()

                    viewBinding.audioPlayButton.setImageDrawable(AppCompatResources.getDrawable(context,me.jagar.chatvoiceplayerlibrary.R.drawable.ic_play_arrow_white_24dp))

                    viewBinding.audioPlayButton.setOnClickListener {
                        if (viewBinding.audioPlayText.text == "Audio File") {
                            // set drawable of pause
                            viewBinding.audioPlayButton.setImageDrawable(AppCompatResources.getDrawable(context,R.drawable.ic_baseline_pause_24))
                            // start the player
                            try {
                                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                                mediaPlayer.setDataSource(previousAudioDataSource)
                                mediaPlayer.prepare()
                                mediaPlayer.start()
                                viewBinding.audioPlayButton.isClickable = false
                                mediaPlayer.setOnCompletionListener {
                                    viewBinding.audioPlayButton.isClickable = true
                                    viewBinding.audioPlayButton.setImageDrawable(
                                        AppCompatResources.getDrawable(context,me.jagar.chatvoiceplayerlibrary.R.drawable.ic_play_arrow_white_24dp)
                                    )
                                    mediaPlayer.stop()
                                    mediaPlayer.release()
                                }

                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            // (context as ContextWrapper).baseContext


                        }
                    }
                }

                Constant.MESSAGE_TYPE_LOCATION -> {
                    viewBinding.outGoingMessageLayout.visibility = View.GONE
                    viewBinding.audioPlayerLayout.visibility = View.GONE
                    viewBinding.sentImageLayout.visibility = View.GONE
                    viewBinding.locationLayout.visibility = View.VISIBLE
                }
                else -> {
                    Log.d(TAG, "Invalid Message Type exception ")
                }
            }

            viewBinding.outgoingMessageTime.text = displayTime

                if (chatMessage.messageReaction?.toInt()!! <= 5) {
                    setTheAnimation(chatMessage.messageReaction!!, viewBinding.outgoingReaction)

                }

                val popup = ReactionPopup(
                    context, config,
                    (object : ReactionSelectedListener {
                        override fun invoke(position: Int): Boolean {
                            setTheAnimation(position, viewBinding.outgoingReaction)
                            //update message in firebase
                            updateMessageInFirebase(
                                position,
                                chatMessage,
                                chatMessage.message!!,
                                chatMessage.messageType!!
                            )
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
                viewBinding.messageLayout.setOnTouchListener { v, event ->
                    popup.onTouch(v, event)
                }

           itemView.setOnLongClickListener(object : OnLongClickListener {

               override fun onLongClick(v: View?): Boolean {
                   val view = LayoutInflater.from(context)
                       .inflate(R.layout.layout_delete_message, null)
                   val binding = LayoutDeleteMessageBinding.bind(view)
                   val dialog = AlertDialog.Builder(context).setTitle("Delete Message")
                       .setView(binding.root).create()

                   binding.everyone.setOnClickListener {
                       updateMessageInFirebase(-1,chatMessage,"This message is removed.",Constant.MESSAGE_TYPE_TEXT)
                       chatMessage.message = "This message is removed."
                       dialog.dismiss()
                   }
                   binding.delete.setOnClickListener {
                       deleteMessageForSender(senderRoom,chatMessage.messageId)
                       itemView.visibility = View.GONE
                       dialog.dismiss()
                   }

                   binding.cancel.setOnClickListener { dialog.dismiss() }

                   dialog.show()
                   return false
               }
           })
            }
       else
       {
           viewBinding.outgoingMessageText.text = chatMessage.message.toString()
           viewBinding.outGoingMessageLayout.visibility = View.VISIBLE
           viewBinding.audioPlayerLayout.visibility = View.GONE
           viewBinding.sentImageLayout.visibility = View.GONE
           viewBinding.outgoingMessageTime.text = displayTime
       }

            viewBinding.mapButton.setOnClickListener {
                // open new map fragment parse lat lng to it.
                val bundle = Bundle()
                bundle.putString("Location",chatMessage.message.toString())
                Navigation.findNavController(viewBinding.root).navigate(R.id.getPlaceLocationFragment,bundle)

            }

        }

    }


    inner class IncomingMessageViewHolder(val binding: IncomingMessageLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SuspiciousIndentation")
        fun messageBind(chatMessage: MessagesModel) {
            //val date = Calendar.getInstance()


            // SETTING UP NOTIFICATION FOR INCOMING MESSAGES

            val displayTime = getTimeFromLong(chatMessage.timestamp)
            binding.tvIncomingTime.text = displayTime
    if (chatMessage.message != "This message is removed.")
    {

            when (chatMessage.messageType) {
                Constant.MESSAGE_TYPE_TEXT -> {
                    binding.tvIncomingMessage.text = chatMessage.message.toString()
                    binding.messageLayout.visibility = View.VISIBLE
                    binding.audioPlayerLayout.visibility = View.GONE
                    binding.incomingImageLayout.visibility = View.GONE
                    binding.incomingLocationLayout.visibility = View.GONE
                }
                Constant.MESSAGE_TYPE_IMAGE -> {
                    binding.messageLayout.visibility = View.GONE
                    binding.audioPlayerLayout.visibility = View.GONE
                    binding.incomingLocationLayout.visibility = View.GONE
                    binding.incomingImageLayout.visibility = View.VISIBLE
                    Glide.with(context).load(Uri.parse(chatMessage.message)).placeholder(
                        AppCompatResources.getDrawable(context,R.drawable.gallary)).centerCrop()
                        .into(binding.incomingImage)
                }
                Constant.MESSAGE_TYPE_AUDIO -> {
                    binding.messageLayout.visibility = View.GONE
                    binding.incomingImageLayout.visibility = View.GONE
                    binding.incomingLocationLayout.visibility = View.GONE
                    binding.audioPlayerLayout.visibility = View.VISIBLE
                    // set up audio player

                    binding.audioPlayText.text = "Audio File"
                    binding.audioPlayButton.setImageDrawable(AppCompatResources.getDrawable(context,R.drawable.ic_play_blue))

                    binding.audioPlayButton.setOnClickListener {
                        if (binding.audioPlayText.text == "Audio File") {
                            // set drawble of pause
                            binding.audioPlayButton.setImageDrawable(AppCompatResources.getDrawable(context,R.drawable.ic_baseline_pause_24_blue))
                            binding.audioPlayText.text == "Playing..."

                            // start the player
                            try {
                                mediaPlayer.setDataSource(chatMessage.message)
                                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                                mediaPlayer.prepare()
                                mediaPlayer.start()
                                binding.audioPlayButton.isClickable = false
                                mediaPlayer.setOnCompletionListener {
                                    binding.audioPlayButton.isClickable = true
                                    binding.audioPlayButton.setImageDrawable(AppCompatResources.getDrawable(context,R.drawable.ic_play_blue))
                                    binding.audioPlayText.text == "Stopped"
                                    mediaPlayer.stop()
                                    mediaPlayer.release()
                                }

                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }

                    }
                }
                Constant.MESSAGE_TYPE_LOCATION ->
                {
                    binding.audioPlayerLayout.visibility = View.GONE
                    binding.incomingImageLayout.visibility = View.GONE
                    binding.messageLayout.visibility = View.GONE
                    binding.incomingLocationLayout.visibility = View.VISIBLE
                }
                else -> {
                    Log.d(TAG, "Invalid Message Type exception ")
                }
            }

            binding.tvIncomingMessage.text = chatMessage.message

            //binding.tvIncomingTime.text= date.get(Calendar.HOUR_OF_DAY).toString() + date.get(Calendar.MINUTE).toString()

                if (chatMessage.messageReaction?.toInt()!! <= 5) {
                    setTheAnimation(chatMessage.messageReaction!!.toInt(), binding.incomingReaction)

                }

                val popup = ReactionPopup(
                    context, config,
                    (object : ReactionSelectedListener {
                        override fun invoke(position: Int): Boolean {
                            setTheAnimation(position, binding.incomingReaction)
                            updateMessageInFirebase(
                                position,
                                chatMessage,
                                chatMessage.message!!,
                                chatMessage.messageType!!
                            )
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
                binding.cardView4.setOnTouchListener { v, event ->
                    popup.onTouch(v, event)
                }

        itemView.setOnLongClickListener(object : OnLongClickListener {

            override fun onLongClick(v: View?): Boolean {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.layout_delete_message, null)
                val binding = LayoutDeleteMessageBinding.bind(view)
                val dialog = AlertDialog.Builder(context).setTitle("Delete Message")
                    .setView(binding.root).create()
                binding.everyone.visibility = View.GONE

                binding.delete.setOnClickListener {
                    deleteMessageForSender(receiverRoom,chatMessage.messageId)
                    itemView.visibility = View.GONE
                    dialog.dismiss()
                }

                binding.cancel.setOnClickListener { dialog.dismiss() }

                dialog.show()
                return false
            }
        })


            }
            else
            {
                binding.tvIncomingMessage.text = chatMessage.message
                binding.messageLayout.visibility = View.VISIBLE
                binding.audioPlayerLayout.visibility = View.GONE
                binding.incomingImageLayout.visibility = View.GONE

    }
            binding.mapButton.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("Location",chatMessage.message.toString())
                Navigation.findNavController(binding.root).navigate(R.id.getPlaceLocationFragment,bundle)
            }

        }

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

        var att=""
        val tt = cal.get(Calendar.AM_PM)
        att = if(tt==0) {
            "AM"
        } else {
            "PM"
        }
        return String.format("%02d:%02d %s", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),att)
    }

    private fun updateMessageInFirebase(position: Int, item: MessagesModel, message: String,messageType : Int) {
        val data = MessagesModel(
            item.messageId,
            message,
            item.senderId,
            item.timestamp,
            position,
            item.senderRoom,
            messageType
        )

        db.child(senderRoom).child("messages").child(item.messageId).setValue(data)
        db.child(receiverRoom).child("messages").child(item.messageId).setValue(data)

    }

    private fun deleteMessageForSender(senderRoom: String, messageId: String) {
        FirebaseDatabase.getInstance().reference
            .child("chats")
            .child(senderRoom)
            .child("messages")
            .child(messageId).setValue(null)

    }


}

class MessagesDiffUtil : DiffUtil.ItemCallback<MessagesModel>() {

    override fun areItemsTheSame(oldItem: MessagesModel, newItem: MessagesModel): Boolean {
        return oldItem.message.toString() == newItem.message.toString() &&
                oldItem.messageId == newItem.messageId &&
                oldItem.timestamp == newItem.timestamp &&
                oldItem.senderId == newItem.senderId &&
                oldItem.messageType == newItem.messageType
//        return oldItem.message == newItem.message
    }


    override fun areContentsTheSame(oldItem: MessagesModel, newItem: MessagesModel): Boolean {
       return oldItem == newItem
//        return oldItem.messageId == newItem.messageId &&
//                oldItem.message == newItem.message &&
//                oldItem.messageReaction == oldItem.messageReaction
    }

}
