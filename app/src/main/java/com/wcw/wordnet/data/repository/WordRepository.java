package com.wcw.wordnet.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.wcw.wordnet.data.algorithm.Sm2Algorithm;
import com.wcw.wordnet.data.local.dao.ReviewQueueDao;
import com.wcw.wordnet.data.local.dao.WordDao;
import com.wcw.wordnet.data.local.database.AppDatabase;
import com.wcw.wordnet.model.RootStatistic;
import com.wcw.wordnet.model.entity.ReviewQueue;
import com.wcw.wordnet.model.entity.WordNode;

import java.util.List;

import io.reactivex.Completable;    // 异步操作完成状态
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 单词仓库类
 * 作为单一数据源，封装所有数据操作
 * 提供线程安全的异步接口，供ViewModel使用
 */

public class WordRepository {

    private final WordDao wordDao;

    private final ReviewQueueDao reviewQueueDao;  // 复习队列DAO
    private final Sm2Algorithm sm2Algorithm = new Sm2Algorithm();  // SM-2算法实例

    private final Application application;

    /**
     * 构造函数
     * @param application 用于获取数据库实例
     */
    public WordRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        this.wordDao = db.wordDao();
        this.reviewQueueDao = db.reviewQueueDao();  // 新增：初始化DAO
        // 新增：初始化复习队列（自动为所有单词创建复习计划）
        initializeReviewQueue();
    }

    /**
     * 插入单词（异步）（并自动加入复习队列）
     * 在后台线程执行，避免阻塞主线程
     * @param word 要插入的单词
     * @return Completable, 用于链式调用和错误处理
     */
    public Completable insertWord(WordNode word){
        return Completable.fromAction(() -> {
                    wordDao.insert(word);  // 先插入单词
                    // ✅ 新增：立即创建复习项
                    ReviewQueue item = sm2Algorithm.createInitialItem(word.getWord());
                    reviewQueueDao.insertReviewQueue(item);
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * 更新单词（异步）
     * @param word 要更新的单词
     * @return Completable
     */
    public Completable updateWord(WordNode word) {
        return Completable.fromAction(() ->
                wordDao.update(word))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 删除单词（异步）
     * @param word 要删除的单词
     * @return Completable
     */
    public Completable deleteWord(WordNode word){
        return Completable.fromAction(() ->
                wordDao.delete(word))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 查询所有可见单词
     * @return LiveData包装的单词列表，自动观察数据变化
     */
    public LiveData<List<WordNode>> getAllActiveWords(){
        return wordDao.getAllActiveWords();
    }

    /**
     * 获取单个单词
     */
    public Single<WordNode> getWordById(String word){
        return wordDao.getWordById(word)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 获取薄弱词（记忆强度最低的10个）
     * @return LiveData包装的薄弱词列表
     */
    public LiveData<List<WordNode>> getWeakWords(){
        return wordDao.getWeakWords(10);    // 默认获取10个
    }

    /**
     * 根据词根搜索相关单词
     * @param root 词根，如"struct"、"re"
     * @return 包含该词根的单词列表LiveData
     */
    public LiveData<List<WordNode>> getWordsByRoot(String root) {
        return wordDao.getWordsByRoot(root);
    }

    /**
     * 获取词根统计信息（用于数据看板）
     * @return 词根统计LiveData
     */
    public LiveData<List<RootStatistic>> getRootStatistics(){
        return wordDao.getRootStatistics();
    }

    /**
     * 归档单词（软删除）
     * @param word 要归档的单词
     * @return Completable
     */
    public Completable archiveWord(String word){
        return Completable.fromAction(() ->
                wordDao.softDelete(word))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 获取单词总数（用于UI显示）
     * @return 单词数量LiveData
     */
    public LiveData<Integer> getWordCount() {
        return wordDao.getWordCount();
    }

    /**
     * 获取已掌握单词数
     * @return 已掌握数量LiveData
     */
    public LiveData<Integer> getMasteredWordCount() {
        return wordDao.getMasteredWordCount();
    }

    /**
     * 初始化复习队列
     * 作用：将所有活跃单词加入复习计划，仅首次运行时执行
     * 线程：在IO线程异步执行，不阻塞主线程
     */
    private void initializeReviewQueue() {
        // ✅ 将 Disposable 赋值给局部变量，消除"未使用"警告
        Disposable initializationTask = Completable.fromAction(() -> {
                    // 1. 先清空旧队列（防止重复插入）
                    reviewQueueDao.deleteAllReviewQueues();

                    // 2. 获取所有活跃单词（需要同步方法）
                    List<WordNode> allWords = wordDao.getAllActiveWordsSync();

                    // 3. 为每个单词创建初始复习项
                    for (WordNode word : allWords) {
                        // 创建初始复习项：立即复习
                        ReviewQueue item = sm2Algorithm.createInitialItem(word.getWord());
                        reviewQueueDao.insertReviewQueue(item);
                    }

                    android.util.Log.d("WordRepository", "✅ 复习队列初始化完成，共添加 " + allWords.size() + " 个单词");
                })
                .subscribeOn(Schedulers.io())  // 在IO线程执行
                .subscribe(
                        () -> android.util.Log.d("WordRepository", "初始化成功"),
                        throwable -> android.util.Log.e("WordRepository", "初始化失败", throwable)
                );

        // 变量 initializationTask 未被使用，但赋值操作消除了 warning
        // 这是一个 fire-and-forget 的一次性任务，无需手动管理
    }

    /**
     * 获取下一个需要复习的单词（自动推送最紧急的）
     * @return LiveData包装的WordNode，自动响应数据变化
     */
    public Single<WordNode> getNextReviewWord() {
        return reviewQueueDao.getNextDueWord(System.currentTimeMillis());
    }

    /**
     * 处理用户复习评分
     * @param wordId 单词ID
     * @param quality 评分（0=忘记, 3=困难, 4=良好, 5=完美）
     * @return Completable，用于ViewModel订阅结果
     */
    public Completable processReview(String wordId, int quality) {
        return Completable.fromAction(() -> {
            // 1. 获取当前复习项
            ReviewQueue item = reviewQueueDao.getReviewItemSync(wordId);
            if (item == null) {
                throw new RuntimeException("未找到复习项: " + wordId);
            }

            // 2. 用SM-2算法计算下次复习时间
            ReviewQueue updatedItem = sm2Algorithm.calculateNextReview(item, quality);

            // 3. 更新到数据库
            reviewQueueDao.updateReviewQueue(updatedItem);

            android.util.Log.d("WordRepository",
                    "单词 '" + wordId + "' 评分: " + quality +
                            ", 下次复习: " + new java.util.Date(updatedItem.getNextReviewTime()));
        }).subscribeOn(Schedulers.io());  // 确保在IO线程执行
    }

    /**
     * 获取待复习单词数量（用于UI显示）
     * ✅ 修改：返回 Single<Integer> 而不是 LiveData<Integer>
     */
    public Single<Integer> getDueReviewCount() {
        return reviewQueueDao.getDueReviewCount(System.currentTimeMillis());
    }

    /**
     * 将新单词加入复习队列（在AddFragment添加单词时调用）
     * @param wordId 新单词的ID
     */
    public Completable addWordToReviewQueue(String wordId) {
        return Completable.fromAction(() -> {
            ReviewQueue item = sm2Algorithm.createInitialItem(wordId);
            reviewQueueDao.insertReviewQueue(item);
        }).subscribeOn(Schedulers.io());
    }

}
