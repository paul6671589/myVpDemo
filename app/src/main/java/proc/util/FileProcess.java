package proc.util;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by liangqireng on 2018/2/7.
 */

public class FileProcess {

    private String mFileName = null;

    public FileProcess(String fileName) {
        this.mFileName = fileName;
    }

    public boolean fileExist() {
        File file = new File(mFileName);
        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }
}
