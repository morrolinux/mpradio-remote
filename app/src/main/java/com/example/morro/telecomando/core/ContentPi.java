package com.example.morro.telecomando.core;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class ContentPi extends ContentProvider {
    static final String AUTHORITY = "com.example.morro.telecomando.Core.ContentPi";
    static final String BASE_PATH = "library";
    static final String URL = "content://" + AUTHORITY + "/" + BASE_PATH;
    public static final Uri CONTENT_URI = Uri.parse(URL);

    /* DB Table(s) and Columns */
    public static final String SONG_PATH = "path";
    public static final String SONG_TITLE = "title";
    public static final String SONG_ARTIST = "artist";
    public static final String SONG_ALBUM = "album";
    public static final String SONG_YEAR = "year";
    static final String[] ALL_COLUMNS = {SONG_PATH, SONG_TITLE, SONG_ARTIST, SONG_ALBUM, SONG_YEAR};

    private static Map<String, String> libraryMap;

    private SQLiteDatabase db;
    static final String DATABASE_NAME = "mpradio.db";
    static final String LIBRARY_TABLE_NAME = "library";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_TABLE =
            String.format("CREATE TABLE %s (%s TEXT PRIMARY KEY, %s TEXT, %s TEXT, %s TEXT, %s TEXT)",
                    LIBRARY_TABLE_NAME, SONG_PATH, SONG_TITLE, SONG_ARTIST, SONG_ALBUM, SONG_YEAR);

    private static class DBWrapper extends SQLiteOpenHelper {
        DBWrapper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + LIBRARY_TABLE_NAME);
            onCreate(db);
        }
    }

    public static long getDbSize(Context context){
        ContentResolver resolver = context.getContentResolver();
        ContentProviderClient client = resolver.acquireContentProviderClient(ContentPi.CONTENT_URI);
        assert client != null;
        ContentPi contentPi = (ContentPi) client.getLocalContentProvider();
        assert contentPi != null;
        long size = DatabaseUtils.queryNumEntries(contentPi.db, LIBRARY_TABLE_NAME);
        client.release();
        return size;
    }

    public SQLiteDatabase getRawDB(){
        return db;
    }

    public static void dbQuery(ArrayList<Song> songs, Context context, String selectionClause, String[] selArgs) {
        Cursor c = context.getContentResolver().query(ContentPi.CONTENT_URI, null, selectionClause, selArgs,null);

        if(c!=null && c.getCount() > 0) {
            songs.clear();
            c.moveToFirst();

            int indexTitle = c.getColumnIndex(SONG_TITLE);
            int indexAlbum = c.getColumnIndex(SONG_ALBUM);
            int indexArtist = c.getColumnIndex(SONG_ARTIST);
            int indexYear = c.getColumnIndex(SONG_YEAR);
            int indexPath = c.getColumnIndex(SONG_PATH);
            do {
                songs.add(new Song(
                        c.getString(indexTitle), c.getString(indexArtist),
                        c.getString(indexAlbum), c.getString(indexYear), c.getString(indexPath)));
            } while (c.moveToNext());
            c.close();
        }
    }

    public static void dbClear(Context context) {
        context.getContentResolver().delete(ContentPi.CONTENT_URI, null, null);
    }

    public static void dbCreateFromJSON(String content, Context context) {
        if (content == null || content.length() < 1) {
            Log.d("MPRADIO", "Received abnormal response from Pi while fetching playlist");
            return;
        }

        dbClear(context);      //Make sure we don't keep stuff that's been deleted on the Pi

        try {
            JSONArray jsonarray = new JSONArray(content);
            JSONObject jsonobject;
            for (int i = 0; i < jsonarray.length(); i++) {
                jsonobject = jsonarray.getJSONObject(i);

                String title = jsonobject.getString("title");
                String artist = jsonobject.getString("artist");
                String album = jsonobject.getString("album");
                String year = jsonobject.getString("year");
                String path = jsonobject.getString("path");

                dbInsertSong(title, artist, album, path, year, context);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void dbInsertSong(String title, String artist, String album, String path, String year, Context context) {
        ContentValues values = new ContentValues();
        values.put(ContentPi.SONG_TITLE, title);
        values.put(ContentPi.SONG_ARTIST, artist);
        values.put(ContentPi.SONG_ALBUM, album);
        values.put(ContentPi.SONG_PATH, path);
        values.put(ContentPi.SONG_YEAR, year);
        context.getContentResolver().insert(ContentPi.CONTENT_URI, values);
    }

    public void debugContent() {
        Cursor c = db.rawQuery("SELECT * FROM " + LIBRARY_TABLE_NAME, null);
        if (c != null) {
            c.moveToFirst();
            do {
                String s = "";

                for (int i = 0; i < c.getColumnCount(); i++) {
                    s += " || " + c.getString(i);
                }

                Log.d("MPRADIO", s);

            } while (c.moveToNext());
            c.close();
        }
    }

    @Override
    public boolean onCreate() {
        // library.add(new Song("t1", "a1", "album1", "year1", "path1"));
        // library.add(new Song("t2", "a2", "album2", "year2", "path2"));
        DBWrapper dbHelper = new DBWrapper(getContext());
        db = dbHelper.getWritableDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setProjectionMap(libraryMap);
        qb.setTables(LIBRARY_TABLE_NAME);
        return db != null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor c;
        c = db.query(LIBRARY_TABLE_NAME, ALL_COLUMNS,
                selection, selectionArgs, null, null, SONG_TITLE + " ASC");
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }


    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "com.example.morro.telecomando.Core.Song";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        long rowID = -1;

        try{
            rowID = db.insertOrThrow(LIBRARY_TABLE_NAME, "", values);
        } catch (android.database.sqlite.SQLiteConstraintException e){
            Log.d("MPRADIO","SQLiteConstraintException");
        }

        if (rowID > 0) {
            return ContentUris.withAppendedId(CONTENT_URI, rowID);
        }
        else {
            Log.e("MPRADIO","ERROR: record already exists? " + values);
            return null;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int delCount = 0;

        delCount = db.delete(LIBRARY_TABLE_NAME, selection, selectionArgs);

        getContext().getContentResolver().notifyChange(uri, null);
        return delCount;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        int updCount = 0;
        updCount = db.update(LIBRARY_TABLE_NAME, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return updCount;
    }

}
