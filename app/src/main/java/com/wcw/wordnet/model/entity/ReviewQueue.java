package com.wcw.wordnet.model.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 智能复习队列表
 * 外键关联：wordId → word_nodes.word
 */
@Entity(
        tableName = "review_queue",
        foreignKeys = @ForeignKey(
                entity = WordNode.class,
                parentColumns = "word",
                childColumns = "wordId",
                onDelete = ForeignKey.CASCADE
        )
)
public class ReviewQueue {

    @PrimaryKey
    @NonNull
    private String wordId;

    @ColumnInfo(name = "next_review_time")
    private long nextReviewTime;

    @ColumnInfo(name = "interval_days")
    private int intervalDays = 1;  // 默认间隔1天

    @ColumnInfo(name = "easiness_factor")
    private float easinessFactor = 2.5f;  // SM-2算法默认值

    @ColumnInfo(name = "repetition_count")
    private int repetitionCount = 0;

    @ColumnInfo(name = "review_state")
    private int reviewState = 0;  // 0=待复习(PENDING)

    /**
     * Room要求的无参构造函数
     */
    public ReviewQueue() {}

    /**
     * 快速创建构造函数
     */
    @Ignore
    public ReviewQueue(@NonNull String wordId, long nextReviewTime) {
        this.wordId = wordId;
        this.nextReviewTime = nextReviewTime;
    }

    // --- Getter & Setter ---
    @NonNull
    public String getWordId() { return wordId; }
    public void setWordId(@NonNull String wordId) { this.wordId = wordId; }

    public long getNextReviewTime() { return nextReviewTime; }
    public void setNextReviewTime(long nextReviewTime) { this.nextReviewTime = nextReviewTime; }

    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

    public float getEasinessFactor() { return easinessFactor; }
    public void setEasinessFactor(float easinessFactor) { this.easinessFactor = easinessFactor; }

    public int getRepetitionCount() { return repetitionCount; }
    public void setRepetitionCount(int repetitionCount) { this.repetitionCount = repetitionCount; }

    public int getReviewState() { return reviewState; }
    public void setReviewState(int reviewState) { this.reviewState = reviewState; }

}
