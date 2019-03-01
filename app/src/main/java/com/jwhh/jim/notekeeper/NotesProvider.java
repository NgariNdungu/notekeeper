package com.jwhh.jim.notekeeper;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

import com.jwhh.jim.notekeeper.NoteKeeperDatabaseContract.courseInfoEntry;
import com.jwhh.jim.notekeeper.NoteKeeperDatabaseContract.noteInfoEntry;
import com.jwhh.jim.notekeeper.NoteKeeperProviderContract.Courses;
import com.jwhh.jim.notekeeper.NoteKeeperProviderContract.Notes;

public class NotesProvider extends ContentProvider {
    public static final String MIME_VENDOR_TYPE = "vnd." + NoteKeeperProviderContract.AUTHORITY +
            ".";
    // should contain all that is needed to perform db operations
    SQLiteOpenHelper mDbOpenHelper;
    private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public static final int COURSES = 0;

    public static final int NOTES = 1;

    public static final int NOTES_EXPANDED = 2;

    public static final int NOTES_ROW = 3;

    static {
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Courses.PATH, COURSES);
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Notes.PATH, NOTES);
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Notes.PATH_EXPANDED, NOTES_EXPANDED);
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Notes.PATH + "/#", NOTES_ROW);
    }

    public NotesProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int deletedRows = -1;
        int uriMatch = sUriMatcher.match(uri);
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        switch (uriMatch) {
            case NOTES:
                deletedRows = db.delete(noteInfoEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case COURSES:
                deletedRows = db.delete(courseInfoEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case NOTES_ROW:
                String noteSelection = noteInfoEntry._ID + "=?";
                String[] selectionId = {String.valueOf(ContentUris.parseId(uri))};
                deletedRows = db.delete(noteInfoEntry.TABLE_NAME, noteSelection, selectionId);
                break;
            case NOTES_EXPANDED:
                throw new UnsupportedOperationException("Delete not supported, read only table");
        }

        return deletedRows;
    }

    @Override
    public String getType(Uri uri) {
        String mimeType = null;
        int uriMatch = sUriMatcher.match(uri);

        switch (uriMatch) {
            case NOTES:
                mimeType = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                        MIME_VENDOR_TYPE + Notes.PATH;
                break;
            case COURSES:
                mimeType = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                        MIME_VENDOR_TYPE + Courses.PATH;
                break;
            case NOTES_EXPANDED:
                mimeType = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                        MIME_VENDOR_TYPE + Notes.PATH_EXPANDED;
                break;
            case NOTES_ROW:
                mimeType = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                        MIME_VENDOR_TYPE + Notes.PATH;
        }
        return mimeType;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        long rowId = -1;
        Uri rowUri = null;
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                rowId = db.insert(noteInfoEntry.TABLE_NAME, null, values);
                rowUri = ContentUris.withAppendedId(Notes.CONTENT_URI, rowId);
                break;
            case COURSES:
                rowId = db.insert(courseInfoEntry.TABLE_NAME, null, values);
                rowUri = ContentUris.withAppendedId(Courses.CONTENT_URI, rowId);
                break;
            case NOTES_EXPANDED:
                throw new UnsupportedOperationException("Cannot insert, table is read only");
        }
        return rowUri;
    }

    @Override
    public boolean onCreate() {
        mDbOpenHelper = new NoteKeeperOpenHelper(getContext());

        // indicates if provider has been created successfully
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        switch (sUriMatcher.match(uri)) {
            case COURSES:
                cursor = db.query(courseInfoEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case NOTES:
                cursor = db.query(noteInfoEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case NOTES_EXPANDED:
                cursor = notesExpandedQuery(db, projection, selection, selectionArgs,sortOrder);
                break;
            case NOTES_ROW:
                String selectRow = noteInfoEntry._ID + "=?";
                String[] args = {String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(noteInfoEntry.TABLE_NAME, projection, selectRow, args,
                        null, null, null);
                break;
        }

        return cursor;
    }

    private Cursor notesExpandedQuery(SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String tablesWithJoin = noteInfoEntry.TABLE_NAME + " JOIN " + courseInfoEntry.TABLE_NAME +
                " ON " + noteInfoEntry.getQName(noteInfoEntry.COLUMN_COURSE_ID) +
                " = " + courseInfoEntry.getQName(courseInfoEntry.COLUMN_COURSE_ID);

        String[] columns = new String[projection.length];
        for (int idx = 0; idx < projection.length; idx++){
            columns[idx] = projection[idx].equals(BaseColumns._ID) ||
                    projection[idx].equals(courseInfoEntry.COLUMN_COURSE_ID) ?
                    noteInfoEntry.getQName("_id"): projection[idx];
        }
        return db.query(tablesWithJoin, columns, selection, selectionArgs,
                null, null, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        int updatedRows = -1;
        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case NOTES:
                updatedRows = db.update(noteInfoEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case COURSES:
                updatedRows = db.update(courseInfoEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case NOTES_ROW:
                String noteSelection = noteInfoEntry._ID + "=?";
                String[] noteIdArg = {String.valueOf(ContentUris.parseId(uri))};
                updatedRows = db.update(noteInfoEntry.TABLE_NAME, values, noteSelection, noteIdArg);
                break;
            case NOTES_EXPANDED:
                throw new UnsupportedOperationException("Update not supported, read only table");
        }
        return updatedRows;
    }
}
