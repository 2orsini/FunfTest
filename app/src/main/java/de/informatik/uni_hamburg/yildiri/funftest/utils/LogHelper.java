package de.informatik.uni_hamburg.yildiri.funftest.utils;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This is a utility class to help with everything related to logs
 */
public class LogHelper {

    private static final String LOGCAT_DUMP_LOG_COMMAND = "logcat -d";

    /**
     * Read the logs of this app and write them to the specified file
     * @param outputFile file to write the logs to
     */
    public static void saveLogToFile(File outputFile) {
        BufferedInputStream bis = null;
        FileOutputStream fos = null;

        try {
            Log.d("LogHelper", "Trying to save logs of this app to file " + outputFile.getPath());
            Process process = Runtime.getRuntime().exec(LOGCAT_DUMP_LOG_COMMAND);

            if(!outputFile.exists()) {
                outputFile.getParentFile().mkdir();
                outputFile.createNewFile();
            }
            bis = new BufferedInputStream(process.getInputStream());
            fos = new FileOutputStream(outputFile.getPath());

            int read;
            byte[] buf = new byte[1024];
            while((read = bis.read(buf)) != -1) {
                fos.write(buf, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(fos != null) {
                    fos.flush();
                    fos.close();
                }
                if(bis != null) {
                    bis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
