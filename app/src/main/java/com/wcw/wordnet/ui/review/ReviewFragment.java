package com.wcw.wordnet.ui.review;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.wcw.wordnet.BuildConfig;
import com.wcw.wordnet.data.local.dao.ReviewQueueDao;
import com.wcw.wordnet.data.local.database.AppDatabase;
import com.wcw.wordnet.databinding.FragmentReviewBinding;
import com.wcw.wordnet.model.entity.ReviewQueue;
import com.wcw.wordnet.ui.WordGraphViewModel;
import com.wcw.wordnet.ui.main.MainActivity;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 复习Fragment（重构后）
 * 职责：三状态复习流程（回忆→评估→完成）
 * 状态机：IDLE → RECALLING → EVALUATING → RECALLING → ... → COMPLETED
 */
public class ReviewFragment extends Fragment {

    private WordGraphViewModel viewModel;
    private FragmentReviewBinding binding;
    private int reviewedCount = 0;  // 本轮已复习单词数量

    // ✅ 新增：本轮复习计数器
    private int sessionReviewedCount = 0;

    // ✅ 新增：临时CompositeDisposable用于测试
    private final CompositeDisposable testDisposable = new CompositeDisposable();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 从Activity获取共享ViewModel
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

        // ✅ 在每次会话开始时重置计数器
        sessionReviewedCount = 0;  // 重置为0

        setupClickListeners();
        setupObservers();

        // ✅ 开发环境：显示测试按钮
        if (BuildConfig.DEBUG) {
            binding.btnResetForTesting.setVisibility(View.VISIBLE);
            binding.btnResetForTesting.setOnClickListener(v -> resetReviewQueueForTesting());
        }

