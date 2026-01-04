package com.wcw.wordnet;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room数据库主类（AppDatabase）
 * 整个数据库的入口，提供单例模式的数据库实例和DAO访问
 * 版本号：1
 * 包含实体：WordNode（单词表）
 */

@Database(entities = {WordNode.class, ReviewQueue.class}, version = 2, exportSchema = false)
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

    /**
     * 获取DAO实例
     * @return WordDao接口（自动实现）
     */
    public abstract WordDao wordDao();

    public abstract ReviewQueueDao reviewQueueDao();  // 新增DAO

    /**
     * 获取数据库单例
     * 双重检查锁定（Double-Checked Locking）模式，兼顾性能和线程安全
     * @param context 应用上下文
     * @return AppDatabase实例
     */
    public static AppDatabase getDatabase(final Context context){
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
            // 在后台线程预填充数据
            databaseWriteExecutor.execute(() -> {
                // 获取DAO实例
                WordDao dao = INSTANCE.wordDao();

                // 插入6个示范单词
                WordNode[] defaultWords = {
                        new WordNode("reconstruction"),
                        new WordNode("unpredictability"),
                        new WordNode("structure"),
                        new WordNode("dictionary"),
                        new WordNode("preview"),
                        new WordNode("construction")
                };

                // 设置词根
                defaultWords[0].setMorphemeList("[\"re\",\"struct\",\"tion\"]");
                defaultWords[1].setMorphemeList("[\"un\",\"pre\",\"dict\",\"ability\"]");
                defaultWords[2].setMorphemeList("[\"struct\",\"ure\"]");
                defaultWords[3].setMorphemeList("[\"dict\",\"ion\",\"ary\"]");
                defaultWords[4].setMorphemeList("[\"pre\",\"view\"]");
                defaultWords[5].setMorphemeList("[\"con\",\"struct\",\"tion\"]");

                // 插入数据库
                for (WordNode word : defaultWords) {
                    dao.insert(word);
                }

                Log.d("AppDatabase", "默认数据插入完成");
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
