package com.wcw.wordnet.data.local.database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.wcw.wordnet.data.local.dao.MorphemeDao;
import com.wcw.wordnet.data.local.dao.ReviewQueueDao;
import com.wcw.wordnet.data.local.dao.WordDao;
import com.wcw.wordnet.model.entity.MorphemeRelation;
import com.wcw.wordnet.model.entity.ReviewQueue;
import com.wcw.wordnet.model.entity.WordNode;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room数据库主类（AppDatabase）
 * 整个数据库的入口，提供单例模式的数据库实例和DAO访问
 * 版本号：1
 * 包含实体：WordNode（单词表）
 */

@Database(
        entities = {
                WordNode.class,
                ReviewQueue.class,
                MorphemeRelation.class
        },
        version = 4,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * 数据库名称
     * 存储在：/data/data/com.wcw.webnet/databases/wordnet_db
     */
    private static final String DATABASE_NAME = "wordnet_db";

    /**
     * 单利模式
     * volatile 确保线程安全，禁止指令重排序
     */
    private static volatile AppDatabase INSTANCE;   // INSTANCE 是 AppDatabase 类自身的静态成员变量

    /**
     * 线程池：用于数据库的异步操作（初始化、IO）
     * 固定4个线程，避免创建过多线程
     */
    private static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    private static Context appContext; // 新增静态Context字段

    /**
     * 获取DAO实例
     * @return WordDao接口（自动实现）
     */
    public abstract WordDao wordDao();

    public abstract ReviewQueueDao reviewQueueDao();  // 新增DAO
    public abstract MorphemeDao morphemeDao();

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加新列，默认值为空字符串
            database.execSQL("ALTER TABLE word_nodes ADD COLUMN chineseMeaning TEXT NOT NULL DEFAULT ''");
        }
    };

    /**
     * 获取数据库单例
     * 双重检查锁定（Double-Checked Locking）模式，兼顾性能和线程安全
     * @param context 应用上下文
     * @return AppDatabase实例
     */
    public static AppDatabase getDatabase(final Context context){

        // 在首次调用时保存Application Context
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }

        // 第一次检查（无锁，快速路径）
        if (INSTANCE == null) {
            // synchronized 是 Java 的线程同步关键字，同一时间只允许一个线程进入被锁住的代码块
            synchronized (AppDatabase.class) {
                // 第二次检查（有锁，确保只创建一次）
                if (INSTANCE == null){
                    // 创建数据库实例
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class, DATABASE_NAME)
                            // 数据库创建回调
                            .addCallback(roomCallback)
                            .addMigrations(MIGRATION_3_4)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 数据库创建回调
     * 只会在App第一次安装时调用，可用于预填充初始词根数据
     */
    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                WordDao wordDao = INSTANCE.wordDao();
                MorphemeDao morphemeDao = INSTANCE.morphemeDao();
                // ✅ 使用静态存储的appContext，不再从db获取
                if (!DataInitializer.isAlreadyInitialized(wordDao)) {
                    DataInitializer.initialize(appContext, wordDao, morphemeDao).subscribe();
                }
            });
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            // 数据库每次打开时调用
            android.util.Log.d("AppDatabase", "Database opened successfully");
        }
    };

    /**
     * 获取数据库IO线程池
     * 供 Repository 层使用，执行异步操作
     * @return ExecutorService
     */
    public static ExecutorService getDatabaseExecutor(){
        return databaseWriteExecutor;
    }

    /**
     * 关闭数据库（应用退出时调用）
     * 清理资源，避免内存泄漏
     */
    public static void closeDatabase() {
        if (INSTANCE != null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE != null && INSTANCE.isOpen()) {
                    INSTANCE.close();
                    INSTANCE = null;
                }
            }
        }
        // 关闭线程池
        databaseWriteExecutor.shutdown();
    }

}
