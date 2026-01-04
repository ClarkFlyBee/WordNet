package com.wcw.wordnet.ui.worddetail;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.wcw.wordnet.databinding.ActivityWordDetailBinding;
import com.wcw.wordnet.model.entity.MorphemeRelation;
import com.wcw.wordnet.model.entity.WordNode;
import com.wcw.wordnet.ui.WordGraphViewModel;

import java.util.ArrayList;

/**
 * 单词详情页面
 * 展示词根网络可视化
 */
public class WordDetailActivity extends AppCompatActivity {
    private ActivityWordDetailBinding binding;
    private WordGraphViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWordDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 获取ViewModel
        viewModel = new ViewModelProvider(this).get(WordGraphViewModel.class);

        // 获取传递的单词
        String word = getIntent().getStringExtra("WORD");
        if (word == null) {
            finish();
            return;
        }

        // 加载单词信息和词根关系
        loadWordDetail(word);
    }

    private void loadWordDetail(String word) {
        // 观察单词信息
        viewModel.getWordById(word).observe(this, wordNode -> {
            if (wordNode != null) {
                binding.tvWord.setText(wordNode.getWord());
                binding.tvMorphemes.setText(wordNode.getMorphemeList());
                binding.tvMemoryStrength.setText(String.format("掌握度: %.0f%%", wordNode.getMemoryStrength() * 100));

                // 加载词根关系
                loadMorphemeGraph(wordNode);
            }
        });
    }

    private void loadMorphemeGraph(WordNode wordNode) {
        // 解析词根关系
        ArrayList<MorphemeRelation> relations = new ArrayList<>(wordNode.parseMorphemeRelations());

        // 在 MorphemeGraphView 中显示
        binding.morphemeGraphView.setData(wordNode.getWord(), relations);
    }
}