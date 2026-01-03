package com.wcw.wordnet;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一次性LiveData事件
 * 用于 Toast、Snackbar、Navigation等场景，避免重复触发
 * 原理：使用AtomicBoolean标记值是否已被消费
 * @param <T>
 */

public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        super.observe(owner, value -> {
            // 只有 pending 为 true 才触发回调
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(value);
            }
        });
    }

    @MainThread
    @Override
    public void setValue(@NonNull T value){
        pending.set(true);  // 有新值待消费
        super.setValue(value);
    }

    /**
     * 发送空事件
     */
    @MainThread
    public void call(){
        setValue(null);
    }

}
