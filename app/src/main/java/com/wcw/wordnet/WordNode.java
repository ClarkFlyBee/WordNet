package com.wcw.wordnet;


import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 单词节点实体类
 */

@Entity(tableName = "word_nodes")   // 指定数据库表名为 word_nodes
public class WordNode {

    /**
     * 单词本身
     * 主键，确保不重复
     */
    @PrimaryKey
    @NonNull
    private String word;

    /**
     * 记忆强度（0.0-1.0）
     * 0.0 = 完全遗忘；1.0 = 永久掌握
     */
    private float memoryStrength;

    /**
     * 上次复习时间戳（毫秒）
     * System.currentTimeMillis()
     */
    private long lastReviewed;

    /**
     * 复习次数
     */
    private int reviewCount;

    /**
     * 是否在背单词列表中
     * true = 正在学习；false = 已归档/删除
     */
    private boolean isActive;

    /**
     * 词根词缀列表（JSON格式）
     * 避免复杂的多表关联查询
     */
    private String morphemeList;

    /**
     * Room要求实体类包含无参构造函数
     */
    public WordNode(){
        this.memoryStrength = 0.0f;
        this.lastReviewed = System.currentTimeMillis();
        this.reviewCount = 0;
        this.isActive = true;
        this.morphemeList = "[]";
    }

    public WordNode(
            @NonNull String word,
            float memoryStrength,
            long lastReviewed,
            int reviewCount,
            boolean isActive,
            String morphemeList)
    {
        this.word = word;
        this.memoryStrength = memoryStrength;
        this.lastReviewed = lastReviewed;
        this.reviewCount = reviewCount;
        this.isActive = isActive;
        this.morphemeList = morphemeList;
    }

    /**
     * 用于创建单词，其他属性均赋予默认值
     */
    public WordNode(@NonNull String word) {
        this.word = word;
        this.memoryStrength = 0.0f;
        this.lastReviewed = System.currentTimeMillis();
        this.reviewCount = 0;
        this.isActive = true;
        this.morphemeList = "[]";
    }

    @NonNull
    public String getWord() {
        return word;
    }

    public void setWord(@NonNull String word) {
        this.word = word;
    }

    public float getMemoryStrength() {
        return memoryStrength;
    }

    public void setMemoryStrength(float memoryStrength) {
        // 边界保护
        this.memoryStrength = Math.max(0.0f, Math.min(1.0f, memoryStrength));
    }

    public long getLastReviewed() {
        return lastReviewed;
    }

    public void setLastReviewed(long lastReviewed) {
        this.lastReviewed = lastReviewed;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getMorphemeList() {
        return morphemeList;
    }

    public void setMorphemeList(String morphemeList) {
        this.morphemeList = morphemeList;
    }

    /**
     * 判断单词是否已经掌握
     */
    public boolean isMastered(){
        return memoryStrength >= 0.8f;
    }

    /**
     * 计算下次应该复习的时间
     * 模拟艾宾浩斯遗忘曲线，复习次数越多，记忆强度越高，遗忘总是先快后慢
     */
    public long getNextReviewTime(){
        // 复习间隔基数：1、2、4、8、15、……
        int baseInterval = reviewCount < 5 ? (int)Math.pow(2, reviewCount) : 15;
        // 记得越牢，间隔越长
        double modifier = 1.0 + memoryStrength * 2.0; // 1.0 ~ 3.0 倍
        long intervalMillis = (long)(baseInterval * modifier * 24 * 60 * 60 * 1000);
        return lastReviewed + intervalMillis;
    }

    /**
     * 更新记忆强度
     */
    public void updateMemoryStrength(boolean isCorrect){
        reviewCount++;
        float score = isCorrect ? 0.3f : -0.1f;
        memoryStrength = memoryStrength * 0.7f + score * 0.3f;
        // 边界保护
        memoryStrength = Math.max(0.0f, Math.min(1.0f, memoryStrength));
    }

    @NonNull
    @Override
    public String toString() {
        return "WordNode{" +
                "word='" + word + "'" +
                ", memoryStrength=" + memoryStrength +
                ", reviewCount=" + reviewCount +
                ", isActive=" + isActive +
                "}";
    }

}
