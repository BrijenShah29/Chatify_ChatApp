package com.example.chatapp_chatify.Adapters

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp_chatify.DataClass.UserStatus
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.databinding.LayoutUserStoriesBinding
import dagger.hilt.android.internal.managers.FragmentComponentManager
import omari.hamza.storyview.StoryView
import omari.hamza.storyview.callback.StoryClickListeners
import omari.hamza.storyview.model.MyStory
import java.util.*


class StoriesAdapter(val context: Context, val userName: String?) : ListAdapter<UserStatus,StoriesAdapter.StoriesViewHolder>(StoriesDiffUtil())
{
    inner class StoriesViewHolder(val binding : LayoutUserStoriesBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(context: Context, item: UserStatus){
                binding.userName.text = item.name.toString()
            binding.userStatusCircles.setPortionsColor(ResourcesCompat.getColor(context.resources, R.color.purple_200,null))
                //show last updated
                val time = getTime(item.lastUpdated!!)
                binding.duration.text = time.toString()
                // setup profile Image
                Glide.with(context).load(Uri.parse(item.profileImage)).centerCrop().into(binding.userImage)
            binding.userStatusCircles.setPortionsCount(item.status.size)
        }

    }

    private fun getTime(lastUpdated: Long) :String {
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone(cal.timeZone.toZoneId())
        cal.timeInMillis = lastUpdated!!

        var att=""
        val tt = cal.get(Calendar.AM_PM)
        att = if(tt==0) {
            "AM"
        } else {
            "PM"
        }
        if( Calendar.getInstance().get(Calendar.HOUR_OF_DAY) - cal.get(Calendar.HOUR_OF_DAY) >= 12)
        {
            return String.format("%02d:%02d %s on %s" ,cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), att,cal.get(Calendar.DATE))
        }else
        {
            return  String.format("today at %02d:%02d %s" ,cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), att)
        }




    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoriesViewHolder {
        val binding = LayoutUserStoriesBinding.inflate(LayoutInflater.from(context),parent,false)
        return StoriesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoriesViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(context,item)
        val time = getTime(item.lastUpdated!!)
        holder.binding.userImage.setOnClickListener {
            // start story and turn down the border color
            holder.binding.userStatusCircles.setPortionsColor(ResourcesCompat.getColor(context.resources, R.color.primary_color,null))
            val storyArray = ArrayList<MyStory>()
            for(status in item.status){
               val data =  MyStory(status.imageUrl,Date(status.timeStamp!!))
                storyArray.add(data)
            }
            showStories(storyArray,holder.itemView.context, item.name!!,item.profileImage.toString(),time)
        }
    }

    private fun showStories(
        storyArray: ArrayList<MyStory>,
        context: Context,
        name: String,
        userProfile: String,
        time: String
    ) {
        StoryView.Builder((FragmentComponentManager.findActivity(context) as Activity as FragmentActivity).supportFragmentManager)
            .setStoriesList(storyArray) // Required
            .setStoryDuration(5000) // Default is 2000 Millis (2 Seconds)
            .setTitleText(name) // Default is Hidden
            .setSubtitleText("") // Default is Hidden
            .setTitleLogoUrl(Uri.parse(userProfile).toString()) // Default is Hidden
            .setStoryClickListeners(object : StoryClickListeners {
                override fun onDescriptionClickListener(position: Int) {
                    //your action
                }

                override fun onTitleIconClickListener(position: Int) {
                    // navigate current user to that user's chat list
                }
            }) // Optional Listeners
            .build() // Must be called before calling show method
            .show()

    }


}

class StoriesDiffUtil : DiffUtil.ItemCallback<UserStatus>(){
    override fun areItemsTheSame(oldItem: UserStatus, newItem: UserStatus): Boolean {
        return oldItem.lastUpdated == newItem.lastUpdated
    }

    override fun areContentsTheSame(oldItem: UserStatus, newItem: UserStatus): Boolean {
        return oldItem == newItem
    }

}
