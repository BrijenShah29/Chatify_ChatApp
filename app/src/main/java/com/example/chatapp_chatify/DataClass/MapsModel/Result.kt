package com.example.chatapp_chatify.DataClass.MapsModel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Result(
    val business_status: String,
    val formatted_address: String,
    val geometry: Geometry,
    val icon: String,
    val icon_background_color: String,
    val icon_mask_base_uri: String,
    val name: String,
    val opening_hours: OpeningHours,
    val place_id: String,
    val rating: Double
):Parcelable{

}