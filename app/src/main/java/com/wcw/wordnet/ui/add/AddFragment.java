package com.wcw.wordnet.ui.add;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.wcw.wordnet.databinding.FragmentAddBinding;
import com.wcw.wordnet.ui.WordGraphViewModel;

/**
 * 添加Fragment
 * 职责：添加新单词
 * 生命周期：用户点击底部"添加"Tab时显示
 */
public class AddFragment extends Fragment {

    private WordGraphViewModel viewModel;
    private FragmentAddBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(WordGraphViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnConfirmAdd.setOnClickListener(v -> {
            String word = binding.etNewWord.getText().toString().trim();
            if (!word.isEmpty()) {
                viewModel.addWord(word);
                binding.etNewWord.setText("");
                Toast.makeText(getContext(), "添加成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "单词不能为空", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
