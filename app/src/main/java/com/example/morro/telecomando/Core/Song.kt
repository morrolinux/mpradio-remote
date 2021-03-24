package com.example.morro.telecomando.Core

import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * data model being displayed by the RecyclerView
 */
class Song(val title: String, val artist: String, val album: String, val year: String, val itemPath: String) {
    val json: String
        get() {
            val song = JSONObject()
            try {
                song.put("title", title)
                song.put("artist", artist)
                song.put("album", album)
                song.put("year", year)
                song.put("path", itemPath)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return song.toString()
        }

    companion object {
        @JvmStatic
        fun buildDummyTrackList(num: Int): ArrayList<Song> {
            val songs = ArrayList<Song>()
            for (i in 1..num) {
                songs.add(Song("Title", "Artist", "Album", "Year", "path"))
            }
            return songs
        }
    }
}