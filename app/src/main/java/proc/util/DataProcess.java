package proc.util;

import com.example.libvoiceprocess.Dtw;
import com.example.libvoiceprocess.Mfcc;
import com.example.libvoiceprocess.Normal;
import proc.util.VoiceRecorder;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 数据处理类
 */
public class DataProcess{

    /**
     * 获得mfcc参数
     */
    public static double[][] getMfcc(double[] nomalizeData) throws Exception{
        ExecutorService executor = Executors.newCachedThreadPool();
        Future<double[][]> future = executor.submit(new calcMFCC(nomalizeData));
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            throw e;
        }
    }

    /**
     *计算DTW距离
     */
    public static double getDTW(double[][] mfcc1, double[][] mfcc2) {
        return Dtw.dtw(mfcc1, mfcc2);
    }

    private static class calcMFCC implements Callable<double[][]> {

        private double[] nomalizeData;
        private final int NUM_OF_MFCC = 12;
        private final int NUM_OF_FILTER = 26;

        public calcMFCC(double[] nomalizeData) {
            this.nomalizeData = nomalizeData;
        }

        @Override
        public double[][] call() throws Exception {

            //预加重信号
            double[] preEmphasis = Normal.highpass(nomalizeData);
            //短时能量和短时过零率
            double[] stApm = Normal.shortTernEnergy(preEmphasis);
            double[] stCZ = Normal.shortTernCZ(preEmphasis, VoiceRecorder.mFrequency);
            //端点检测
            ArrayList<Integer> endPoints = null;
            endPoints = Normal.divide(stApm, stCZ);

            if (endPoints.size() < 2) {
                return null;
            }

            ArrayList<double[]> speechFrames = new ArrayList<>();

            for (int i = 0; i < endPoints.size(); i = i + 2){
                for (int j = endPoints.get(i); j < endPoints.get(i + 1); ++j) {
                    double[] frame = new double[512];
                    System.arraycopy(preEmphasis, 256 * j, frame, 0, 512);
                    Normal.hamming(frame);
                    speechFrames.add(frame);
                }
            }

            double[][] mfcc = new double[speechFrames.size()][NUM_OF_MFCC];
            for (int i = 0; i < speechFrames.size(); ++i) {
                double[] fftData = Mfcc.rFFT(speechFrames.get(i), 512);
                double[] mfccData = Mfcc.mfcc(fftData, 512, NUM_OF_FILTER, NUM_OF_MFCC, VoiceRecorder.mFrequency);
                mfcc[i] = mfccData;
            }
            double[][] dtMfcc = Mfcc.diff(mfcc, 2); //mfcc一阶差分参数
            double[][] result = new double[speechFrames.size()][2 * NUM_OF_MFCC];
            for (int i = 0; i < result.length; i++){ //合并mfcc和一阶差分参数
                System.arraycopy(mfcc[i], 0, result[i], 0, NUM_OF_MFCC);
                System.arraycopy(dtMfcc[i], 0, result[i], NUM_OF_MFCC, NUM_OF_MFCC);
            }
            return result;
        }
    }
}
