package com.example.morro.telecomando.Core;

import android.content.ContentProvider;
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

import java.util.ArrayList;
import java.util.Map;

import static java.lang.Thread.sleep;

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

    private DBWrapper dbHelper;
    private SQLiteQueryBuilder qb;

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

    public long getDbSize(){
        return DatabaseUtils.queryNumEntries(db, LIBRARY_TABLE_NAME);
    }

    public SQLiteDatabase getRawDB(){
        return db;
    }

    // iterate on all library records and return an array of Song objects
    public boolean getTrackList(ArrayList<Song> songs)
    {
        Cursor c = db.rawQuery("SELECT * FROM " + LIBRARY_TABLE_NAME, null);

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
            return true;
        }
        return false;
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
        dbHelper = new DBWrapper(getContext());
        db = dbHelper.getWritableDatabase();

        qb = new SQLiteQueryBuilder();
        qb.setProjectionMap(libraryMap);
        qb.setTables(LIBRARY_TABLE_NAME);
        return db != null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor c;
        c = db.query(LIBRARY_TABLE_NAME, ALL_COLUMNS,
                selection, null, null, null, SONG_TITLE + " ASC");
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
            Uri uriOut = ContentUris.withAppendedId(CONTENT_URI, rowID);
            return uriOut;
        }
        else {
            Log.d("MPRADIO","ERROR: record already exists?");
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
