package com.example.chatapp_chatify.Adapters

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp_chatify.DataClass.MapsModel.Result
import com.example.chatapp_chatify.MapsFragment
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.databinding.LandmarkLayoutListBinding
import com.example.chatapp_chatify.databinding.LayoutCallLogBinding
import com.example.chatapp_chatify.utils.OnItemSelectedListener
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.internal.managers.FragmentComponentManager


class MapsPlacesAdapter(val context: Context) :ListAdapter<Result,MapsPlacesAdapter.PlacesViewHolder>(DiffUtilMaps()){

    private var listener: OnItemSelectedListener? = null
    private var previousSelectedItem : LinearLayout ? =null
    //= (FragmentComponentManager.findActivity(context) as Activity as FragmentActivity).supportFragmentManager.findFragmentById(R.id.mapsFragment) as OnItemSelectedListener

    init {
       // listener = (FragmentComponentManager.findActivity(context) as Activity as FragmentActivity).supportFragmentManager.findFragmentById(R.id.mapsFragment) as OnItemSelectedListener
    }

    inner class PlacesViewHolder(val binding : LandmarkLayoutListBinding) : RecyclerView.ViewHolder(binding.root)
    {
        fun bind(item: Result,context: Context)
        {
            binding.placeTitle.text = item.name
            binding.placeAddress.text = item.formatted_address
            if(item.opening_hours!=null) {
                if (item.opening_hours.open_now) {

                    binding.placeCurrentlyOpen.text =
                        context.resources.getString(R.string.operational_status)
                } else {
                    binding.placeCurrentlyOpen.text =
                        context.resources.getString(R.string.operational_status_close)
                }
            }
            Glide.with(context).load(item.icon).placeholder(AppCompatResources.getDrawable(context, R.drawable.ic_baseline_holiday_village_24)).centerCrop().into(binding.placeImage)

             binding.root.setOnClickListener {
                 makeItSelected(binding.root)

                 listener?.onItemSelected(item)
             }

        }
    }

    private fun makeItSelected(root: LinearLayout) {
        root.setBackgroundColor(context.resources.getColor(R.color.secondary_color,null))
        previousSelectedItem?.setBackgroundColor(context.resources.getColor(R.color.semiWhite,null))
        previousSelectedItem = root



    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlacesViewHolder {
        val binding =  LandmarkLayoutListBinding.inflate(LayoutInflater.from(context),parent,false)
        return  PlacesViewHolder(binding)
    }


    override fun onBindViewHolder(holder: PlacesViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item,context)
        //listener = FragmentComponentManager.findActivity(context) as Activity as OnItemSelectedListener
    }

    class DiffUtilMaps : DiffUtil.ItemCallback<Result>(){

        override fun areItemsTheSame(oldItem: Result, newItem: Result): Boolean {
            return oldItem.place_id == newItem.place_id
        }


        override fun areContentsTheSame(oldItem: Result, newItem: Result): Boolean {
            return oldItem == newItem
        }

    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener) {
        this.listener = listener
    }



}

