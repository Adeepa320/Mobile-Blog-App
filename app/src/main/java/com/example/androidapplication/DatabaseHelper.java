package com.example.androidapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLiteOpenHelper that manages the local blog_posts table.
 * Provides full CRUD operations used by PostRepository.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "blog.db";
    private static final int    DB_VERSION = 3;

    static final String TABLE   = "blog_posts";
    static final String COL_ID  = "id";
    static final String COL_TITLE    = "title";
    static final String COL_BODY     = "body";
    static final String COL_IMAGE    = "image_uri";
    static final String COL_DATE     = "date";

    private static DatabaseHelper instance;

    /** Singleton — call DatabaseHelper.get(context) everywhere. */
    static synchronized DatabaseHelper get(Context ctx) {
        if (instance == null) instance = new DatabaseHelper(ctx.getApplicationContext());
        return instance;
    }

    private DatabaseHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                COL_ID    + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT NOT NULL, " +
                COL_BODY  + " TEXT NOT NULL, " +
                COL_IMAGE + " TEXT, " +
                COL_DATE  + " TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    /** Insert a new post; returns the assigned id. */
    long insert(BlogPost p) {
        ContentValues cv = toValues(p);
        return getWritableDatabase().insert(TABLE, null, cv);
    }

    /** Update an existing post by id. */
    void update(BlogPost p) {
        getWritableDatabase().update(TABLE, toValues(p),
                COL_ID + "=?", new String[]{String.valueOf(p.id)});
    }

    /** Delete a single post by id. */
    void delete(int id) {
        getWritableDatabase().delete(TABLE,
                COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    /** Delete multiple posts by id list. */
    void deleteAll(List<Integer> ids) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int id : ids)
                db.delete(TABLE, COL_ID + "=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Return all posts ordered newest-first. */
    List<BlogPost> getAll() {
        List<BlogPost> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(
                TABLE, null, null, null, null, null, COL_ID + " DESC");
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    /** Return posts whose title or body contains the query (case-insensitive). */
    List<BlogPost> search(String query) {
        String like = "%" + query + "%";
        Cursor c = getReadableDatabase().query(TABLE, null,
                COL_TITLE + " LIKE ? OR " + COL_BODY + " LIKE ?",
                new String[]{like, like}, null, null, COL_ID + " DESC");
        List<BlogPost> list = new ArrayList<>();
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private ContentValues toValues(BlogPost p) {
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, p.title);
        cv.put(COL_BODY,  p.body);
        cv.put(COL_IMAGE, p.imageUri);
        cv.put(COL_DATE,  p.date);
        return cv;
    }

    private BlogPost fromCursor(Cursor c) {
        return new BlogPost(
                c.getInt(c.getColumnIndexOrThrow(COL_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_TITLE)),
                c.getString(c.getColumnIndexOrThrow(COL_BODY)),
                c.getString(c.getColumnIndexOrThrow(COL_IMAGE)),
                c.getString(c.getColumnIndexOrThrow(COL_DATE)));
    }
}
