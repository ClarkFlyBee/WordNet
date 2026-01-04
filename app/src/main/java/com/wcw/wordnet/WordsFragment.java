package com.wcw.wordnet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wcw.wordnet.databinding.FragmentWordsBinding;

/**
 * 单词列表Fragment
 * 职责：展示所有单词，支持词根搜索
 * 生命周期：用户点击底部"单词"Tab时显示
 */
public class WordsFragment extends Fragment {

    private WordGraphViewModel viewModel;
    private FragmentWordsBinding binding;
    private WordAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(WordGraphViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWordsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 初始化 RecyclerView
        adapter = new WordAdapter();
        binding.rvWords.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvWords.setAdapter(adapter);

        // 2. 观察所有单词
        viewModel.getAllActiveWords().observe(getViewLifecycleOwner(), words -> {
            adapter.submitList(words);
        });

        // 3. 观察进度条
        viewModel.getWordCount().observe(getViewLifecycleOwner(), totalCount -> {
            viewModel.getMasteredWordCount().observe(getViewLifecycleOwner(), masteredCount -> {
                if (totalCount != null && totalCount > 0 && masteredCount != null) {
                    int percentage = (int) ((masteredCount * 100.0f) / totalCount);
                    binding.pbMastery.setProgress(percentage);
                    binding.tvMasteryText.setText(percentage + "% (" + masteredCount + "/" + totalCount + ")");
                } else {
                    binding.pbMastery.setProgress(0);
                    binding.tvMasteryText.setText("0% (0/0)");
                }
            });
        });

        // 4. 搜索功能
        binding.btnSearch.setOnClickListener(v -> {
            String root = binding.etSearchRoot.getText().toString().trim();
            viewModel.searchByRoot(root);
        });

        viewModel.getWordsByRoot().observe(getViewLifecycleOwner(), words -> {
            adapter.submitList(words);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
