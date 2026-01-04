package com.wcw.wordnet.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.wcw.wordnet.model.entity.ReviewQueue;
import com.wcw.wordnet.model.entity.WordNode;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * 复习队列数据访问对象
 */
@Dao
public interface ReviewQueueDao {

    /**
     * 插入单个复习项
     * 如果wordId已存在，则替换（用于更新复习计划）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReviewQueue(ReviewQueue item);

    /**
     * 批量插入复习项（初始化时使用）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReviewQueues(List<ReviewQueue> items);

    /**
     * 更新复习项（修改下次复习时间、间隔、难度因子等）
     */
    @Update
    void updateReviewQueue(ReviewQueue item);

    /**
     * 删除单个复习项（当单词被归档或删除时）
     */
    @Query("DELETE FROM review_queue WHERE wordId = :wordId")
    void deleteReviewQueue(String wordId);

    /**
     * 清空整个复习队列（重置系统时使用）
     */
    @Query("DELETE FROM review_queue")
    void deleteAllReviewQueues();

    /**
     * 根据wordId同步查询复习项（Repository内部使用）
     */
    @Query("SELECT * FROM review_queue WHERE wordId = :wordId LIMIT 1")
    ReviewQueue getReviewItemSync(String wordId);

    /**
     * 获取下一个到期的复习单词（只取1个）
     * 联合查询：review_queue + word_nodes
     * ✅ 修改：返回 Single<WordNode> 而不是 LiveData<WordNode>
     */
    @Query("SELECT w.* FROM word_nodes w " +
            "INNER JOIN review_queue q ON w.word = q.wordId " +
            "WHERE q.next_review_time <= :currentTime " +
            "AND w.isActive = 1 " +
            "ORDER BY q.next_review_time ASC LIMIT 1")
    Maybe<WordNode> getNextDueWord(long currentTime);

    /**
     * 获取多个到期的复习单词（用于批量加载，提高性能）
     * @param limit 限制数量，如10个
     */
    @Query("SELECT w.* FROM word_nodes w " +
            "INNER JOIN review_queue q ON w.word = q.wordId " +
            "WHERE q.next_review_time <= :currentTime " +
            "AND w.isActive = 1 " +
            "ORDER BY q.next_review_time ASC LIMIT :limit")
    LiveData<List<WordNode>> getDueReviewWords(long currentTime, int limit);

    /**
     * 统计当前到期的复习数量（用于显示小红点或进度）
     * ✅ 修改：返回 Single<Integer> 而不是 LiveData<Integer>
     */
    @Query("SELECT COUNT(*) FROM review_queue WHERE next_review_time <= :currentTime")
    Single<Integer> getDueReviewCount(long currentTime);

    /**
     * 查询所有复习项（调试用，生产环境可删除）
     */
    @Query("SELECT * FROM review_queue ORDER BY next_review_time ASC")
    LiveData<List<ReviewQueue>> getAllReviewQueues();

}
