package com.example.morro.telecomando.Core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mpradio.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_LIBRARY = "library";
    public static final String SONG_PATH = "path";
    public static final String SONG_TITLE = "title";
    public static final String SONG_ARTIST = "artist";
    public static final String SONG_ALBUM = "album";
    public static final String SONG_YEAR = "year";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static final String[] ALL_COLUMNS =
            {SONG_PATH, SONG_TITLE, SONG_ARTIST, SONG_ALBUM, SONG_YEAR};

    private static final String CREATE_TABLE =
            String.format("CREATE TABLE %s (%s TEXT PRIMARY KEY, %s TEXT, %s TEXT, %s TEXT, %s TEXT)",
                    TABLE_LIBRARY, SONG_PATH, SONG_TITLE, SONG_ARTIST, SONG_ALBUM, SONG_YEAR);

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_LIBRARY);
        onCreate(sqLiteDatabase);
    }
}
