package com.wcw.wordnet;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wcw.wordnet.databinding.ActivityMainBinding;

import java.util.List;

/**
 * 主Activity
 * 展示数据、转发用户事件、管理生命周期
 * 遵循MVVM结构：不直接操作数据库，所有逻辑通过ViewModel
 */

public class MainActivity extends AppCompatActivity {

    private WordGraphViewModel viewModel;
    private ActivityMainBinding binding;    // ViewBinding 替代传统的 findViewById
    private WordAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 初始化 ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. 初始化 ViewModel（生命周期感知）
        viewModel = new ViewModelProvider(this).get(WordGraphViewModel.class);

        // 3. 初始化 RecyclerView
        setupRecyclerView();

        // 4. 观察 LiveData（数据变化自动刷新UI）
        observeViewModel();

        // 5. 设置点击事件（转发给ViewModel）
        setupClickListeners();
    }

    // UI初始化
    /**
     * 设置 RecyclerView 布局和适配器
     */
    private void setupRecyclerView() {
        adapter = new WordAdapter();    // 创建适配器
        binding.rvWords.setLayoutManager(new LinearLayoutManager(this));    // 垂直列表
        binding.rvWords.setAdapter(adapter);    // 绑定适配器
    }

    // LiveData观察（数据驱动UI）
    /**
     * 观察ViewModel的所有LiveData
     */
    private void observeViewModel() {
        // 1. 观察薄弱词列表
        viewModel.getWeakwords().observe(this, words -> {
            if (words == null || words.isEmpty()) {
                showEmptyState();   // 显示暂无单词
            } else {
                showWordList(words);    // 显示单词列表
            }
        });

        // 2. 观察单词总数
        viewModel.getWordCount().observe(this, count -> {
            binding.tvWordCount.setText("已经学习 " + (count != null ? count : 0) + " 个单词");
        });

        // 3. 观察已掌握单词数
        viewModel.getMasteredWordCount().observe(this, count -> {
            if (count != null) {
                binding.pbMastery.setProgress(count);   // 更新进度条
            }
        });

        // 4. 观察搜索结果
        viewModel.getWordsByRoot().observe(this, words -> {
            if (words != null) {
                adapter.submitList(words);    // 显示搜索结果
            }
        });

        // 5. 观察错误消息（一次性事件）
        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // 6. 观察单词添加成功事件
        viewModel.getWordAddedEvent().observe(this, aVoid -> {
            // 清空输入框
            binding.etWord.setText("");
            Toast.makeText(MainActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
        });

        // 7. 观察复习完成事件
        viewModel.getWordReviewedEvent().observe(this, aVoid -> {
            // 刷新列表（LiveData会自动更新，这里可以显示提示）
            Toast.makeText(MainActivity.this, "复习记录已更新", Toast.LENGTH_SHORT).show();
        });
    }

    // 点击事件
    /**
     * 设置所有按钮的点击事件
     */
    private void setupClickListeners() {
        // 1. 添加单词按钮
        binding.btnAddWord.setOnClickListener(v -> {
            String word = binding.etWord.getText().toString().trim();
            viewModel.addword(word);    // 转发给ViewModel
        });

        // 2. 根据词根搜索按钮
        binding.btnSearchRoot.setOnClickListener(v -> {
            String root = binding.etSearchRoot.getText().toString().trim();  // 触发搜索
            viewModel.searchByRoot(root);
        });

        // 3. 复习“答对”按钮
        binding.btnReviewCorrect.setOnClickListener(v->{
            WordNode current = adapter.getCurrentWord();
            if (current != null) {
                viewModel.reviewWord(current.getWord(),  true);
            }
        });

        // 4. 复习“答错”按钮
        binding.btnReviewCorrect.setOnClickListener(v->{
            WordNode current = adapter.getCurrentWord();
            if (current != null) {
                viewModel.reviewWord(current.getWord(),  false);
            }
        });
    }

    // UI 状态管理
    /**
     * 显示空状态（暂无单词）
     */
    private void showEmptyState() {
        binding.rvWords.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.VISIBLE);
    }

    /**
     * 显示单词列表
     */
    private void showWordList(List<WordNode> words) {
        binding.rvWords.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);
        adapter.submitList(words);  // DiffUtil 自动计算差异
    }

}