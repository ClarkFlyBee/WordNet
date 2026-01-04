package com.wcw.wordnet.ui.review;

/**
 * 复习状态机枚举
 * 用于控制 ReviewFragment 的三状态切换
 */
public enum ReviewState {
    /**
     * 空闲状态：未开始复习或复习会话结束
     */
    IDLE,

    /**
     * 回忆中：显示单词和词根，隐藏释义，用户尝试回忆
     */
    RECALLING,

    /**
     * 评估中：显示完整信息，用户自我评分（0/3/4/5分）
     */
    EVALUATING,

    /**
     * 完成状态：本轮所有待复习单词已完成
     */
    COMPLETED
}