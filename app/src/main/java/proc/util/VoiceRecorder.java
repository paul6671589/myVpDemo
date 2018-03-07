package proc.util;

/**
 * Created by liangqireng on 2017/11/30.
 */

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;


public class VoiceRecorder {
    public static final int mFrequency = 16000;
    public static final int mChannelCfg = AudioFormat.CHANNEL_IN_MONO;
    public static final int mAudDataEnding = AudioFormat.ENCODING_PCM_16BIT;

    private int mRecBufSize;
    private AudioRecord mAudioRecord;
    private int mMicSource;
    private boolean mInitSuceed = false;

    // 保存录音数据链表
    private recCbInterface mCallback;
    private LinkedList<short[]> mData =  new LinkedList<>();

    private boolean mIsRecording;

    Thread mRecThread;
    int mIndex;

    public VoiceRecorder(recCbInterface callback) {

        //每次缓冲区能读入最多的字节数
        mRecBufSize = AudioRecord.getMinBufferSize(mFrequency, mChannelCfg, mAudDataEnding);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mMicSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        } else {
            mMicSource = MediaRecorder.AudioSource.VOICE_CALL;
        }

        mInitSuceed = true;
        mAudioRecord = new AudioRecord(mMicSource, mFrequency, mChannelCfg, mAudDataEnding, mRecBufSize);
        if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            mAudioRecord.release();
            mInitSuceed = false;
        }

        this.mCallback = callback;
    }

    /**
     * 开始录音
     */
    public void start(int index) {
        mData.clear();
        try {
            mAudioRecord.startRecording();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        mIsRecording = true;
        mRecThread = new Thread(new recTask());
        mRecThread.start();
        mIndex = index;
    }

    /**
     * 停止录音
     */
    public void stop() {
        mIsRecording = false;
        try {
            mRecThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mAudioRecord.stop();
        mAudioRecord.release();
    }

    /**
     *
     * @return 获得归一化信号
     */
    public synchronized double[] getNormalizedData() {
        if(mData.size() == 0) {
            return null;
        }
        int dataLength = mData.getFirst().length * (mData.size() - 1) + mData.getLast().length;
        // 浮点
        double[] data = new double[dataLength];

        // 迭代器
        Iterator<short[]> it = mData.listIterator();

        int count = 0;
        while (it.hasNext()) {
            short[] temp = it.next();
            for (int i = 0; i < temp.length; i++){
                data[count + i] = ((double) temp[i]) / Short.MAX_VALUE;
            }
            count += temp.length;
        }

        return data;
    }

    /**
     * 录音线程
     */
    private class recTask implements Runnable {

        @Override
        public void run() {
            while (mIsRecording) {
                short[] buffer = new short[mRecBufSize / 2];
                int bufSize = mAudioRecord.read(buffer, 0, mRecBufSize / 2);
                synchronized (this) {
                    if (bufSize > 0) {
                        mData.add(buffer);
                        if (mIndex != 0) {
                            mCallback.updateRecBuf(buffer, bufSize, mIndex);
                        }
                    }
                }
            }
        }
    }

    public interface recCbInterface {
        public void updateRecBuf(short[] buf, int bufSize, int index);
    }
}