        // 开始复习会话
        viewModel.startReviewSession();
    }

    /**
     * 设置所有按钮点击事件
     */
    private void setupClickListeners() {
        // 回忆状态：显示答案
        binding.btnShowAnswer.setOnClickListener(v -> {
            viewModel.showAnswer();
        });

        // 评估状态：四个评分按钮
        binding.btnForgot.setOnClickListener(v -> submitReview(0));
        binding.btnHard.setOnClickListener(v -> submitReview(3));
        binding.btnGood.setOnClickListener(v -> submitReview(4));
        binding.btnEasy.setOnClickListener(v -> submitReview(5));

        // 完成状态：到单词列表
        binding.btnViewWords.setOnClickListener(v -> {
//            requireActivity().getSupportFragmentManager().popBackStack();
            ((MainActivity) requireActivity()).switchToWordsTab();
        });

        // 完成状态：到新增模块
        binding.btnAddNewWord.setOnClickListener(v -> {
            // 切换到底部导航的"添加"Tab
            // 需要与 MainActivity 通信，让它切换 BottomNavigationView
            ((MainActivity) requireActivity()).switchToAddTab();
        });
    }

    /**
     * 设置LiveData观察
     */
    private void setupObservers() {
        // 1. 观察复习状态变化（核心：驱动三状态切换）
        viewModel.getReviewState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;

            // 根据状态显示/隐藏对应的View
            binding.recallView.setVisibility(state == ReviewState.RECALLING ? View.VISIBLE : View.GONE);
            binding.evaluateView.setVisibility(state == ReviewState.EVALUATING ? View.VISIBLE : View.GONE);
            binding.scoreButtons.setVisibility(state == ReviewState.EVALUATING ? View.VISIBLE : View.GONE);
            binding.completedView.setVisibility(state == ReviewState.COMPLETED ? View.VISIBLE : View.GONE);

            // ✅ 特别注意：完成状态时，确保按钮一定可见
            if (state == ReviewState.COMPLETED) {
                // 强制显示按钮（防止被之前的逻辑隐藏）
                binding.btnViewWords.setVisibility(View.VISIBLE);
                binding.btnAddNewWord.setVisibility(View.VISIBLE);

                // 开发环境：显示测试按钮
                if (BuildConfig.DEBUG) {
                    Log.d("DEBUG_WCW", "现在是调试状态");
                    binding.btnResetForTesting.setVisibility(View.VISIBLE);
                }
            }

            // 状态切换动画（可选）
            if (state == ReviewState.RECALLING) {
                // 重置评估视图的滚动位置
                binding.evaluateView.scrollTo(0, 0);
            }
        });

        // 2. 观察当前复习单词
        viewModel.getCurrentReviewWord().observe(getViewLifecycleOwner(), word -> {
            if (word != null) {
                // 更新UI显示
                binding.tvWord.setText(word.getWord());
                binding.tvMorphemes.setText(formatMorphemes(word.getMorphemeList()));
                binding.tvWordAnswer.setText(word.getWord());  // 评估状态也显示

                reviewedCount++;  // 计数器+1
            }
        });

        // 3. 观察错误消息
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        // 4. 观察待复习数量（可选：显示在Toolbar）
        viewModel.getDueReviewCount().observe(getViewLifecycleOwner(), count -> {
            // 可以在这里更新Toolbar的小红点或数字
        });

        viewModel.getDueReviewCount().observe(getViewLifecycleOwner(), count -> {
            // 可以在此更新进度，但更简单的做法是：
            // 在 ViewModel 中维护 sessionTotalCount 变量
        });
    }

    /**
     * 提交复习评分
     * @param quality 0=忘记, 3=困难, 4=良好, 5=完美
     */
    private void submitReview(int quality) {

        sessionReviewedCount++;  // 每次提交+1

        // 更新统计文本
        binding.tvCompletionStats.setText(
                String.format("本次复习了 %d 个单词", sessionReviewedCount)
        );

        // 显示反馈Toast
        String feedback = getFeedbackText(quality);
        Toast.makeText(getContext(), feedback, Toast.LENGTH_SHORT).show();

        // 提交到ViewModel
        viewModel.submitReview(quality);
    }

    /**
     * 获取评分反馈文本
     */
    private String getFeedbackText(int quality) {
        switch (quality) {
            case 0: return "❌ 忘记 - 会再次复习";
            case 3: return "🤔 困难 - 加强复习频率";
            case 4: return "✅ 良好 - 按原计划复习";
            case 5: return "🌟 完美 - 延长复习间隔";
            default: return "已记录";
        }
    }

    /**
     * 格式化词根显示
     */
    private String formatMorphemes(String morphemeList) {
        if (morphemeList == null || morphemeList.isEmpty()) {
            return "暂无词根信息";
        }
        return morphemeList.replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace(",", " + ");
    }

    /**
     * ✅ 测试专用：重置所有单词的复习时间为"现在"
     */
    private void resetReviewQueueForTesting() {
        // 显示加载提示
        Toast.makeText(getContext(), "重置中...", Toast.LENGTH_SHORT).show();

        testDisposable.add(
                Completable.fromAction(() -> {
                            // 1. 获取 DAO（通过 Database 单例）
                            AppDatabase db = AppDatabase.getDatabase(requireContext());
                            ReviewQueueDao queueDao = db.reviewQueueDao();

                            // 2. 获取所有复习项（同步查询）
                            List<ReviewQueue> allItems = queueDao.getAllReviewQueuesSync();

                            // 3. 重置所有时间为现在
                            for (ReviewQueue item : allItems) {
                                item.setNextReviewTime(System.currentTimeMillis());
                                queueDao.updateReviewQueue(item);
                            }

                            Log.d("ReviewFragment", "✅ 重置完成，共 " + allItems.size() + " 个单词");
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    // 成功：重新开始复习会话
                                    Toast.makeText(getContext(), "✅ 已重置，开始新一轮复习", Toast.LENGTH_SHORT).show();
                                    sessionReviewedCount = 0;  // 重置计数器
                                    binding.tvCompletionStats.setText("本次复习了 0 个单词");  // 重置统计
                                    viewModel.startReviewSession();  // 重新开始
                                },
                                throwable -> {
                                    // 失败：显示错误
                                    Toast.makeText(getContext(), "重置失败：" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("ReviewFragment", "重置失败", throwable);
                                }
                        )
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}