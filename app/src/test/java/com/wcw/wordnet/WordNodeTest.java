package com.wcw.wordnet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.wcw.wordnet.model.entity.WordNode;

import org.junit.Test;

public class WordNodeTest {

    @Test
    public void testNextReviewTime_calculationLogic() {
        WordNode word = new WordNode("test");
        word.setMemoryStrength(0.5f);   // 中等记忆
        word.setReviewCount(2);         // 基础间隔=4天
        word.setLastReviewed(0);        // 固定时间便于计算

        long actualInterval = word.getNextReviewTime() - word.getLastReviewed();

        // 预期：4天 * (1 + 0.5 * 2) = 8天
        long expectedMillis = 8 * 24 * 60 * 60 * 1000L;

        assertEquals("复习间隔计算错误", expectedMillis, actualInterval, 1000);
    }

    @Test
    public void testNextReviewTime_boundaryValues() {
        WordNode word = new WordNode("test");
        word.setLastReviewed(0);

        // 测试记忆强度=0（最小值）
        word.setMemoryStrength(0.0f);
        word.setReviewCount(0);
        long interval0 = word.getNextReviewTime() - word.getLastReviewed();
        assertEquals("memoryStrength=0时应为1天",
                1 * 24 * 60 * 60 * 1000L, interval0, 1000);

        // 测试记忆强度=1（最大值）
        word.setMemoryStrength(1.0f);
        word.setReviewCount(0);
        long interval1 = word.getNextReviewTime() - word.getLastReviewed();
        assertEquals("memoryStrength=1时应为3天",
                3 * 24 * 60 * 60 * 1000L, interval1, 1000);
    }

}
