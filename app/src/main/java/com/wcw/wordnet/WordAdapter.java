package com.wcw.wordnet;


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
    private WordNode currentWord;

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
        holder.bind(word);

        // 点击项时设置当前复习单词
        holder.itemView.setOnClickListener(v -> {
            currentWord = word;
        });
    }

    /**
     * 获取正在复习的单词
     * @return WordNode or null
     */
    public WordNode getCurrentWord(){
        return currentWord;
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

        private int getColorByStrength(float strength) {
            // 0.0=红色，1.0=绿色，中间渐变
            int red = (int)(255 * (1 - strength));
            int green = (int)(255 * strength);
            int blue = 0;
            return 0xFF000000 | (red << 16) | (green << 8) | blue;  // ARGB格式
        }

    }

}
