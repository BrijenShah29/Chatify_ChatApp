package com.example.chatapp_chatify.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp_chatify.DataClass.CallModel
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.databinding.LayoutCallLogBinding
import com.example.chatapp_chatify.databinding.LayoutMessagesChatscreenBinding
import com.example.chatapp_chatify.utils.Constant
import java.util.*

class CallAdapter(val context: Context) :
    ListAdapter<CallModel, CallAdapter.CallListViewHolder>(DiffUtilCalls()) {

    inner class CallListViewHolder(val binding: LayoutCallLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: CallModel,
            context: Context,
        ) {

            // binding.userName.text = item.name

            Glide.with(context).load(item.callerImage).centerCrop().into(binding.callerImage)
            binding.callerName.text = item.callerName

            val callerTime = getTime(item.timestamp!!)
            binding.callTime.text = callerTime.toString()
            if (item.callFormat == Constant.CALL_TYPE_AUDIO) {
                binding.callType.setImageDrawable(
                    AppCompatResources.getDrawable(context, R.drawable.ic_call))
            } else {
                binding.callType.setImageDrawable(
                    AppCompatResources.getDrawable(context, R.drawable.ic_baseline_video_call_24)
                )
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CallAdapter.CallListViewHolder {
        val binding = LayoutCallLogBinding.inflate(LayoutInflater.from(context), parent, false)
        return CallListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallAdapter.CallListViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, context)
    }

    private fun getTime(lastUpdated: Long): String {
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone(cal.timeZone.toZoneId())
        cal.timeInMillis = lastUpdated!!

        var att = ""
        val tt = cal.get(Calendar.AM_PM)
        att = if (tt == 0) {
            "AM"
        } else {
            "PM"
        }
        if (Calendar.getInstance()
                .get(Calendar.HOUR_OF_DAY) - cal.get(Calendar.HOUR_OF_DAY) >= 12
        ) {
            return String.format("%02d:%02d %s on %s", cal.get(Calendar.HOUR_OF_DAY), cal.get(
                Calendar.MINUTE), att, cal.get(Calendar.DATE))
        } else {
            return String.format("today at %02d:%02d %s", cal.get(Calendar.HOUR_OF_DAY), cal.get(
                Calendar.MINUTE), att)
        }


    }

    class DiffUtilCalls : DiffUtil.ItemCallback<CallModel>() {
        override fun areItemsTheSame(oldItem: CallModel, newItem: CallModel): Boolean {
            return oldItem.callId == newItem.callId &&
                    oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: CallModel, newItem: CallModel): Boolean {
            return oldItem == newItem
        }

    }
}