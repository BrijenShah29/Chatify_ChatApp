package com.example.chatapp_chatify.Adapters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface.BOLD
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.FirebaseMessagesViewModel
import com.example.chatapp_chatify.databinding.LayoutMessagesChatscreenBinding
import com.example.chatapp_chatify.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class ChatListAdapter(
    val context: Context,
    val userManager: UserManager,
    val auth: FirebaseAuth,
    val db: FirebaseDatabase,
    val messageViewModel: FirebaseMessagesViewModel
) : ListAdapter<Users,ChatListAdapter.ChatListViewHolder>(DiffUtil()){
    inner class ChatListViewHolder(val binding : LayoutMessagesChatscreenBinding) : RecyclerView.ViewHolder(binding.root){
        val previousTimeStamp : Long = 0
        fun bind(item: Users, context: Context, senderRoom: String) {

            binding.userName.text = item.name
            Glide.with(context).load(item.profileImage).centerCrop().into(binding.userImage)

        }


    }


    private fun getTimeFromLong(timestamp: Long): String {

        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone(cal.timeZone.toZoneId())
        cal.timeInMillis = timestamp
        var att=""
        val tt = cal.get(Calendar.AM_PM)
        att = if(tt==0) {
            "AM"
        } else {
            "PM"
        }
        return String.format("%02d:%02d %s", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), att)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val binding = LayoutMessagesChatscreenBinding.inflate(LayoutInflater.from(context),parent,false)
        return ChatListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        val item = getItem(position)
        var previousTime : Long = 0
        val senderRoom = auth.currentUser?.uid.toString() + item.uid.toString()
        holder.bind(item,context, senderRoom)

        db.reference.child("Chats").child(senderRoom).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()) {

                    val lastMessage = snapshot.child("LastMessage").getValue(String::class.java)
                    val longTime = snapshot.child("LastMessageTime").getValue(Long::class.java)?.toLong()
                    holder.binding.messageText.text = lastMessage.toString()
                    holder.binding.messageText.setTextColor(Color.BLACK)
                    holder.binding.messageText.setTextAppearance(BOLD)
                    holder.binding.messageTime.text = getTimeFromLong(longTime!!)

                    if(previousTime!=longTime){
                        holder.binding.messageBadgeText.visibility = VISIBLE
                        previousTime = longTime
                    }
                    else
                    {
                        holder.binding.messageBadgeText.visibility = INVISIBLE
                    }
                    //holder.binding.messageDoubleTick.visibility = View.GONE
                }else
                {
                    holder.binding.messageBadgeText.visibility = INVISIBLE
                    holder.binding.messageText.text = "Tap To Chat"
                }
            }
            override fun onCancelled(error: DatabaseError) {

            }

        })




        holder.itemView.setOnClickListener {

            // SAVING USER AS OPPOSITE CHAT PARTNER
            userManager.saveOppositeUserId(item.uid)
            userManager.saveOppositeUserImage(item.profileImage)
            userManager.saveOppositePhoneNumber(item.phoneNumber)
            userManager.saveOppositeUserName(item.name)


            messageViewModel.fetchMessagesForOffLine(senderRoom)
            //messageViewModel.startListeningToMessages(senderRoom)


            // Navigate to the user chat screen with data of users

            val bundle = Bundle()
            bundle.putParcelable("UserData",item)
            bundle.putString("senderRoom",senderRoom)

            Navigation.findNavController(holder.itemView).navigate(R.id.action_chatScreenFragment_to_userChatFragment,bundle)



        }

    }

    class DiffUtil() : androidx.recyclerview.widget.DiffUtil.ItemCallback<Users>() {

        override fun areItemsTheSame(oldItem: Users, newItem: Users): Boolean {
            return oldItem.uid == newItem.uid &&
                    oldItem.name == newItem.name &&
                    oldItem.phoneNumber == newItem.phoneNumber

        }


        override fun areContentsTheSame(oldItem: Users, newItem: Users): Boolean {
            return oldItem.uid == newItem.uid &&
                    oldItem.name == newItem.name &&
                    oldItem.phoneNumber == newItem.phoneNumber

        }
    }

}
