package com.wcw.wordnet.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.wcw.wordnet.model.RootStatistic;
import com.wcw.wordnet.model.entity.WordNode;

import java.util.List;

import io.reactivex.Single;

/**
 * 单词数据访问对象
 * 定义对word_nodes表的操作，Room会自动实现这些方法
 */

@Dao
public interface WordDao {
    /**
     * 插入新单词
     * 如果单词已经存在（主键冲突），则替换
     * @param word 要插入的单词实体
     */
    @Insert
    void insert(WordNode word);

    /**
     * 更新单词信息
     * @param word 要更新的单词实体
     */
    @Update
    void update(WordNode word);

    /**
     * 删除单词（硬删除）
     * @param word 要删除的单词实体
     */
    @Delete
    void delete(WordNode word);

    /**
     * 查询所有可见的单词
     * @return 单词列表LiveData
     */
    @Query("SELECT * " +
            "FROM word_nodes " +
            "WHERE isActive = 1 " +
            "ORDER BY memoryStrength ASC")
    LiveData<List<WordNode>> getAllActiveWords();

    /**
     * 获取记忆强度最弱的N个单词，用于复习列表，优先复习最薄弱的单词
     * @param limit 返回的最大数量
     * @return 薄弱词列表 LiveData
     */
    @Query("SELECT * " +
            "FROM word_nodes " +
            "WHERE isActive = 1 " +
            "ORDER BY memoryStrength ASC " +
            "LIMIT :limit")
    LiveData<List<WordNode>> getWeakWords(int limit);

    /**
     * 根据单词精准查询
     * @param word 要查询的单词字符串
     * @return 单词实体的Single
     */
    @Query("SELECT * " +
            "FROM word_nodes " +
            "WHERE word = :word " +
            "LIMIT 1")
    Single<WordNode> getWordById(String word);

    /**
     * 根据词根查询相关单词
     * 在morphemeList(JSON字符串)中模糊匹配词根
     * 示例: 输入"struct"会匹配 ["re","struct","tion"] 和 ["struct","ure"]
     * @param root 词根字符串，如"struct"、"re"
     * @return 包含该词根的单词列表LiveData
     */
    @Query("SELECT * " +
            "FROM word_nodes " +
            "WHERE morphemeList " +
            "LIKE '%' || :root || '%' AND isActive=1")  // ‘||’ 是SQLite的字符串连接符
    LiveData<List<WordNode>> getWordsByRoot(String root);

    /**
     * 统计词根学习情况
     * 返回每个词根对应的单词数量和平均记忆强度
     * 用于数据看板的“词根掌握雷达图”
     * @return 词根统计信息
     */
    @Query("SELECT " +
            "morphemeList, " +
            "COUNT(*) as wordCount, " +
            "AVG(memoryStrength) as avgStrength " +
            "FROM word_nodes " +
            "WHERE isActive=1 " +
            "GROUP BY morphemeList " +
            "ORDER BY wordCount DESC")
    LiveData<List<RootStatistic>> getRootStatistics();

    /**
     * 获取单词总数
     * return 单词数量
     */
    @Query("SELECT COUNT(*) " +
            "FROM word_nodes " +
            "WHERE isActive = 1")
    LiveData<Integer> getWordCount();

    /**
     * 获取已掌握的单词数（memoryStrength >= 0.8）
     * @return 已掌握单词数量
     */
    @Query("SELECT COUNT(*) " +
            "FROM word_nodes " +
            "WHERE isActive=1 AND memoryStrength>0.8")
    LiveData<Integer> getMasteredWordCount();

    /**
     * 软删除，将单词标记为不可见
     * @param word 要归档的单词
     */
    @Query("UPDATE word_nodes " +
            "SET isActive=0 " +
            "WHERE word=:word")
    void softDelete(String word);

    /**
     * 同步获取所有活跃单词（用于Repository初始化）
     * 注意：此方法在IO线程调用，不会阻塞主线程
     */
    @Query("SELECT * FROM word_nodes WHERE isActive = 1")
    List<WordNode> getAllActiveWordsSync();

    /**
     * 同步获取单个单词（Repository内部使用）
     */
    @Query("SELECT * FROM word_nodes WHERE word = :word LIMIT 1")
    WordNode getWordByIdSync(String word);  // ✅ 返回直接的 WordNode，不是 LiveData
}
