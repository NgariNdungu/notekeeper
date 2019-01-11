package com.jwhh.jim.notekeeper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NoteKeeperOpenHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "NoteKeeper.db";
    public static final int DATABASE_VERSION = 1;

    public NoteKeeperOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /* Create tables and populate initial data */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(NoteKeeperDatabaseContract.courseInfoEntry.SQL_CREATE_TABLE);
        sqLiteDatabase.execSQL(NoteKeeperDatabaseContract.noteInfoEntry.SQL_CREATE_TABLE);

        // populate initial data using DatabaseDataWorker
        DatabaseDataWorker worker = new DatabaseDataWorker(sqLiteDatabase);
        worker.insertCourses();
        worker.insertSampleNotes();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
