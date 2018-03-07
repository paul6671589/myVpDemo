package com.cokus.wavelibrary.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * 该类只是一个初始化surfaceview的封装
 */
public class WaveSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder holder;

    // 上下边距距离(dp)
    private int mPadding = 5;

    public int getLine_off() {
        return mPadding;
    }

    public void setLine_off(int line_off) {
        this.mPadding = line_off;
    }

    public WaveSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.holder = getHolder();
        holder.addCallback(this);
    }

    /**
     * init surfaceview
     */
    public void initSurfaceView(final SurfaceView sfv) {
        new Thread() {
            public void run() {
                Canvas canvas = sfv.getHolder().lockCanvas(
                        new Rect(0, 0, sfv.getWidth(), sfv.getHeight())); // 关键:获取画布
                if (canvas == null) {
                    return;
                }

                int height = sfv.getHeight();
                int width = sfv.getWidth();

                drawView(canvas, width, height, mPadding);

                sfv.getHolder().unlockCanvasAndPost(canvas); // 解锁画布，提交画好的图像
            }
        }.start();
    }

    public void resetView(){
        new Thread() {
            public void run() {
                Canvas canvas = holder.lockCanvas(
                        new Rect(0, 0, getWidth(), getHeight())); // 关键:获取画布
                if (canvas == null) {
                    return;
                }

                int height = getHeight();
                int width = getWidth();

                drawView(canvas, width, height, mPadding);

                getHolder().unlockCanvasAndPost(canvas); // 解锁画布，提交画好的图像
            }
        }.start();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void surfaceCreated(SurfaceHolder holder) {
        initSurfaceView(this);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void drawView(Canvas canvas, int width, int height, int padding) {
        float halfHeight = height * 0.5f;
        float halfWidth = width * 0.5f;
        float rateY = (65536.0f / (height - padding * 2));

        // 背景颜色
        canvas.drawColor(Color.rgb(120, 120, 120));
        // 初始化画笔
        Paint centerLine = new Paint();
        Paint frameLine = new Paint();
        Paint dashLine = new Paint();

        // 边框画笔
        frameLine.setColor(Color.rgb(227, 207, 87));
        frameLine.setStrokeWidth(5);
        canvas.drawLine (padding, padding, width - padding, padding, frameLine);
        canvas.drawLine (padding, height - padding, width - padding, height - padding, frameLine);
        canvas.drawLine (padding, padding, padding, height - padding, frameLine);
        canvas.drawLine (width - padding, padding, width - padding, height - padding, frameLine);

        // 中线画笔
        centerLine.setColor(Color.rgb(39, 199, 175));
        centerLine.setStrokeWidth(2);
        canvas.drawLine(padding, height / 2, width - 60, height / 2, centerLine);

        // 网格线画笔
        dashLine.setColor(Color.rgb(200, 200, 200));
        // 0
        canvas.drawText("      0", width - 60, height / 2 + 5, dashLine);
        // +(-)2000
        canvas.drawLine(padding, halfHeight + 2000 / rateY, width - 60, halfHeight + 2000 / rateY, dashLine);
        canvas.drawText("  -2000", width - 60, halfHeight + 2000 / rateY + 5, dashLine);
        canvas.drawLine(padding, halfHeight - 2000 / rateY, width - 60, halfHeight - 2000 / rateY, dashLine);
        canvas.drawText("   2000", width - 60, halfHeight - 2000 / rateY + 5, dashLine);
        // +(-)5000
        canvas.drawLine(padding, halfHeight + 5000 / rateY, width - 60, halfHeight + 5000 / rateY, dashLine);
        canvas.drawText("  -5000", width - 60, halfHeight + 5000 / rateY + 5, dashLine);
        canvas.drawLine(padding, halfHeight - 5000 / rateY, width - 60, halfHeight - 5000 / rateY, dashLine);
        canvas.drawText("   5000", width - 60, halfHeight - 5000 / rateY + 5, dashLine);
        // +(-)10000
        canvas.drawLine(padding, halfHeight + 10000 / rateY, width - 60, halfHeight + 10000 / rateY, dashLine);
        canvas.drawText("-10000", width - 60, halfHeight + 10000 / rateY + 5, dashLine);
        canvas.drawLine(padding, halfHeight - 10000 / rateY, width - 60, halfHeight - 10000 / rateY, dashLine);
        canvas.drawText(" 10000", width - 60, halfHeight - 10000 / rateY + 5, dashLine);
        // +(-)15000
        canvas.drawLine(padding, halfHeight + 15000 / rateY, width - 60, halfHeight + 15000 / rateY, dashLine);
        canvas.drawText("-15000", width - 60, halfHeight + 15000 / rateY + 5, dashLine);
        canvas.drawLine(padding, halfHeight - 15000 / rateY, width - 60, halfHeight - 15000 / rateY, dashLine);
        canvas.drawText(" 15000", width - 60, halfHeight - 15000 / rateY + 5, dashLine);
        // +(-)30000
        canvas.drawLine(padding, halfHeight + 30000 / rateY, width - 60, halfHeight + 30000 / rateY, dashLine);
        canvas.drawText("-30000", width - 60, halfHeight + 30000 / rateY + 5, dashLine);
        canvas.drawLine(padding, halfHeight - 30000 / rateY, width - 60, halfHeight - 30000 / rateY, dashLine);
        canvas.drawText(" 30000", width - 60, halfHeight - 30000 / rateY + 5, dashLine);
    }
}
