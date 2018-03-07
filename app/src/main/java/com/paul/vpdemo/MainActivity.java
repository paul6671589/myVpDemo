package com.paul.vpdemo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.DialogInterface;
import android.app.AlertDialog;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import com.cokus.wavelibrary.draw.WaveCanvas;
import com.cokus.wavelibrary.view.WaveSurfaceView;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

import android.util.Log;
import java.io.File;

import proc.util.DataProcess;
import proc.util.VoiceRecorder;

public class MainActivity extends AppCompatActivity implements VoiceRecorder.recCbInterface {

    private final int RECORD_REG_VP1 = 1;
    private final int RECORD_REG_VP2 = 2;
    private final int RECORD_VER_VP = 0;
    private final int NUM_OF_MFCC = 12;

    private Button mBtnRegVp1;
    private Button mBtnRegVp2;
    private Button mBtnVerVp;
    private Button mBtnText;

    private android.app.AlertDialog mAlertDlg;
    private VoiceRecorder mRecorder;

    private ArrayList<Short> mRecData1 = null;
    private ArrayList<Short> mRecData2 = null;

    // 录入MFCC系数
    private double[][] mMfcc1;
    private double[][] mMfcc2;
    private double[][] mMfcc3;
    // 读取MFCC系数
    private double[][] mReadMfcc1;
    private double[][] mReadMfcc2;
    private Future<double[][]> mFuture1;
    private Future<double[][]> mFuture2;

    // 数量动态的线程池
    ExecutorService mThreadPool = Executors.newCachedThreadPool();
    // 并发等待计数器
    private CountDownLatch mLatchWrite = new CountDownLatch(2);
    private CountDownLatch mLatchRead = new CountDownLatch(2);

    private long m_d_ltime = 0;
    private long m_d_ctime = 0;

    private Handler mHandler = null;

    private WaveSurfaceView mWavesurfaceviewOne = null;
    private WaveSurfaceView mWavesurfaceviewTwo = null;
    private WaveCanvas mWaveCanvasOne = null;
    private WaveCanvas mWaveCanvasTwo = null;

    private int mRateX = 32; // 采样间隔 (控制多少pcm sample取一个点)

    private final String DATA_DIR = Environment
            .getExternalStorageDirectory() + "/Android/data/" + getClass().getPackage().getName();
    private File mFilePath = null;
    private int mRecWord = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnRegVp1 = (Button)findViewById(R.id.regVoicePrintOne);
        mBtnRegVp2 = (Button)findViewById(R.id.regVoicePrintTwo);
        mBtnVerVp  = (Button)findViewById(R.id.verVoicePrint);
        mBtnText = (Button)findViewById(R.id.textShower);

        mWavesurfaceviewOne = (WaveSurfaceView)findViewById(R.id.wavesurfaceviewOne);
        mWavesurfaceviewTwo = (WaveSurfaceView)findViewById(R.id.wavesurfaceviewTwo);

        mRecData1 = new ArrayList<Short>();
        mRecData2 = new ArrayList<Short>();

        mHandler = new Handler();

        // Create file directory
        if (sdCardExists()) {
            mFilePath = new File(DATA_DIR);
            if (!mFilePath.exists()) {
                mFilePath.mkdirs();
            }
        }

