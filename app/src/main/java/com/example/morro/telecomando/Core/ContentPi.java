package com.example.morro.telecomando.Core;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContentPi extends ContentProvider {
    static final String PROVIDER_NAME = "com.example.morro.telecomando.Core.ContentPi";
    static final String URL = "content://" + PROVIDER_NAME + "/library";
    static final Uri CONTENT_URI = Uri.parse(URL);

    static final String TITLE = "Title";
    static final String ARTIST = "Artist";
    static final String ALBUM = "Album";
    static final String YEAR = "Year";
    static final String PATH = "Path";
    private static Map<String, String> libraryMap;

    List<Song> library;

    MpradioBTHelper mpradioBTHelper;

    public void setup(MpradioBTHelper mpradioBTHelper) {
        this.mpradioBTHelper = mpradioBTHelper;
    }

    private static void createTrackList(String content, ArrayList<Song> songs) {
        songs.clear();                      //CLEAR instead of adding duplicates
        try {
            Log.d("MPRADIO", "received library:"+ content);
            JSONArray jsonarray = new JSONArray(content);
            JSONObject jsonobject;
            for (int i = 0; i < jsonarray.length(); i++) {
                jsonobject = jsonarray.getJSONObject(i);
                String title = jsonobject.getString("title");
                String artist = jsonobject.getString("artist");
                String album = jsonobject.getString("album");
                String year = jsonobject.getString("year");
                String path = jsonobject.getString("path");
                songs.add(new Song(title, artist, album, year, path));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreate() {
        library.add(new Song("t1", "a1", "album1", "year1", "path1"));
        library.add(new Song("t2", "a2", "album2", "year2", "path2"));
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor c = library;
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    private static class RfcommWrapper {
        Context context;
        RfcommWrapper(Context context) {
            this.context = context
        }

        String result = mpradioBTHelper.sendMessageGetReply();

    }

}
