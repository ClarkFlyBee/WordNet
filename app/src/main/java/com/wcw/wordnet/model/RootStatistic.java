package com.wcw.wordnet.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/**
 * 词根统计信息
 * 作为数据库查询结果的映射
 */

public class RootStatistic {

    @ColumnInfo(name = "morphemeList")
    private String morphemeList;

    @ColumnInfo(name = "wordCount")
    private int wordCount;

    @ColumnInfo(name = "avgStrength")
    private float avgStrength;

    public RootStatistic(String morphemeList, int wordCount, float avgStrength) {
        this.morphemeList = morphemeList;
        this.wordCount = wordCount;
        this.avgStrength = avgStrength;
    }

    public String getMorphemeList() {
        return morphemeList;
    }

    public int getWordCount() {
        return wordCount;
    }

    public float getAvgStrength() {
        return avgStrength;
    }

    /**
     * 获取主词根（第一个词根）
     * 示例: ["re","struct","tion"] → "["re"" → "re"
     */
    public String getPrimaryRoot() {
        if (morphemeList == null || morphemeList.isEmpty()) {
            return "unknown";
        }
        return morphemeList.split(",")[0].replaceAll("[\"\\[\\]]", "");
    }

    @NonNull
    @Override
    public String toString(){
        return String.format("词根: %s, 单词数: %d, 平均掌握度: %.2f", getPrimaryRoot(), wordCount, avgStrength);
    }

}
