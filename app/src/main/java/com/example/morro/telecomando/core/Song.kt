package com.example.morro.telecomando.core

import org.json.JSONObject
import java.util.*

/**
 * data model being displayed by the RecyclerView
 */
data class Song(val title: String, val artist: String, val album: String, val year: String, val itemPath: String) {

    fun getJson(): String {
        val song = JSONObject()
        song.put("title", title)
        song.put("artist", artist)
        song.put("album", album)
        song.put("year", year)
        song.put("path", itemPath)
        return song.toString()
    }

    override fun toString(): String {
        return "$title ($artist)"
    }

    /* works like static functions in java classes */
    companion object {
        @JvmStatic
        fun buildDummyTrackList(num: Int = 1): ArrayList<Song> {
            val songs = ArrayList<Song>()
            for (i in 1..num) {
                songs.add(Song("Title$i", "Artist$i", "Album$i", "Year$i", "path$i"))
            }
            return songs
        }
    }
}