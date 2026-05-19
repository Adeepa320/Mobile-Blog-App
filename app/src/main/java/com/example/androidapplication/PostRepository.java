package com.example.androidapplication;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Thin facade over DatabaseHelper.
 * All callers use PostRepository.init(context) once (in MainActivity.onCreate),
 * then the static helpers everywhere else.
 */
public class PostRepository {

    private static DatabaseHelper db;

    /** Must be called once before any other method (MainActivity.onCreate). */
    public static void init(Context ctx) {
        if (db != null) return;
        db = DatabaseHelper.get(ctx);
        // Seed sample posts only when the database is empty
        if (db.getAll().isEmpty()) seed();
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    public static List<BlogPost> getAll()                  { return db.getAll(); }
    public static List<BlogPost> search(String q)          { return db.search(q); }

    public static void add(BlogPost p) {
        long id = db.insert(p);
        p.id = (int) id;
    }

    public static void update(BlogPost p)                  { db.update(p); }
    public static void delete(int id)                      { db.delete(id); }
    public static void deleteAll(List<Integer> ids)        { db.deleteAll(ids); }

    // ── helpers ─────────────────────────────────────────────────────────────

    public static String today() {
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
    }

    // ── seed ────────────────────────────────────────────────────────────────

    private static void seed() {
        db.insert(new BlogPost(0, "The Future of AI and Machine Learning",
            "Artificial Intelligence and Machine Learning are rapidly transforming the way we " +
            "live and work. From autonomous vehicles to personalised recommendations, AI is " +
            "becoming an integral part of our daily lives.\n\nRecent breakthroughs in deep " +
            "learning have enabled machines to perform tasks once thought exclusively human. " +
            "Natural language processing, computer vision, and reinforcement learning are just " +
            "a few areas where we're seeing remarkable progress.\n\nAs we move forward, it's " +
            "crucial to consider the ethical implications of AI development.",
            null, "May 8, 2026"));
        db.insert(new BlogPost(0, "Exploring Hidden Gems: A Travel Guide",
            "Discover breathtaking destinations off the beaten path. These hidden gems offer " +
            "stunning landscapes and unforgettable experiences for every traveller.\n\nFrom the " +
            "misty mountains of northern Patagonia to the ancient temples of rural Cambodia, " +
            "the world is full of places that rarely make mainstream travel lists.",
            null, "May 7, 2026"));
        db.insert(new BlogPost(0, "Healthy Habits for a Productive Life",
            "Small daily habits can lead to massive improvements in your health, focus, and " +
            "overall well-being. Start with these simple routines today.\n\nMorning exercise, " +
            "even just a 20-minute walk, sets a positive tone for the entire day. Consistency " +
            "is the key — progress compounds over time.",
            null, "May 6, 2026"));
    }
}
