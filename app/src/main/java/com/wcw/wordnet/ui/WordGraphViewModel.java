package com.wcw.wordnet.ui;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.wcw.wordnet.data.local.dao.ReviewQueueDao;
import com.wcw.wordnet.data.local.dao.WordDao;
import com.wcw.wordnet.data.repository.WordRepository;
import com.wcw.wordnet.model.entity.ReviewQueue;
import com.wcw.wordnet.model.entity.WordNode;
import com.wcw.wordnet.ui.review.ReviewState;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 单词图谱ViewModel
 * UI和数据层的桥梁，管理单词数据的生命周期和业务逻辑
 */

public class WordGraphViewModel extends AndroidViewModel {

    private final WordRepository repository;
    private final CompositeDisposable disposable;   // 用于统一管理在ViewModel创建的所有异步任务

    // ✅ 新增：复习状态LiveData（用于驱动UI状态机）
    private final MutableLiveData<ReviewState> reviewState = new MutableLiveData<>(ReviewState.IDLE);

    // ✅ 新增：当前复习单词
    private final MutableLiveData<WordNode> currentReviewWord = new MutableLiveData<>();

    // ✅ 新增：待复习数量（用于显示小红点）
    private final MutableLiveData<Integer> dueReviewCount = new MutableLiveData<>(0);

    /**
     * 薄弱词列表（记忆强度最低的10个）
     * Activity通过observe()订阅，数据变化自动刷新UI
     */
    private final LiveData<List<WordNode>> weakWords;

    /**
     * 单词总数（可见的）
     * 用于在标题栏显示“已学习X个单词”
     */
    private final LiveData<Integer> wordCount;

    /**
     * 已掌握单词数
     * 用于进度条显示
     */
    private final LiveData<Integer> masteredWordCount;

    // 搜索条件
    private final MutableLiveData<String> rootQuery;

    // 搜索结果（先声明，后初始化）
    public final LiveData<List<WordNode>> wordsByRoot;

    // 一次性事件
    /**
     * 错误消息（Toast/Snackbar）
     * 屏幕旋转后不再重播
     */
    private final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();

    /**
     * 单词添加成功事件
     * 触发后自动清空，避免重复提示
     */
    private final SingleLiveEvent<Void> wordAddedEvent = new SingleLiveEvent<>();

    /**
     * 单词复习完成事件
     * 用于刷新复习列表
     */
    private final SingleLiveEvent<Void> wordReviewedEvent = new SingleLiveEvent<>();

    /**
     * 构造函数（系统自动调用）
     * @param application 应用上下文
     */
    public WordGraphViewModel(@NonNull Application application){
        super(application);

        // 创建 Repository 实例（单例模式，不会重复创建）
        this.repository = new WordRepository(application);

        // 初始化 RxJava 订阅池（用于管理所有异步任务）
        this.disposable = new CompositeDisposable();

        // ✅ 新增：加载待复习数量（启动时自动执行）
        loadDueReviewCount();

        /**
         * 搜索条件（可变）
         */
        this.rootQuery = new MutableLiveData<>();

        /**
         * 搜索结果（只读），自动跟对 rootQuery 变化
         */
        // switchMap：当入口数据（搜索条件）变化时，它会自动关闭旧的数据流，并开启新的数据流。
        this.wordsByRoot =
                Transformations.switchMap(rootQuery, root -> {
                    if (root == null || root.trim().isEmpty()) {
                        // 返回空的 MutableLiveData（值为 null）
                        MutableLiveData<List<WordNode>> empty = new MutableLiveData<>();
                        empty.setValue(null);
                        return empty;     // 空数据
                    }
                    return repository.getWordsByRoot(root.trim());
                });

        // 从 Repository 获取 LiveData（连接数据源）
        this.weakWords = repository.getWeakWords();
        this.wordCount = repository.getWordCount();
        this.masteredWordCount = repository.getMasteredWordCount();

        // 设置 rootQuery 初始值为 null，触发 wordsByRoot 返回空数据
        this.rootQuery.setValue(null);
    }

