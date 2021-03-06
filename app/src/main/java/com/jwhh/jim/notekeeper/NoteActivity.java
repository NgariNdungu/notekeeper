package com.jwhh.jim.notekeeper;

import android.annotation.SuppressLint;
import android.app.LoaderManager;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.jwhh.jim.notekeeper.NoteKeeperDatabaseContract.courseInfoEntry;
import com.jwhh.jim.notekeeper.NoteKeeperDatabaseContract.noteInfoEntry;
import com.jwhh.jim.notekeeper.NoteKeeperProviderContract.Courses;
import com.jwhh.jim.notekeeper.NoteKeeperProviderContract.Notes;

import java.net.URI;
import java.util.List;

public class NoteActivity extends android.support.v7.app.AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final int LOADER_NOTES = 0;
    public static final int LOADER_COURSES = 1;
    private final String TAG = getClass().getSimpleName();
    public static final String NOTE_ID = "com.jwhh.jim.notekeeper.NOTE_ID";
    public static final String ORIGINAL_NOTE_COURSE_ID = "com.jwhh.jim.notekeeper.ORIGINAL_NOTE_COURSE_ID";
    public static final String ORIGINAL_NOTE_TITLE = "com.jwhh.jim.notekeeper.ORIGINAL_NOTE_TITLE";
    public static final String ORIGINAL_NOTE_TEXT = "com.jwhh.jim.notekeeper.ORIGINAL_NOTE_TEXT";
    public static final int ID_NOT_SET = -1;
    private NoteInfo mNote;
    private boolean mIsNewNote;
    private Spinner mSpinnerCourses;
    private EditText mTextNoteTitle;
    private EditText mTextNoteText;
    private int mNoteId;
    private boolean mIsCancelling;
    private String mOriginalNoteCourseId;
    private String mOriginalNoteTitle;
    private String mOriginalNoteText;
    private NoteKeeperOpenHelper dbOpenHelper;
    private Cursor noteCursor;
    private int courseIdPos;
    private int noteTitlePos;
    private int noteTextPos;
    private SimpleCursorAdapter adapterCourses;
    private boolean mcourseQueryFinished;
    private boolean mNotesQueryFinished;
    private Uri mNoteUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbOpenHelper = new NoteKeeperOpenHelper(this);
        mSpinnerCourses = (Spinner) findViewById(R.id.spinner_courses);

        adapterCourses = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
                new String[]{courseInfoEntry.COLUMN_COURSE_TITLE},
                new int[]{android.R.id.text1}, 1);
        adapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCourses.setAdapter(adapterCourses);
        getLoaderManager().initLoader(LOADER_COURSES,null,this);
        // get id from intent
        readDisplayStateValues();
        if(savedInstanceState == null) {
//            saveOriginalNoteValues();
        } else {
            restoreOriginalNoteValues(savedInstanceState);
        }

        mTextNoteTitle = (EditText) findViewById(R.id.text_note_title);
        mTextNoteText = (EditText) findViewById(R.id.text_note_text);

        if(!mIsNewNote)
            getLoaderManager().initLoader(LOADER_NOTES, null, this);
        Log.d(TAG, "onCreate");
    }

    private void loadCourseData() {
        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
        String[] courseColumns = {
                courseInfoEntry.COLUMN_COURSE_TITLE,
                courseInfoEntry.COLUMN_COURSE_ID,
                courseInfoEntry._ID
        };

        Cursor courseCursor = database.query(courseInfoEntry.TABLE_NAME, courseColumns,
                null,null,null,null,courseInfoEntry.COLUMN_COURSE_TITLE);
        adapterCourses.changeCursor(courseCursor);
    }

    @Override
    protected void onDestroy() {
        dbOpenHelper.close();
        super.onDestroy();
    }

    private void loadNoteData() {
        // load note from database and set member variables
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();

//        String titleStart = "dynamic";

//        String selection = noteInfoEntry.COLUMN_COURSE_ID + " = ? AND " +

        String selection = noteInfoEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(mNoteId)};

        String[] noteColumns = {
                noteInfoEntry.COLUMN_NOTE_TITLE,
                noteInfoEntry.COLUMN_COURSE_ID,
                noteInfoEntry.COLUMN_NOTE_TEXT
        };
        noteCursor = db.query(noteInfoEntry.TABLE_NAME, noteColumns, selection, selectionArgs,
                null,null,null);
        courseIdPos = noteCursor.getColumnIndex(noteInfoEntry.COLUMN_COURSE_ID);
        noteTitlePos = noteCursor.getColumnIndex(noteInfoEntry.COLUMN_NOTE_TITLE);
        noteTextPos = noteCursor.getColumnIndex(noteInfoEntry.COLUMN_NOTE_TEXT);

        // Don't forget to move to first row
        noteCursor.moveToNext();
        displayNote();
    }

    private void restoreOriginalNoteValues(Bundle savedInstanceState) {
        mOriginalNoteCourseId = savedInstanceState.getString(ORIGINAL_NOTE_COURSE_ID);
        mOriginalNoteTitle = savedInstanceState.getString(ORIGINAL_NOTE_TITLE);
        mOriginalNoteText = savedInstanceState.getString(ORIGINAL_NOTE_TEXT);
    }

    private void saveOriginalNoteValues() {
        if(mIsNewNote)
            return;
        mOriginalNoteCourseId = mNote.getCourse().getCourseId();
        mOriginalNoteTitle = mNote.getTitle();
        mOriginalNoteText = mNote.getText();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mIsCancelling) {
            Log.i(TAG, "Cancelling note at position: " + mNoteId);
            if(mIsNewNote) {
                deleteNoteFromDatabase();
            } else {
//                storePreviousNoteValues();
            }
        } else {
            saveNote();
        }
        Log.d(TAG, "onPause");
    }

    private void deleteNoteFromDatabase() {
        final Uri uri = ContentUris.withAppendedId(Notes.CONTENT_URI, mNoteId);
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                getContentResolver().delete(uri, null, null);
                return null;
            }
        };
        task.execute();

    }

    private void storePreviousNoteValues() {
        CourseInfo course = DataManager.getInstance().getCourse(mOriginalNoteCourseId);
        mNote.setCourse(course);
        mNote.setTitle(mOriginalNoteTitle);
        mNote.setText(mOriginalNoteText);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ORIGINAL_NOTE_COURSE_ID, mOriginalNoteCourseId);
        outState.putString(ORIGINAL_NOTE_TITLE, mOriginalNoteTitle);
        outState.putString(ORIGINAL_NOTE_TEXT, mOriginalNoteText);
    }

    private void saveNote() {
        String courseId = selectedCourseId();
        String noteTitle = mTextNoteTitle.getText().toString();
        String noteText = mTextNoteText.getText().toString();
        saveNoteToDatabase(courseId, noteTitle, noteText);
    }

    private String selectedCourseId() {
        int selectedPosition = mSpinnerCourses.getSelectedItemPosition();
        Cursor cursor = adapterCourses.getCursor();
        cursor.moveToPosition(selectedPosition);
        int courseIdPos = cursor.getColumnIndex(courseInfoEntry.COLUMN_COURSE_ID);
        return cursor.getString(courseIdPos);
    }

    private void saveNoteToDatabase(String courseId, String noteTitle, String noteText) {
        ContentValues values = new ContentValues();
        values.put(courseInfoEntry.COLUMN_COURSE_ID, courseId);
        values.put(noteInfoEntry.COLUMN_NOTE_TITLE, noteTitle);
        values.put(noteInfoEntry.COLUMN_NOTE_TEXT, noteText);

        Uri uri = ContentUris.withAppendedId(Notes.CONTENT_URI, mNoteId);
        getContentResolver().update(uri, values, null, null);
    }

    private void displayNote() {
        String courseId = noteCursor.getString(courseIdPos);
        String noteTitle = noteCursor.getString(noteTitlePos);
        String noteText = noteCursor.getString(noteTextPos);
        int courseIndex = getIndexOfCourseId(courseId);
        mSpinnerCourses.setSelection(courseIndex);
        mTextNoteTitle.setText(noteTitle);
        mTextNoteText.setText(noteText);
    }

    private int getIndexOfCourseId(String courseId) {
        Cursor cursor = adapterCourses.getCursor();
        int courseRowIndex = 0;
        if (cursor != null) {
            int courseIdPos = cursor.getColumnIndex(courseInfoEntry.COLUMN_COURSE_ID);

            boolean more = cursor.moveToFirst();
            while (more) {
                String cursorCourseId = cursor.getString(courseIdPos);
                if (courseId.equals(cursorCourseId)){
                    break;
                }

                courseRowIndex++;
                more = cursor.moveToNext();
            }

        }
        return  courseRowIndex;
    }

    private void readDisplayStateValues() {
        Intent intent = getIntent();
        mNoteId = intent.getIntExtra(NOTE_ID, ID_NOT_SET);
        mIsNewNote = mNoteId == ID_NOT_SET;
        if(mIsNewNote) {
            createNewNote();
        }

        Log.i(TAG, "mNoteId: " + mNoteId);
//        mNote = DataManager.getInstance().getNotes().get(mNoteId);

    }

    private void createNewNote() {
        ContentValues values = new ContentValues();
        values.put(Notes.COURSE_ID, "");
        values.put(Notes.NOTE_TITLE, "");
        values.put(Notes.NOTE_TEXT, "");

        Uri rowUri = getContentResolver().insert(Notes.CONTENT_URI, values);

        mNoteId = (int) ContentUris.parseId(rowUri);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        } else if (id == R.id.action_cancel) {
            mIsCancelling = true;
            finish();
        } else if(id == R.id.action_next) {
            moveNext();
        } else if(id == R.id.action_set_reminder) {
            setReminderNotification();
        }

        return super.onOptionsItemSelected(item);
    }

    private void setReminderNotification() {
        String noteText = String.valueOf(mTextNoteText.getText());
        String noteTitle = mTextNoteTitle.getText().toString();
        int noteId = (int) ContentUris.parseId(mNoteUri);
        NoteReminderNotification.notify(this, noteText, noteTitle, noteId);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_next);
        int lastNoteIndex = DataManager.getInstance().getNotes().size() - 1;
        item.setEnabled(mNoteId < lastNoteIndex);
        return super.onPrepareOptionsMenu(menu);
    }

    private void moveNext() {
        saveNote();

        ++mNoteId;
        mNote = DataManager.getInstance().getNotes().get(mNoteId);

        saveOriginalNoteValues();
        displayNote();
        invalidateOptionsMenu();
    }

    private void sendEmail() {
        CourseInfo course = (CourseInfo) mSpinnerCourses.getSelectedItem();
        String subject = mTextNoteTitle.getText().toString();
        String text = "Checkout what I learned in the Pluralsight course \"" +
                course.getTitle() +"\"\n" + mTextNoteText.getText().toString();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc2822");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = null;
        if (id == LOADER_NOTES)
            loader = createLoaderNotes();

        if (id == LOADER_COURSES)
            loader = createLoaderCourses();
        return loader;
    }

    @SuppressLint("StaticFieldLeak")
    private CursorLoader createLoaderCourses() {
        mcourseQueryFinished = false;
        Uri uri = Courses.CONTENT_URI;
        String[] courseColumns = {
                Courses.COURSE_TITLE,
                Courses.COURSE_ID,
                Courses._ID
        };
        return new CursorLoader(this, uri, courseColumns, null, null, Courses.COURSE_TITLE);
    }

    @SuppressLint("StaticFieldLeak")
    private CursorLoader createLoaderNotes() {
        mNotesQueryFinished = false;
        mNoteUri = ContentUris.withAppendedId(Notes.CONTENT_URI, mNoteId);
        String[] noteColumns = {
                Notes.NOTE_TITLE,
                Notes.COURSE_ID,
                Notes.NOTE_TEXT
        };

        return new CursorLoader(this, mNoteUri, noteColumns,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_NOTES) {
            loadFinishedNotes(data);
        } else if (loader.getId() == LOADER_COURSES) {
            adapterCourses.changeCursor(data);
            mcourseQueryFinished = true;
            displayNoteIfQueryFinished();
        }
    }

    private void loadFinishedNotes(Cursor data) {
        noteCursor = data;
        courseIdPos = noteCursor.getColumnIndex(noteInfoEntry.COLUMN_COURSE_ID);
        noteTitlePos = noteCursor.getColumnIndex(noteInfoEntry.COLUMN_NOTE_TITLE);
        noteTextPos = noteCursor.getColumnIndex(noteInfoEntry.COLUMN_NOTE_TEXT);

        // Don't forget to move to first row
        noteCursor.moveToNext();
        mNotesQueryFinished = true;
        displayNoteIfQueryFinished();
    }

    private void displayNoteIfQueryFinished() {
        if (mNotesQueryFinished && mcourseQueryFinished)
            displayNote();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // close the cursor
        if (loader.getId() == LOADER_NOTES) {
            if (noteCursor != null) {
                noteCursor.close();
            }
        } else if (loader.getId() == LOADER_COURSES)
            adapterCourses.changeCursor(null);

    }
}












