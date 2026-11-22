package com.csm.mushafi.data;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * الإصدار 2 أضاف فهرس البحث والعلامات المرجعية.
 * الترقية تحذف قاعدة الإصدار 1، وهي نسخ مستوردة من الملف وليس فيها بيانات يدوية.
 */
@Database(
        entities = {SurahEntity.class, AyahEntity.class, SearchEntity.class, BookmarkEntity.class},
        version = 2,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract QuranDao quranDao();

    public static AppDatabase get(android.content.Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(), AppDatabase.class, "mushafi.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
