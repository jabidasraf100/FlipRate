package com.nexusbuild.apps.fliprate.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.nexusbuild.apps.fliprate.R;
import com.nexusbuild.apps.fliprate.data.RatesHistoryRepository;

import java.util.Collections;
import java.util.List;

public class RateChartView extends View {
    private List<RatesHistoryRepository.RatePoint> series = Collections.emptyList();

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RateChartView(Context context) {
        super(context);
        init();
    }

    public RateChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        int accent = getResources().getColor(R.color.accent);
        int border = getResources().getColor(R.color.border);
        int textMuted = getResources().getColor(R.color.text_muted);

        linePaint.setColor(accent);
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);

        dotPaint.setColor(accent);
        dotPaint.setStyle(Paint.Style.FILL);

        axisPaint.setColor(border);
        axisPaint.setStrokeWidth(2f);

        textPaint.setColor(textMuted);
        textPaint.setTextSize(24f);
    }

    public void setSeries(List<RatesHistoryRepository.RatePoint> series) {
        this.series = series != null ? series : Collections.emptyList();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (series.isEmpty()) {
            return;
        }

        float width = getWidth();
        float height = getHeight();
        float paddingLeft = 90f;
        float paddingRight = 20f;
        float paddingTop = 20f;
        float paddingBottom = 40f;

        float plotWidth = width - paddingLeft - paddingRight;
        float plotHeight = height - paddingTop - paddingBottom;

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (RatesHistoryRepository.RatePoint p : series) {
            min = Math.min(min, p.rate);
            max = Math.max(max, p.rate);
        }
        double range = (max - min) == 0 ? 1 : (max - min);

        // Axes
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, height - paddingBottom, axisPaint);
        canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, axisPaint);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(formatNumber(max), paddingLeft - 12, paddingTop + 10, textPaint);
        canvas.drawText(formatNumber(min), paddingLeft - 12, height - paddingBottom, textPaint);

        int count = series.size();
        float stepX = count > 1 ? plotWidth / (count - 1) : 0;

        float[] xs = new float[count];
        float[] ys = new float[count];
        for (int i = 0; i < count; i++) {
            RatesHistoryRepository.RatePoint p = series.get(i);
            xs[i] = paddingLeft + stepX * i;
            ys[i] = (float) (paddingTop + plotHeight - ((p.rate - min) / range) * plotHeight);
        }

        for (int i = 0; i < count - 1; i++) {
            canvas.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1], linePaint);
        }
        for (int i = 0; i < count; i++) {
            canvas.drawCircle(xs[i], ys[i], 6f, dotPaint);
        }

        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < count; i++) {
            String label = series.get(i).date.length() >= 10 ? series.get(i).date.substring(5) : series.get(i).date;
            canvas.drawText(label, xs[i], height - paddingBottom + 28, textPaint);
        }
    }

    private String formatNumber(double value) {
        return String.format(java.util.Locale.US, "%.4f", value);
    }
}
