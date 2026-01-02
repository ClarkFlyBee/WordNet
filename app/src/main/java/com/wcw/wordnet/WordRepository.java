package com.wcw.wordnet;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import io.reactivex.Completable;    // 异步操作完成状态
import io.reactivex.schedulers.Schedulers;

/**
 * 单词仓库类
 * 作为单一数据源，封装所有数据操作
 * 提供线程安全的异步接口，供ViewModel使用
 */

public class WordRepository {

    private final WordDao wordDao;
    private final Application application;

    /**
     * 构造函数
     * @param application 用于获取数据库实例
     */
    public WordRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        this.wordDao = db.wordDao();
    }

    /**
     * 插入单词（异步）
     * 在后台线程执行，避免阻塞主线程
     * @param word 要插入的单词
     * @return Completable, 用于链式调用和错误处理
     */
    public Completable insertWord(WordNode word){
        return Completable.fromAction(() ->
                wordDao.insert(word))
                .subscribeOn(Schedulers.io());  // 强制在IO线程执行
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
    public LiveData<List<WordNode>> getWordByRoot(String root) {
        return wordDao.getWordByRoot(root);
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

}
