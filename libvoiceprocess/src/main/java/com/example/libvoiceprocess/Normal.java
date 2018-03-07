package com.example.libvoiceprocess;

/**
 * Created by liangqireng on 2017/11/30.
 */

import java.util.ArrayList;

public class Normal {

    // 输出数组最大值
    public static double getMaxVal(double[] array) {
        if (array.length == 0) {
            return 0.0f;
        }

        double max = array[0];
        for (double t:array) {
            if (t > max) {
                max = t;
            }
        }
        return max;
    }

    // 高通滤波，预加重
    public static double[] highpass(double[] array) {
        double[] res = new double[array.length];
        res[0] = array[0];
        for (int i = 1; i < array.length; i
                ++) {
            res[i] = array[i] - 0.945 * array[i - 1];
        }
        return res;
    }

    // 计算短时能量，50%的overlap，重叠个数为256
    public static double[] shortTernEnergy(double[] array) {
        int div = 512;
        int num_div = (int)(array.length * 2 / div) - 1;
        double[] st_amp = new double[num_div];
        for (int i = 0; i < num_div; i++) {
            double sum = 0.0;
            for (int j = 0; j < div; j++) {
                sum += Math.abs(array[div * i / 2 + j]);
            }
            st_amp[i] = sum;
        }
        return st_amp;
    }

    // 短时过零率信号，50%的overlap，重叠个数为256
    public static double[] shortTernCZ(double[] array, int samplerate) {
        int div = 512;
        int num_div = (int)(array.length * 2 / div) - 1;
        double[] st_cz = new double[num_div];
        for (int i = 0; i < num_div; i++) {
            double sum = 0.0;
            for (int j = 1; j < div; j++) {
                sum += 0.5 * (Math.abs(Math.signum(array[div * i / 2 + j] - 0.005) - Math.signum(array[div * i / 2 + j - 1] - 0.005))
                        + Math.abs(Math.signum(array[div * i / 2 + j] + 0.005) - Math.signum(array[div * i / 2 + j - 1] + 0.005)));
            }
            st_cz[i] = sum;
        }
        return st_cz;
    }

    // 汉明窗
    public static void hamming(double[] array) {
        int len = array.length;
        for (int i = 0; i < len; i++) {
            array[i] *= (0.54 - 0.46 * Math.cos(2 * Math.PI * i / (len - 1)));
        }
    }

    /**
     * 端点检测
     * @param stEnergy 短时能量信号
     * @param stCZ 短时过零率信号
     * @return 包含端点值的list，奇偶下标分别对应语音段的首尾值
     */
    public static ArrayList<Integer> divide(double[] stEnergy, double[] stCZ) {
        double maxEnergy = getMaxVal(stEnergy);
        double amp1 = Math.max(0.8, maxEnergy / 10); //短时能量门限值ֵ
        double amp2 = Math.max(1.6, maxEnergy / 5);

        double maxCZ = getMaxVal(stCZ);
        double zcr1 = maxCZ / 15; //短时过零率门限值
        double zcr2 = maxCZ / 8;

        int status = 0;
        int count = 0;
        int silence = 0;

        int minLength = 18;
        int maxSilence = 15;

        ArrayList<Integer> divPoints = new ArrayList<>();

        for (int i = 0; i < stEnergy.length; ++i) {
            switch (status) {
                case 0:
                    if (stEnergy[i] > amp2 || stCZ[i] > zcr2) {
                        status = 2;
                        divPoints.add(Math.max(i - count - 1, 0));
                        ++count;
                        silence = 0;
                    }
                    else if (stEnergy[i] > amp1 || stCZ[i] > zcr1) {
                        status = 1;
                        ++count;
                    }
                    else {
                        status = 0;
                        count = 0;
                    }
                    break;
                case 1:
                    if (stEnergy[i] > amp2 || stCZ[i] > zcr2) {
                        status = 2;
                        divPoints.add(Math.max(i - count, 0));
                        ++count;
                        silence = 0;
                    }
                    else if (stEnergy[i] > amp1 || stCZ[i] > zcr1) {
                        status = 1;
                        ++count;
                    }
                    else {
                        status = 0;
                        count = 0;
                    }
                    break;
                case 2:
                    if (stEnergy[i] > amp1 || stCZ[i] > zcr1) {
                        status = 2;
                        silence = 0;
                        ++count;
                    }
                    else if (count < minLength) {
                        status = 0;
                        silence = 0;
                        count = 0;
                        divPoints.remove(divPoints.size() - 1);
                    }
                    else {
                        silence++;
                        if (silence < maxSilence)
                            ++count;
                        else
                            status = 3;
                    }
                    break;
                case 3:
                    divPoints.add(i - silence/2);
                    status = 0;
                    silence = 0;
                    count = 0;
                    break;
            }

        }
        if ((divPoints.size() & 0x01) == 1 && status == 2){
            divPoints.add(stEnergy.length - silence/2 - 1);
        }

        return divPoints;
    }
}
