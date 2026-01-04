package com.wcw.wordnet.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.wcw.wordnet.model.entity.MorphemeRelation;
import com.wcw.wordnet.model.entity.WordNode;

import java.util.List;

/**
 * 词根关系数据访问对象
 */
@Dao
public interface MorphemeDao {

    /**
     * 插入词根关系
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MorphemeRelation relation);

    /**
     * 批量插入
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MorphemeRelation> relations);

    /**
     * 获取单词的所有词根关系
     */
    @Query("SELECT * FROM morpheme_relations WHERE wordId = :wordId ORDER BY position ASC")
    LiveData<List<MorphemeRelation>> getRelationsByWord(String wordId);

    /**
     * 获取词根的所有相关单词
     */
    @Query("SELECT w.* FROM word_nodes w " +
            "INNER JOIN morpheme_relations mr ON w.word = mr.wordId " +
            "WHERE mr.morpheme = :morpheme AND w.isActive = 1")
    LiveData<List<WordNode>> getWordsByMorpheme(String morpheme);

    /**
     * 获取所有词根（去重）
     */
    @Query("SELECT DISTINCT morpheme FROM morpheme_relations ORDER BY morpheme ASC")
    LiveData<List<String>> getAllMorphemes();

    /**
     * 删除单词的所有词根关系（当单词被删除时）
     */
    @Query("DELETE FROM morpheme_relations WHERE wordId = :wordId")
    void deleteByWordId(String wordId);
}