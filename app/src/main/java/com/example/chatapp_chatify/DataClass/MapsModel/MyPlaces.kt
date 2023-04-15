package com.example.chatapp_chatify.DataClass.MapsModel

class MyPlaces {
        var results: Array<Results>? = null

        inner class Results
        {
            var name: String? = null
            var geometry: Geometry? = null
            var formatted_address : String ? = null
            var icon : String? = null

            inner class Geometry {
                var location: Location? = null

                inner class Location {
                    var lat: Double = 0.0
                    var lng: Double = 0.0
                }
            }
        }
    }