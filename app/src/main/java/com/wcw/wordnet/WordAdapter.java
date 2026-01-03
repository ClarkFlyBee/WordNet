package com.wcw.wordnet;


import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 单词列表适配器
 * 使用 ListAdapter + DiffUtil，自动计算列表差异，高效刷新
 */

public class WordAdapter extends ListAdapter<WordNode, WordAdapter.WordViewHolder> {

    /**
     * DiffUtil.ItemCallback：定义如何判断两个WordNode是同一个
     */
    private static final DiffUtil.ItemCallback<WordNode> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<WordNode>() {
                @Override
                public boolean areItemsTheSame(@NonNull WordNode oldItem, @NonNull WordNode newItem) {
                    // 主键相同就是同一个项
                    return oldItem.getWord().equals(newItem.getWord());
                }

                @Override
                public boolean areContentsTheSame(@NonNull WordNode oldItem, @NonNull WordNode newItem) {
                    // 内容相同（记忆强度、复习次数等）
                    return oldItem.getMemoryStrength() == newItem.getMemoryStrength()
                            && oldItem.getReviewCount() == newItem.getReviewCount()
                            && oldItem.isActive() == newItem.isActive();
                }
            };

    // 当前正在复习的单词（用于 btn_review_correct / wrong）
    private WordNode selectedWord;

    public WordAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_word, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        WordNode word = getItem(position);

        // 1. 绑定数据
        holder.bind(word);

        // 2. 设置选中状态
        if (selectedWord != null && selectedWord.getWord().equals(word.getWord())) {
            // 选中态：绿色边框
            holder.itemView.setBackgroundResource(R.drawable.item_selected_background);
        } else {
            // 非选中态
            holder.itemView.setBackground(null);
            holder.setColorsByStrength(word.getMemoryStrength());
        }

        // 3. 点击监听
        holder.itemView.setOnClickListener(v -> {
            selectedWord = word;
            notifyDataSetChanged();
            Log.d("DebugSelect", "用户选中: " + word.getWord());
        });
    }

    /**
     * 获取正在复习的单词
     * @return WordNode or null
     */
    public WordNode getCurrentWord(){
        return selectedWord;
    }

    // ViewHolder
    static class WordViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvWord;
        private final TextView tvMorphemes;
        private final TextView tvStrength;
        private final View itemView;

        public WordViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvWord = itemView.findViewById(R.id.tv_word);
            tvMorphemes = itemView.findViewById(R.id.tv_morphemes);
            tvStrength = itemView.findViewById(R.id.tv_strength);
        }

        public void bind(WordNode word){
            tvWord.setText(word.getWord());
            tvMorphemes.setText("词根：" + formatMorphemes(word.getMorphemeList()));

            // 显示记忆强度百分比
            int percentage = (int)(word.getMemoryStrength() * 100);
            tvStrength.setText("掌握度：" + percentage + "%");

            // 根据掌握度设置背景色（绿色=熟，红色=生）
            int color = getColorByStrength(word.getMemoryStrength());
            itemView.setBackgroundColor(color);
        }

        private String formatMorphemes(String morphemeList){
            // 格式化 JSON 字符串，如 ["re","struct","tion"] → re + struct + tion
            return morphemeList.replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .replace(",", " + ");
        }

        /**
         * 根据记忆强度获取背景色
         * @param strength 0.0-1.0
         * @return ARGB颜色值
         */
        private int getColorByStrength(float strength) {
            float[] hsv = new float[3];
            hsv[0] = 120 * strength;            // 色相：0 = 红，120 = 绿
            hsv[1] = 0.2f + strength * 0.3f;    // 饱和度：0.2 ~ 0.5（柔和）
            hsv[2] = 0.95f;                     // 明度
            return Color.HSVToColor(hsv);
        }

        /**
         * 根据背景色计算可读性最强的文字颜色
         * 使用 W3C 标准亮度公式：L = 0.299*R + 0.587*G + 0.114*B
         * @param backgroundColor 背景色（ARGB）
         * @return 黑色或白色文字颜色
         */
        private int getContrastTextColor(int backgroundColor) {
            // 1. 提取 RGB 分量（ARGB 格式：AARRGGBB）
            int red = Color.red(backgroundColor);
            int green = Color.green(backgroundColor);
            int blue = Color.blue(backgroundColor);

            // 2. 计算感知亮度（0-255）
            double luminance = 0.299 * red + 0.587 * green + 0.114 * blue;

            // 3. 阈值128（中点），亮度>=128用黑色，<128用白色
            return luminance >= 128 ? Color.BLACK : Color.WHITE;
        }

        public void setColorsByStrength(float strength) {
            // 1. 背景色
            int backgroundColor = getColorByStrength(strength);
            itemView.setBackgroundColor(backgroundColor);

            // 2. 强制设置所有 TextView 的文字颜色（覆盖复用残留）
            int textColor = getContrastTextColor(backgroundColor);
            tvWord.setTextColor(textColor);
            tvMorphemes.setTextColor(textColor);  // 确保这行执行
            tvStrength.setTextColor(textColor);   // 确保这行执行
        }
    }

}
