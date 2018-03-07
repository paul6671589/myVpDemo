package com.cokus.wavelibrary.draw;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioRecord;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;

import com.cokus.wavelibrary.utils.Pcm2Wav;
import com.cokus.wavelibrary.view.WaveSurfaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by hexueyan on 2017/10/25.
 */

public class WaveCanvas {
	
	// 上下边距距离（dp）
    private int mPadding = 5;
    private int marginRight = 60; // 波形图绘制距离右边的距离像素，留着用来画坐标
    private float divider = 0.2f; // 为了节约绘画时间，每0.2个像素画一个数据

    private Paint circleLine = null;
    private Paint centerLine = null;
    private Paint frameLine = null;
    private Paint pcmLine = null;
    private Paint dashLine = null;

    public float rateY = 1; // Y轴缩小的比例 默认为1

    private int maxDrawBufSize = 0;
    private SurfaceView surfaceView = null;

    public WaveCanvas(SurfaceView surfaceView) {
        // 进度条
        circleLine = new Paint();
        circleLine.setColor(Color.rgb(246, 131, 126)); // 设置进度条的颜色
        // 波形中线
        centerLine = new Paint();
        centerLine.setColor(Color.rgb(39, 199, 175)); // 设置波形中线颜色
        centerLine.setStrokeWidth(2); // 设置画笔粗细
        // 边框线
        frameLine = new Paint();
        frameLine.setColor(Color.rgb(227, 207, 87));
        frameLine.setStrokeWidth(5);
        // PCM线
        pcmLine = new Paint();
        pcmLine.setColor(Color.rgb(255, 215, 0));
        pcmLine.setStrokeWidth(1); // 设置画笔粗细
        pcmLine.setAntiAlias(true);
        pcmLine.setFilterBitmap(true);
        pcmLine.setStyle(Paint.Style.FILL);
        // 网格线
        dashLine = new Paint();
        dashLine.setColor(Color.rgb(200, 200, 200));

        this.surfaceView = surfaceView;
        // 右边实际是到 width - padding
        maxDrawBufSize = (int) ((surfaceView.getWidth() - mPadding - marginRight) / divider);
    }

    public int getMaxDrawBufSize() {
        return maxDrawBufSize;
    }

    /**
     * 绘制
     *
     * @param buf 数据缓冲区
     */
    public void SimpleDraw2(ArrayList<Short> buf, int bufLen) {
        if (surfaceView == null)
            return;
            
        int height = surfaceView.getHeight();
        int width = surfaceView.getWidth();
        float halfHeight = height * 0.5f;
        rateY = (65536.0f / (height - mPadding * 2));

        Canvas canvas = surfaceView.getHolder().lockCanvas(
                new Rect(0, 0, surfaceView.getWidth(), surfaceView.getHeight())); // 关键:获取画布
        if (canvas == null)
            return;

		// 背景颜色
        canvas.drawColor(Color.rgb(120, 120, 120));

        int start = (int) (bufLen * divider);
        float y;

        if (width - start - mPadding <= marginRight) { // 如果超过预留的右边距距离
            start = width - marginRight; // 画的位置x坐标
        }
		
		// 进度条
        canvas.drawCircle(start, mPadding * 2, mPadding, circleLine); // 上圆
        canvas.drawCircle(start, height - mPadding * 2, mPadding, circleLine); // 下圆
        canvas.drawLine(start, mPadding, start, height - mPadding, circleLine); // 垂直的线
		// 边框
        canvas.drawLine (mPadding, mPadding, width - mPadding, mPadding, frameLine);
        canvas.drawLine (mPadding, height - mPadding, width - mPadding, height - mPadding, frameLine);
        canvas.drawLine (mPadding, mPadding, mPadding, height - mPadding, frameLine);
        canvas.drawLine (width - mPadding, mPadding, width - mPadding, height - mPadding, frameLine);
        // 中线
        canvas.drawLine(mPadding, height / 2, width - marginRight, height / 2, centerLine);
        canvas.drawText("      0", width - 60, height / 2 + 5, dashLine);
        // 网格线画笔
        // +(-)2000
        canvas.drawLine(mPadding, halfHeight + 2000 / rateY, width - marginRight, halfHeight + 2000 / rateY, dashLine);
        canvas.drawText("  -2000", width - 60, halfHeight + 2000 / rateY + 5, dashLine);
        canvas.drawLine(mPadding, halfHeight - 2000 / rateY, width - marginRight, halfHeight - 2000 / rateY, dashLine);
        canvas.drawText("   2000", width - 60, halfHeight - 2000 / rateY + 5, dashLine);
        // +(-)5000
        canvas.drawLine(mPadding, halfHeight + 5000 / rateY, width - marginRight, halfHeight + 5000 / rateY, dashLine);
        canvas.drawText("  -5000", width - 60, halfHeight + 5000 / rateY + 5, dashLine);
        canvas.drawLine(mPadding, halfHeight - 5000 / rateY, width - marginRight, halfHeight - 5000 / rateY, dashLine);
        canvas.drawText("   5000", width - 60, halfHeight - 5000 / rateY + 5, dashLine);
        // +(-)10000
        canvas.drawLine(mPadding, halfHeight + 10000 / rateY, width - marginRight, halfHeight + 10000 / rateY, dashLine);
        canvas.drawText("-10000", width - 60, halfHeight + 10000 / rateY + 5, dashLine);
        canvas.drawLine(mPadding, halfHeight - 10000 / rateY, width - marginRight, halfHeight - 10000 / rateY, dashLine);
        canvas.drawText(" 10000", width - 60, halfHeight - 10000 / rateY + 5, dashLine);
        // +(-)15000
        canvas.drawLine(mPadding, halfHeight + 15000 / rateY, width - marginRight, halfHeight + 15000 / rateY, dashLine);
        canvas.drawText("-15000", width - 60, halfHeight + 15000 / rateY + 5, dashLine);
        canvas.drawLine(mPadding, halfHeight - 15000 / rateY, width - marginRight, halfHeight - 15000 / rateY, dashLine);
        canvas.drawText(" 15000", width - 60, halfHeight - 15000 / rateY + 5, dashLine);
        // +(-)30000
        canvas.drawLine(mPadding, halfHeight + 30000 / rateY, width - marginRight, halfHeight + 30000 / rateY, dashLine);
        canvas.drawText("-30000", width - 60, halfHeight + 30000 / rateY + 5, dashLine);
        canvas.drawLine(mPadding, halfHeight - 30000 / rateY, width - marginRight, halfHeight - 30000 / rateY, dashLine);
        canvas.drawText(" 30000", width - 60, halfHeight - 30000 / rateY + 5, dashLine);

        for (int i = 0; i < buf.size(); i++) {
            y = buf.get(i) / rateY + height / 2; // 调节缩小比例，调节基准线
            float x = (i) * divider;
            if (width - mPadding - x <= marginRight) {
                x = width - marginRight;
            }
            // 画线的方式很多，你可以根据自己要求去画。这里只是为了简单
            canvas.drawLine(x, y, x, height - y, pcmLine); // 中间出波形
        }
        surfaceView.getHolder().unlockCanvasAndPost(canvas); // 解锁画布，提交画好的图像
    }
}