    // 公开方法（供Activity调用）

    /**
     * 添加新单词
     * 流程：校验→插入→通知UI
     * @param word 用户输入的单词字符串
     */
    public void addWord(String word){
        // 1. 输入校验（主线程，快速操作）
        if (word == null || word.trim().isEmpty()){
            errorMessage.setValue("单词不能为空");
            return;
        }

        // 限制长度
        if (word.length() > 50){
            errorMessage.setValue("单词长度不能超过50字符");
            return;
        }

        // 2. 创建 WordNode 对象（内存操作，无IO）
        WordNode newWord = new WordNode(word.trim().toLowerCase());
        // 默认使用单词本身作为词根（后续可自动拆解）
        newWord.setMorphemeList("[\"" + word + "\"]");

        // 3. 调用 Repository 异步插入（后台线程）
        disposable.add(
                repository.insertWord(newWord)
                        .subscribeOn(Schedulers.io())   // IO线程执行
                        // observeOn：指定下游操作在哪里执行
                        .observeOn(AndroidSchedulers.mainThread())  // 结果回主线程
                        .subscribe(
                                () -> {
                                    // 4. 成功：触发事件，Activity响应
                                    wordAddedEvent.call();  // 通知UI刷新列表
                                },
                                // throwable： RxJava 的“异常包裹”，所有错误的总称
                                throwable -> {
                                    // 5. 失败：显示错误信息
                                    errorMessage.setValue("添加失败：" + throwable.getMessage());
                                }
                        )

        );
    }

