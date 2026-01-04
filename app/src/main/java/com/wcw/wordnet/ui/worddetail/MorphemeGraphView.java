package com.wcw.wordnet.ui.worddetail;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import com.wcw.wordnet.model.entity.MorphemeRelation;
import com.wcw.wordnet.model.entity.WordNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 词根网络可视化自定义 View
 * 绘制词根节点和单词节点的连接关系
 */
public class MorphemeGraphView extends View {
    private Paint morphemePaint;
    private Paint wordPaint;
    private Paint linePaint;
    private Paint textPaint;

    private List<MorphemeRelation> relations = new ArrayList<>();
    private Map<String, PointF> nodePositions = new HashMap<>();
    private String centerWord;

    public MorphemeGraphView(Context context) {
        super(context);
        init();
    }

    public MorphemeGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 词根节点画笔（蓝色圆形）
        morphemePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        morphemePaint.setColor(0xFF2196F3);
        morphemePaint.setStyle(Paint.Style.FILL);

        // 单词节点画笔（灰色矩形）
        wordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wordPaint.setColor(0xFF9E9E9E);
        wordPaint.setStyle(Paint.Style.FILL);

        // 连接线画笔
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xFFBDBDBD);
        linePaint.setStrokeWidth(3f);

        // 文字画笔
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF000000);
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * 设置数据并刷新视图
     */
    public void setData(String centerWord, List<MorphemeRelation> relations) {
        this.centerWord = centerWord;
        this.relations = relations;
        calculatePositions();
        invalidate();  // 重绘
    }

    /**
     * 计算节点位置（简单环形布局）
     */
    private void calculatePositions() {
        nodePositions.clear();

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        // 中心单词在中心
        PointF center = new PointF(width / 2f, height / 2f);
        nodePositions.put(centerWord, center);

        // 词根围绕中心单词
        int count = relations.size();
        float radius = Math.min(width, height) * 0.3f;

        for (int i = 0; i < count; i++) {
            MorphemeRelation relation = relations.get(i);
            float angle = (float) (i * 2 * Math.PI / count - Math.PI / 2);
            float x = center.x + (float) (radius * Math.cos(angle));
            float y = center.y + (float) (radius * Math.sin(angle));
            nodePositions.put(relation.getMorpheme(), new PointF(x, y));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (nodePositions.isEmpty()) return;

        // 1. 绘制连接线
        for (MorphemeRelation relation : relations) {
            PointF wordPos = nodePositions.get(centerWord);
            PointF morphemePos = nodePositions.get(relation.getMorpheme());
            if (wordPos != null && morphemePos != null) {
                canvas.drawLine(wordPos.x, wordPos.y, morphemePos.x, morphemePos.y, linePaint);
            }
        }

        // 2. 绘制词根节点（圆形）
        for (MorphemeRelation relation : relations) {
            PointF pos = nodePositions.get(relation.getMorpheme());
            if (pos != null) {
                canvas.drawCircle(pos.x, pos.y, 60f, morphemePaint);
                canvas.drawText(relation.getMorpheme(), pos.x, pos.y + 15f, textPaint);
            }
        }

        // 3. 绘制中心单词（矩形）
        PointF wordPos = nodePositions.get(centerWord);
        if (wordPos != null) {
            canvas.drawRect(
                    wordPos.x - 100f, wordPos.y - 50f,
                    wordPos.x + 100f, wordPos.y + 50f,
                    wordPaint
            );
            canvas.drawText(centerWord, wordPos.x, wordPos.y + 15f, textPaint);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculatePositions();
    }
}