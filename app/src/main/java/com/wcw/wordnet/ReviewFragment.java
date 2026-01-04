package com.wcw.wordnet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.wcw.wordnet.databinding.FragmentReviewBinding;

/**
 * 复习Fragment
 * 职责：展示当前需要复习的单词，提供评估功能
 * 生命周期：用户点击底部"复习"Tab时显示
 */
public class ReviewFragment extends Fragment {

    private WordGraphViewModel viewModel;
    private FragmentReviewBinding binding;
    private WordNode currentWord;  // 当前正在复习的单词

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 从Activity获取共享的ViewModel（关键点！）
        viewModel = new ViewModelProvider(requireActivity()).get(WordGraphViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 观察薄弱词列表，自动加载第一个
        viewModel.getWeakWords().observe(getViewLifecycleOwner(), words -> {
            if (words != null && !words.isEmpty()) {
                loadWordForReview(words.get(0));  // 加载第一个薄弱词
            } else {
                showNoWordsMessage();
            }
        });

        // 2. 设置评估按钮点击事件
        binding.btnForgot.setOnClickListener(v -> reviewWord(false, 0.0f));  // 完全不记得
        binding.btnHard.setOnClickListener(v -> reviewWord(true, 0.3f));     // 模糊，低强度
        binding.btnEasy.setOnClickListener(v -> reviewWord(true, 0.8f));     // 认识，高强度
    }

    /**
     * 加载单词到复习界面
     */
    private void loadWordForReview(WordNode word) {
        this.currentWord = word;
        binding.tvCurrentWord.setText(word.getWord());
        binding.tvWordMorphemes.setText("词根：" + formatMorphemes(word.getMorphemeList()));
        // 隐藏释义（用户需要尝试回忆）
        binding.tvWordMorphemes.setVisibility(View.GONE);
    }

    /**
     * 用户评估后提交复习结果
     * @param isSuccess 是否成功（认识/模糊/不认识）
     * @param customStrength 自定义强度（模糊=0.3，认识=0.8）
     */
    private void reviewWord(boolean isSuccess, float customStrength) {
        if (currentWord == null) return;

        // 1. 更新记忆强度
        if (isSuccess) {
            currentWord.setMemoryStrength(customStrength);
        } else {
            currentWord.setMemoryStrength(0.0f);  // 完全不记得则重置
        }
        currentWord.setReviewCount(currentWord.getReviewCount() + 1);
        currentWord.setLastReviewed(System.currentTimeMillis());

        // 2. 保存到数据库
        viewModel.updateWord(currentWord);

        // 3. 显示反馈
        String message = isSuccess ? (customStrength > 0.5 ? "认识 ✓" : "模糊 ~") : "不认识 ✗";
        Toast.makeText(getContext(), message + " 下次复习时间已更新", Toast.LENGTH_SHORT).show();

        // 4. 加载下一个单词（延迟500ms，给用户反馈时间）
        binding.getRoot().postDelayed(() -> {
            viewModel.getWeakWords().observe(getViewLifecycleOwner(), words -> {
                if (words != null && !words.isEmpty()) {
                    loadWordForReview(words.get(0));
                }
            });
        }, 500);
    }

    private String formatMorphemes(String morphemeList) {
        return morphemeList.replace("[", "").replace("]", "").replace("\"", "").replace(",", " + ");
    }

    private void showNoWordsMessage() {
        binding.tvCurrentWord.setText("暂无需要复习的单词");
        binding.tvWordMorphemes.setText("去添加一些新单词吧！");
        binding.btnForgot.setVisibility(View.GONE);
        binding.btnHard.setVisibility(View.GONE);
        binding.btnEasy.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;  // 防止内存泄漏
    }
}
