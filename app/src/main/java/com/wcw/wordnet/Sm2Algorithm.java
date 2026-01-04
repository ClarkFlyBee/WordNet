package com.wcw.wordnet;

import java.util.concurrent.TimeUnit;

/**
 * SM-2间隔重复算法实现
 * 基于SuperMemo-2算法，用于科学计算单词的复习间隔
 *
 * 核心参数说明：
 * - Easiness Factor (EF): 难度因子，初始2.5，范围1.3-3.0
 * - Interval: 复习间隔天数，动态调整
 * - Repetition Count: 连续正确次数
 * - Quality: 用户自评质量（0=忘记, 3=困难, 4=良好, 5=完美）
 */
public class Sm2Algorithm {


    private static final float DEFAULT_EASINESS = 2.5f;
    private static final float MIN_EASINESS = 1.3f;
    private static final int MIN_INTERVAL = 1;
    private static final int MAX_QUALITY = 5;

    /**
     * 核心算法：计算下次复习时间
     *
     * @param item 当前复习项（包含EF、间隔、重复次数）
     * @param quality 用户评分：
     *                0 = 完全忘记（重置进度）
     *                3 = 回答困难（需要提示）
     *                4 = 回答正确但犹豫
     *                5 = 回答完美（毫不犹豫）
     * @return 更新后的复习项，包含新的EF、间隔、重复次数和下次复习时间
     */
    public ReviewQueue calculateNextReview(ReviewQueue item, int quality) {
        // 1. 边界保护：确保quality在0-5之间
        quality = Math.max(0, Math.min(MAX_QUALITY, quality));

        // 2. 计算新的难度因子（EF）
        // SM-2公式：EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))
        float currentEasiness = item.getEasinessFactor();
        float easinessChange = (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f));
        float newEasiness = currentEasiness + easinessChange;

        // 3. 保护：EF不能低于1.3（最低难度）
        newEasiness = Math.max(MIN_EASINESS, newEasiness);

        int newInterval;
        int newRepetitions;

        // 4. 判断用户是否记住（quality < 3表示忘记）
        if (quality < 3) {
            // 忘记：重置间隔和重复次数，从头开始
            newInterval = MIN_INTERVAL;
            newRepetitions = 0;
        } else {
            // 记住：增加重复次数，计算新间隔
            newRepetitions = item.getRepetitionCount() + 1;

            // 根据重复次数决定间隔
            switch (newRepetitions) {
                case 1:
                    newInterval = 1;  // 第一次记住：1天后
                    break;
                case 2:
                    newInterval = 6;  // 第二次记住：6天后
                    break;
                default:
                    // 第三次及以上：间隔 = 旧间隔 × 难度因子
                    newInterval = Math.round(item.getIntervalDays() * newEasiness);
                    break;
            }
        }

        // 5. 计算下次复习时间戳（当前时间 + 间隔天数）
        long nextReviewTime = System.currentTimeMillis() +
                TimeUnit.DAYS.toMillis(newInterval);

        // 6. 创建并返回更新后的复习项
        ReviewQueue updatedItem = new ReviewQueue();
        updatedItem.setWordId(item.getWordId());
        updatedItem.setNextReviewTime(nextReviewTime);
        updatedItem.setIntervalDays(newInterval);
        updatedItem.setEasinessFactor(newEasiness);
        updatedItem.setRepetitionCount(newRepetitions);
        updatedItem.setReviewState(0);  // 重置为待复习状态

        return updatedItem;
    }

    /**
     * 创建初始复习项（用于新单词）
     * 设置立即复习（nextReviewTime = 当前时间）
     */
    public ReviewQueue createInitialItem(String wordId) {
        ReviewQueue initialItem = new ReviewQueue();
        initialItem.setWordId(wordId);
        initialItem.setNextReviewTime(System.currentTimeMillis());
        initialItem.setIntervalDays(1);
        initialItem.setEasinessFactor(DEFAULT_EASINESS);
        initialItem.setRepetitionCount(0);
        initialItem.setReviewState(0);
        return initialItem;
    }

    /**
     * 获取质量评分的文本描述（调试用）
     */
    public static String getQualityDescription(int quality) {
        switch (quality) {
            case 0: return "❌ 完全忘记";
            case 3: return "🤔 回答困难";
            case 4: return "✅ 回答正确";
            case 5: return "🌟 完美回答";
            default: return "未知";
        }
    }

}
