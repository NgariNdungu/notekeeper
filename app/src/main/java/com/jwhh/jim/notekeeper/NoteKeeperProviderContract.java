package com.jwhh.jim.notekeeper;

import android.net.Uri;
import android.provider.BaseColumns;

public final class NoteKeeperProviderContract {
    private NoteKeeperProviderContract(){}
    public static final String AUTHORITY = "com.jwhh.jim.notekeeper.provider";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    protected interface CourseIdColumns {
        public static final String COURSE_ID = "course_id";
    }

    protected interface CoursesColumns {
        public static final String COURSE_TITLE = "course_title";
    }

    protected interface NotesColumns {
        public static final String NOTE_TITLE = "note_title";
        public static final String NOTE_TEXT = "note_text";
    }
    public static final class Courses implements BaseColumns, CoursesColumns, CourseIdColumns{
        public static final String PATH = "courses";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);
    }

    public static final class Notes implements BaseColumns, NotesColumns, CourseIdColumns, CoursesColumns{
        public static final String PATH = "notes";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);
        public static final String PATH_EXPANDED = "notes_expanded";
        public static final Uri CONTENT_EXPANDED_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH_EXPANDED);
    }
}
