package com.example.morro.telecomando.Core;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.content.UriMatcher;

public class ContentPi extends ContentProvider {
    static final String AUTHORITY = "com.example.morro.telecomando.Core.ContentPi";
    static final String BASE_PATH = "library";
    static final String URL = "content://" + AUTHORITY + "/" + BASE_PATH;
    public static final Uri CONTENT_URI = Uri.parse(URL);

    static final String TITLE = "Title";
    static final String ARTIST = "Artist";
    static final String ALBUM = "Album";
    static final String YEAR = "Year";
    static final String PATH = "Path";
    private static Map<String, String> libraryMap;

    private static final int LIBRARY = 1;
    private static final int SETTINGS = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, BASE_PATH, LIBRARY);
        uriMatcher.addURI(AUTHORITY, BASE_PATH, SETTINGS);
    }


    private SQLiteDatabase database;

    List<Song> library;

    MpradioBTHelper mpradioBTHelper;

    public void setup(MpradioBTHelper mpradioBTHelper) {
        this.mpradioBTHelper = mpradioBTHelper;
    }

    // TODO: iterate on all library records and return an array of objects
    private static void getTrackList() {
/*        ArrayList<Song> songs;
        for (song in songs) {
            songs.add(new Song(title, artist, album, year, path));
        }

 */
    }

    @Override
    public boolean onCreate() {
        // library.add(new Song("t1", "a1", "album1", "year1", "path1"));
        // library.add(new Song("t2", "a2", "album2", "year2", "path2"));
        DBHelper dbHelper = new DBHelper(getContext());
        database = dbHelper.getWritableDatabase();
        return true;
    }


    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor;
        switch (uriMatcher.match(uri)) {
            case LIBRARY:
                cursor = database.query(DBHelper.TABLE_LIBRARY, DBHelper.ALL_COLUMNS,
                        selection, null, null, null, DBHelper.SONG_TITLE + " ASC");
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case LIBRARY:
                return "com.example.morro.telecomando.Core.Song";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        // TODO: INSERT ONLY IF NOT PRESENT
        long id = 0;
        try {
            id = database.insertOrThrow(DBHelper.TABLE_LIBRARY, null, values);
        } catch (Exception e) {
            Log.d("MPRADIO", "Insertion Failed for " + uri);
            return null;
        }

        if (id > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, id);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int delCount = 0;
        switch (uriMatcher.match(uri)) {
            case LIBRARY:
                delCount = database.delete(DBHelper.TABLE_LIBRARY, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return delCount;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        int updCount = 0;
        switch (uriMatcher.match(uri)) {
            case LIBRARY:
                updCount = database.update(DBHelper.TABLE_LIBRARY, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return updCount;
    }

}
