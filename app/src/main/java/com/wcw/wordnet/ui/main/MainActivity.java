package com.wcw.wordnet.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.wcw.wordnet.R;
import com.wcw.wordnet.databinding.ActivityMainBinding;
import com.wcw.wordnet.ui.WordGraphViewModel;

/**
 * 主Activity（重构后：纯导航容器）
 * 职责：
 * 1. 初始化导航控制器（NavController）
 * 2. 连接底部导航栏（BottomNavigationView）与导航图
 * 3. 提供共享的ViewModel（供三个Fragment使用）
 *
 * 旧逻辑已全部拆分到 ReviewFragment/WordsFragment/AddFragment
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private WordGraphViewModel viewModel;  // 关键点：共享给所有Fragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 初始化 ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. 初始化共享的 ViewModel（Activity作用域）
        // 关键点：所有 Fragment 通过 requireActivity() 获取同一个 ViewModel
        viewModel = new ViewModelProvider(this).get(WordGraphViewModel.class);

        // 3. 设置导航控制器
        setupNavigation();
    }

    /**
     * 设置 Jetpack Navigation
     * 连接 BottomNavigationView 与 NavGraph
     */
    private void setupNavigation() {
        // 获取 NavHostFragment（托管所有 Fragment 的容器）
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            // 获取 NavController（导航控制器）
            NavController navController = navHostFragment.getNavController();

            // 将 BottomNavigationView 与 NavController 绑定
            // 点击底部Tab会自动切换Fragment，无需手动管理
            NavigationUI.setupWithNavController(binding.bottomNav, navController);
        }
    }

    /**
     * 提供共享的 ViewModel（供Fragment调用）
     * 示例：在 ReviewFragment 中通过 ((MainActivity)requireActivity()).getViewModel() 获取
     */
    public WordGraphViewModel getViewModel() {
        return viewModel;
    }


    /**
     * ✅ 供 Fragment 调用的方法：切换到"添加"Tab
     */
    public void switchToAddTab() {
        binding.bottomNav.setSelectedItemId(R.id.addFragment);
    }

    /**
     * ✅ 供 Fragment 调用的方法：切换到"单词"Tab
     */
    public void switchToWordsTab() {
        binding.bottomNav.setSelectedItemId(R.id.wordsFragment);
    }

}