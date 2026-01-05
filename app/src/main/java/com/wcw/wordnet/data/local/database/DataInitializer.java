package com.wcw.wordnet.data.local.database;

import android.content.Context;
import android.util.Log;

import com.wcw.wordnet.R;
import com.wcw.wordnet.data.local.dao.MorphemeDao;
import com.wcw.wordnet.data.local.dao.WordDao;
import com.wcw.wordnet.model.entity.MorphemeRelation;
import com.wcw.wordnet.model.entity.WordNode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

/**
 * 默认数据初始化器
 * 职责：从JSON资源文件加载示范单词，支持热更新
 */
public class DataInitializer {

    private static final String TAG = "DataInitializer";
    private static final int DEFAULT_RESOURCE_ID = R.raw.default_words; // JSON文件名

    public static Completable initialize(Context context, WordDao wordDao, MorphemeDao morphemeDao) {
        return Completable.fromAction(() -> {
            try {
                // 1. 读取JSON文件
                String jsonString = readJsonFromResource(context, DEFAULT_RESOURCE_ID);
                JSONObject jsonObject = new JSONObject(jsonString);
                JSONArray wordsArray = jsonObject.getJSONArray("words");

                // 2. 解析并插入
                for (int i = 0; i < wordsArray.length(); i++) {
                    JSONObject wordObj = wordsArray.getJSONObject(i);

                    // 创建单词实体
                    WordNode wordNode = new WordNode(wordObj.getString("word"));
                    wordNode.setChineseMeaning(wordObj.getString("chinese")); // ✅ 设置中文

                    // 解析词根数组
                    JSONArray morphemeArray = wordObj.getJSONArray("morphemes");
                    List<String> morphemes = new ArrayList<>();
                    for (int j = 0; j < morphemeArray.length(); j++) {
                        morphemes.add(morphemeArray.getString(j));
                    }

                    // 转换为JSON字符串存储
                    wordNode.setMorphemeList(morphemes.toString()
                            .replace("[", "[\"")
                            .replace("]", "\"]")
                            .replace(", ", "\",\""));

                    // 插入单词
                    wordDao.insert(wordNode);

                    // 插入词根关系
                    List<MorphemeRelation> relations = wordNode.parseMorphemeRelations();
                    morphemeDao.insertAll(relations);
                }

                Log.d(TAG, "✅ 默认数据初始化完成，共 " + wordsArray.length() + " 个单词");

            } catch (Exception e) {
                Log.e(TAG, "❌ 数据初始化失败", e);
                throw new RuntimeException(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 从资源文件读取JSON字符串
     */
    private static String readJsonFromResource(Context context, int resourceId) throws Exception {
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        reader.close();
        return stringBuilder.toString();
    }

    /**
     * 检查是否已初始化（防止重复插入）
     */
    public static boolean isAlreadyInitialized(WordDao wordDao) {
        return wordDao.getWordCountSync() > 0;
    }
}