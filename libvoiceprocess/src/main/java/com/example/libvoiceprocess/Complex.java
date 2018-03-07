package com.example.libvoiceprocess;

/**
 * Created by liangqireng on 2017/11/30.
 */

public class Complex {

    private double m_real; // 实部
    private double m_image;// 虚部

    public Complex(double real, double image) {
        m_real = real;
        m_image = image;
    }

    public void setRealPart(double real) {
        m_real = real;
    }

    public void setImagePart(double image) {
        m_image = image;
    }

    public double getRealPart() {
        return m_real;
    }

    public double getImagePart() {
        return m_image;
    }

    public static Complex add(Complex data1, Complex data2) {
        Complex res = new Complex(data1.getRealPart() + data2.getRealPart(), data1.getImagePart() + data2.getImagePart());
        return res;
    }

    public static Complex mul(Complex data1, Complex data2) {
        Complex res = new Complex(0.0f, 0.0f);
        res.setRealPart(data1.getRealPart() * data2.getRealPart() - data1.getImagePart() * data2.getImagePart());
        res.setImagePart(data1.getRealPart() * data2.getImagePart() + data1.getImagePart() * data2.getRealPart());
        return res;
    }

    public static Complex minus(Complex data1, Complex data2) {
        Complex res = new Complex(data1.getRealPart() - data2.getRealPart(), data1.getImagePart() - data2.getImagePart());
        return res;
    }
}
