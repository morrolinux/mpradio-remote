package com.example.morro.telecomando.Core;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * data model being displayed by the RecyclerView
 */

public class Song {
    private String itemPath;
    private String title;
    private String artist;
    private String album;
    private String year;

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getYear() {
        return year;
    }

    public String getItemPath() {
        return itemPath;
    }

    public Song(String title, String artist, String album, String year, String itemPath) {
        this.itemPath = itemPath;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.year = year;
    }

    /*
    public static ArrayList<Item> createTrackList(String content,ArrayList<Item> items) {
        String[] lines = content.split(System.getProperty("line.separator"));
        int nLines = lines.length;
        for(int i=1; i <= nLines ; i++){
            items.add(new Item(lines[0]));
        }

        return items;
    }
    */

    public static ArrayList<Song> createTrackList(int num) {
        ArrayList<Song> songs = new ArrayList<Song>();

        for (int i = 1; i <= num; i++) {
            songs.add(new Song("Title", "Artist", "Album", "Year", "path"));
        }

        return songs;
    }


    public String getJson(){
        JSONObject song = new JSONObject();
        try {
            song.put("title", title);
            song.put("artist", artist);
            song.put("album", album);
            song.put("year", year);
            song.put("path", itemPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return song.toString();
    }

}