        // Check Mfcc1.bin & Mfcc2.bin exist or not
        File mfccData1 = new File(mFilePath + "/Mfcc1.bin");
        File mfccData2 = new File(mFilePath + "/Mfcc2.bin");
        if (mfccData1.exists() && mfccData2.exists()) {
            mBtnVerVp.setEnabled(true);
        }
        // Check the recWord.bin exist or not
        File recWord = new File(mFilePath + "/RecWord.bin");
        FileInputStream fis = null;
        DataInputStream dis = null;
        if (recWord.exists()) {
            try {
                fis = new FileInputStream(mFilePath + "/RecWord.bin");
                dis = new DataInputStream(new BufferedInputStream(fis));
                while (dis.available() > 0) {
                    mRecWord = dis.readInt();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            genNewRandNum();
        }

        mBtnVerVp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch (View v, MotionEvent event) {
                // 按下按钮，表示开始录音
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    AlertDialog.Builder adBuilder = new AlertDialog.Builder(MainActivity.this);
                    adBuilder.setMessage(R.string.recording)
                            .setCancelable(false)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    mAlertDlg = adBuilder.create();
                    // 显示对话框
                    mAlertDlg.show();
                    // 隐去确定按钮
                    mAlertDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    // 启动录音
                    mRecorder = new VoiceRecorder(MainActivity.this);
                    mRecorder.start(RECORD_VER_VP);
                    // 更新TextShower
                    mBtnText.setText(getString(R.string.record_words) + mRecWord);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // 关闭录音
                    mRecorder.stop();
                    mAlertDlg.setMessage(getString(R.string.processing));

                    // 计算Mfcc系数
                    try {
                        mMfcc3 = DataProcess.getMfcc(mRecorder.getNormalizedData());
                    } catch (Exception e) {
                        Log.d("getMfcc", "error");
                    }

                    if (mMfcc3 == null) {
                        mAlertDlg.setMessage(getString(R.string.no_speech));
                        // 需要点击退出
                        mAlertDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                    } else {
                        // 线程池创建读取线程，读取Mfcc文件到double[][]
                        mFuture1 = mThreadPool.submit(new readDataTask("/Mfcc1.bin"));
                        mFuture2 = mThreadPool.submit(new readDataTask("/Mfcc2.bin"));
                        try {
                            mLatchRead.await();
                            mReadMfcc1 = mFuture1.get();
                            mReadMfcc2 = mFuture2.get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }

                        if (mReadMfcc1 == null || mReadMfcc2 == null) {
                            mAlertDlg.setMessage("未能找到声音锁，请重新设置");
                            mAlertDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                        } else {
                            double dis1 = DataProcess.getDTW(mReadMfcc1, mMfcc3);
                            double meanErr1 = dis1 * 2 / (mReadMfcc1.length + mMfcc3.length);
                            double dis2 = DataProcess.getDTW(mReadMfcc2, mMfcc3);
                            double meanErr2 = dis2 * 2 / (mReadMfcc2.length + mMfcc3.length);
                            mBtnText.setText("dtw距离:(" + (Math.round(dis1 * 100) / 100.0f) + "," + (Math.round
                                    (dis2 * 100) / 100.0f) + ")" + '\n'
                                    + "帧数1:(" + mReadMfcc1.length + ")" + '\n'
                                    + "帧数2:(" + mReadMfcc1.length + ")" + '\n'
                                    + "帧数3:(" + mMfcc3.length + ")" + '\n'
                                    + "平均误差:(" + (Math.round(meanErr1 * 100) / 100.0f) + "," + (Math.round
                                    (meanErr2 * 100) / 100.0f) + ")");

                            if (((meanErr1 + meanErr2) / 2 > 40.0f) || (meanErr1 > 40.0f) || (meanErr2 > 40.0f)) {
                                mAlertDlg.setMessage(getString(R.string.verify_fail));
                                mAlertDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                            } else {
                                mAlertDlg.setMessage(getString(R.string.verify_sucess));
                                mAlertDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                            }
                        }
                    }
                }
                return false;
            }
        });

        mBtnRegVp1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch (View v, MotionEvent event) {
                // 按下按钮，表示开始录音
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    AlertDialog.Builder adBuilder = new AlertDialog.Builder(MainActivity.this);
                    adBuilder.setMessage(R.string.recording)
                            .setCancelable(false)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    mAlertDlg = adBuilder.create();
                    // 显示对话框
                    mAlertDlg.show();
                    // 隐去确定按钮
                    mAlertDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    // 重置 surfaceView
                    mWavesurfaceviewOne.resetView();
                    mRecData1.clear();
                    // 启动录音
                    mRecorder = new VoiceRecorder(MainActivity.this);
                    mRecorder.start(RECORD_REG_VP1);
                    // 显示TextShower
                    mBtnText.setText(getString(R.string.record_words) + mRecWord);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // 关闭录音
                    mRecorder.stop();
                    mAlertDlg.setMessage(getString(R.string.processing));

                    // 计算Mfcc系数
                    try {
                        mMfcc1 = DataProcess.getMfcc(mRecorder.getNormalizedData());
                    } catch (Exception e) {
                        Log.d("getMfcc", "error");
                    }

                    if (mMfcc1 == null) {
                        mAlertDlg.setMessage(getString(R.string.no_speech));
                        // 需要点击退出
                        mAlertDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                    } else {
                        // 自动退出
                        mAlertDlg.cancel();
                    }
                }
                return false;
            }
        });

        mBtnRegVp2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch (View v, MotionEvent event) {
                // 按下按钮，表示开始录音
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    AlertDialog.Builder adBuilder = new AlertDialog.Builder(MainActivity.this);
                    adBuilder.setMessage(R.string.recording)
                            .setCancelable(false)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    mAlertDlg = adBuilder.create();
                    // 显示对话框
                    mAlertDlg.show();
                    // 隐去确定按钮
                    mAlertDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    // 重置 surfaceView
                    mWavesurfaceviewTwo.resetView();
                    mRecData2.clear();
                    // 启动录音
                    mRecorder = new VoiceRecorder(MainActivity.this);
                    mRecorder.start(RECORD_REG_VP2);
                    // 显示TextShower
                    mBtnText.setText(getString(R.string.record_words) + mRecWord);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // 关闭录音
                    mRecorder.stop();
                    mAlertDlg.setMessage(getString(R.string.processing));

                    // 计算Mfcc系数
                    try {
                        mMfcc2 = DataProcess.getMfcc(mRecorder.getNormalizedData());
                    } catch (Exception e) {
                        Log.d("getMfcc", "error");
                    }

                    if (mMfcc2 == null) {
                        mAlertDlg.setMessage(getString(R.string.no_speech));
                        // 需要点击退出
                        mAlertDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                    } else {
                        // 开始计算dtw，用mfcc2与mfcc1做比较
                        double dtwDistance;
                        double meanErr;
                        try {
                            dtwDistance = DataProcess.getDTW(mMfcc1, mMfcc2);
                            meanErr = dtwDistance * 2 / (mMfcc1.length + mMfcc2.length);
                            mBtnText.setText("dtw距离:(" + (Math.round(dtwDistance * 100) / 100.0f) + ")" + '\n'
                                    + "帧数1:(" + mMfcc1.length + ")" + '\n'
                                    + "帧数2:(" + mMfcc2.length + ")" + '\n'
                                    + "平均误差:(" + (Math.round(meanErr * 100) / 100.0f) + ")");

                            // 门限设置40
                            if (meanErr > 40) {
                                mAlertDlg.setMessage(getString(R.string.not_same));
                                // 显示确定按钮
                                mAlertDlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                            } else {
                                // 保存mfcc数据到bin file
                                mThreadPool.execute(new writeDataTask("/Mfcc1.bin", mMfcc1));
                                mThreadPool.execute(new writeDataTask("/Mfcc2.bin", mMfcc2));
                                // 等待计数器为0
                                mLatchWrite.await();
                                mAlertDlg.cancel();
                                // 验证按钮打开
                                mBtnVerVp.setEnabled(true);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * 设置菜单监听方法
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_settings:
                Toast.makeText(MainActivity.this, "点击了设置菜单", Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_reset:
                Toast.makeText(MainActivity.this, "点击了关于菜单", Toast.LENGTH_SHORT).show();
                // step 1: 清空surface view
                mWavesurfaceviewOne.resetView();
                mWavesurfaceviewTwo.resetView();
                // step 2: 重新生成随机数
                genNewRandNum();
                // 禁用掉声纹验证按钮
                mBtnVerVp.setEnabled(false);
                break;
            case R.id.menu_quit:
                Toast.makeText(MainActivity.this, "点击了退出菜单", Toast.LENGTH_SHORT).show();
                finish();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void updateRecBuf(short[] buf, int bufSize, int index) {
        for (int i = 0; i < bufSize; i += mRateX) {
            if (index == 1) {
                mRecData1.add(buf[i]);
            } else if (index == 2) {
                mRecData2.add(buf[i]);
            }
        }

        // Draw canvas
        if (index == 1) {
            if (mWaveCanvasOne == null) {
                mWaveCanvasOne = new WaveCanvas(mWavesurfaceviewOne);
            }
            updateRecordUI(mWaveCanvasOne, mRecData1);
        } else if (index == 2) {
            if (mWaveCanvasTwo == null) {
                mWaveCanvasTwo = new WaveCanvas(mWavesurfaceviewTwo);
            }
            updateRecordUI(mWaveCanvasTwo, mRecData2);
        }
    }

    private void updateRecordUI(final WaveCanvas waveCanvas, final ArrayList<Short> buf) {
        m_d_ltime = new Date().getTime();
        // 两次绘图间隔的时间 d:1000/200 = 5
        if (!buf.isEmpty() && m_d_ltime - m_d_ctime >= 10) {
            int removeL = buf.size() - waveCanvas.getMaxDrawBufSize();
            for (int i = 0; i < removeL; i++) {
                buf.remove(0);
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    waveCanvas.SimpleDraw2(buf, buf.size());
                    m_d_ctime = new Date().getTime();
                }
            });
        }
    }

    private class writeDataTask implements Runnable {
        String fileName;
        double[][] data;

        public writeDataTask(String fileName, double[][] data) {
            this.fileName = fileName;
            this.data = data;
        }

        @Override
        public void run() {
            FileOutputStream fos = null;
            DataOutputStream dos = null;
            try {
                fos = new FileOutputStream(mFilePath + fileName);
                //Log.d("paul debug", "path=" + mFilePath + fileName);
                dos = new DataOutputStream(new BufferedOutputStream(fos));
                // data.length跟录取语料的时间长度有关
                // data[0].length恒定为24
                for (int i = 0; i < data.length; i++) {
                    for (int j = 0; j < data[0].length; j++) {
                        dos.writeDouble(data[i][j]);
                    }
                }
                dos.flush();
                // 计数器减一
                mLatchWrite.countDown();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class readDataTask implements Callable<double[][]> {
        String fileName;

        public readDataTask(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public double[][] call() throws Exception {
            FileInputStream fis = null;
            DataInputStream dis = null;
            try {
                fis = new FileInputStream(mFilePath + fileName);
                dis = new DataInputStream(new BufferedInputStream(fis));
                ArrayList<double[]> dataList = new ArrayList<>();
                while (dis.available() > 0) {
                    double[] mfccData = new double[2 * NUM_OF_MFCC];
                    for (int i = 0; i < mfccData.length; i++) {
                        mfccData[i] = dis.readDouble();
                    }
                    dataList.add(mfccData);
                }
                // ArrayList<double[]>转成double[][]
                double[][] result = new double[dataList.size()][2 * NUM_OF_MFCC];
                for (int i = 0; i < dataList.size(); i++) {
                    for (int j = 0; j < 2 * NUM_OF_MFCC; j++) {
                        result[i][j] = dataList.get(i)[j];
                    }
                }
                mLatchRead.countDown();
                return result;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                fis.close();
            }
            return null;
        }
    }

    private boolean sdCardExists() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    private void genNewRandNum() {
        FileOutputStream fos = null;
        DataOutputStream dos = null;
        // 生成新的随机值
        mRecWord = (int)((Math.random() * 9 + 1) * 100000);
        try {
            fos = new FileOutputStream(mFilePath + "/RecWord.bin");
            dos = new DataOutputStream(new BufferedOutputStream(fos));
            dos.writeInt(mRecWord);
            dos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
