package com.wcw.wordnet.model.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 词根关系表
 * 存储单词与词根的关联关系，用于构建词根网络
 */
@Entity(
        tableName = "morpheme_relations",
        foreignKeys = {
                @ForeignKey(
                        entity = WordNode.class,
                        parentColumns = "word",
                        childColumns = "wordId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "wordId"),
                @Index(value = "morpheme")
        }
)
public class MorphemeRelation {
    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * 词根字符串，如 "struct", "re", "pre"
     */
    private String morpheme;

    /**
     * 单词ID（外键关联 word_nodes.word）
     */
    private String wordId;

    /**
     * 词根位置：0=前缀, 1=词根, 2=后缀
     */
    private int position;

    public MorphemeRelation(String morpheme, String wordId, int position) {
        this.morpheme = morpheme;
        this.wordId = wordId;
        this.position = position;
    }

    // Getter and Setter
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getMorpheme() { return morpheme; }
    public void setMorpheme(String morpheme) { this.morpheme = morpheme; }

    public String getWordId() { return wordId; }
    public void setWordId(String wordId) { this.wordId = wordId; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}