    /**
     * 复习单词
     * 流程：更新记忆强度→重新计算复习时间→通知UI
     * @param word 要复习的单词
     * @param isCorrect 用户是否答对
     */
    public void reviewWord(String word, boolean isCorrect) {
        // 1. 先查询单词（异步）
        disposable.add(
                repository.getWordById(word)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess(wordNode -> {
                            // 在主线程更新内存状态
                            wordNode.updateMemoryStrength(isCorrect);
                        })
                        .observeOn(Schedulers.io()) // 切换回IO线程
                        // 扁平化：将 Completable 融入主键
                        .flatMapCompletable(wordNode -> repository.updateWord(wordNode))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                ()->wordReviewedEvent.call(),
                                throwable -> errorMessage.setValue("更新失败：" + throwable.getMessage())
                        )
        );
    }


    /**
     * 归档单词（软删除）
     * @param word 要归档的单词
     */
    public void archiveWord(String word) {
        disposable.add(
                repository.archiveWord(word)
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    // 归档成功，Live会自动刷新列表
                                },
                                throwable -> {
                                    errorMessage.setValue("归档失败: " + throwable.getMessage());
                                }
                        )
        );
    }


    /**
     * 触发搜索：设置搜索条件
     * @param root 词根字符串
     */
    public void searchByRoot(String root) {
        rootQuery.setValue(root);   // 触发 swtichMap 自动查询
    }

    /**
     * 获取搜索结果：供 Activity 观察
     * @return LiveData<List<WordNode> 搜索结果（无需参数）
     */
    public LiveData<List<WordNode>> getWordsByRoot() {
        return wordsByRoot; // 返回 switchMap 生成的 LiveData
    }

    /**
     * 更新单词（供 Fragment 调用）
     * @param word 要更新的单词实体
     */
    public void updateWord(WordNode word) {
        disposable.add(
                repository.updateWord(word)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Log.d("ViewModel", "单词更新成功: " + word.getWord()),
                                throwable -> errorMessage.setValue("更新失败: " + throwable.getMessage())
                        )
        );
    }

    /**
     * 获取所有可见单词（供 WordsFragment 使用）
     * @return 所有单词的 LiveData
     */
    public LiveData<List<WordNode>> getAllActiveWords() {
        return repository.getAllActiveWords();  // 直接转发 Repository 的数据
    }

    public LiveData<Integer> getWordCount() {
        return wordCount;
    }

    public LiveData<List<WordNode>> getWeakWords() {
        return weakWords;
    }

    public LiveData<Integer> getMasteredWordCount() {
        return masteredWordCount;
    }

    public SingleLiveEvent<String> getErrorMessage() {
        return errorMessage;
    }

    public SingleLiveEvent<Void> getWordAddedEvent() {
        return wordAddedEvent;
    }

    public SingleLiveEvent<Void> getWordReviewedEvent() {
        return wordReviewedEvent;
    }

    /**
     * 开始复习会话
     * 调用后自动加载第一个待复习单词
     */
    public void startReviewSession() {
        reviewState.setValue(ReviewState.RECALLING);
        loadNextReviewWord();
    }

    /**
     * 加载下一个复习单词
     * 自动从Repository获取最紧急的到期单词
     */
    private void loadNextReviewWord() {
        disposable.add(
                repository.getNextReviewWord()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                word -> {
                                    // ✅ onSuccess：成功拿到单词
                                    if (word != null) {
                                        currentReviewWord.setValue(word);
                                        // 状态保持 RECALLING（由调用者设置）
                                    }
                                },
                                throwable -> {
                                    // ✅ onError：发生错误
                                    errorMessage.setValue("加载复习单词失败：" + throwable.getMessage());
                                    reviewState.setValue(ReviewState.IDLE);
                                },
                                // ✅ onComplete：这是 Maybe 特有的！表示没有数据
                                () -> {
                                    // 没有更多到期单词，进入完成状态
                                    reviewState.setValue(ReviewState.COMPLETED);
                                    currentReviewWord.setValue(null);
                                }
                        )
        );
    }


    /**
     * 用户提交复习评分
     * @param quality 评分：0=忘记, 3=困难, 4=良好, 5=完美
     */
    public void submitReview(int quality) {
        WordNode word = currentReviewWord.getValue();
        if (word == null) {
            errorMessage.setValue("错误：没有正在复习的单词");
            return;
        }

        disposable.add(
                repository.processReview(word.getWord(), quality)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    // ✅ 成功：刷新待复习数量
                                    loadDueReviewCount();

                                    // ✅ 触发单词已复习事件（供其他Fragment刷新）
                                    wordReviewedEvent.call();

                                    // ✅ 加载下一个单词
                                    reviewState.setValue(ReviewState.RECALLING);  // 重置为回忆状态
                                    loadNextReviewWord();
                                },
                                throwable -> {
                                    errorMessage.setValue("提交复习失败：" + throwable.getMessage());
                                }
                        )
        );
    }

    /**
     * 显示答案（从RECALLING切换到EVALUATING状态）
     */
    public void showAnswer() {
        reviewState.setValue(ReviewState.EVALUATING);
    }

    /**
     * 加载待复习数量（用于UI显示）
     */
    private void loadDueReviewCount() {
        disposable.add(
                repository.getDueReviewCount()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                count -> dueReviewCount.setValue(count),
                                throwable -> android.util.Log.e("ViewModel", "加载待复习数量失败", throwable)
                        )
        );
    }

    /**
     * 获取待复习数量（供Fragment观察）
     */
    public LiveData<Integer> getDueReviewCount() {
        return dueReviewCount;
    }

    // ==================== 新增Getter方法 ====================

    public LiveData<ReviewState> getReviewState() {
        return reviewState;
    }

    public LiveData<WordNode> getCurrentReviewWord() {
        return currentReviewWord;
    }


    /**
     * 获取单个单词的 LiveData（供详情页使用）
     * @param word 单词字符串
     * @return LiveData<WordNode>
     */
    public LiveData<WordNode> getWordById(String word) {
        return repository.getWordByIdLiveData(word);
    }

    /**
     * ViewModel 销毁时调用（系统自动）
     * 清理RxJava订阅，防止内存泄漏
     */
    @Override
    protected void onCleared(){
        super.onCleared();
        // 释放搜游异步任务
        if (!disposable.isDisposed()) {
            disposable.clear();
        }
    }
}
