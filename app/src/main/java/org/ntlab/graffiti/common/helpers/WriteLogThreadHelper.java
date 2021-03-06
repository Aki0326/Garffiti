package org.ntlab.graffiti.common.helpers;

import android.content.Context;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper to write log thread.
 * @author a-hongo
 */
public class WriteLogThreadHelper extends Thread{
    private Context context;

    public WriteLogThreadHelper(Context context) {
        this.context = context;
    }

    /**
     * Output the line log.
     * @param line
     */
    public void outputLine(String line) {
        PrintWriter writer = null;
        try {
            String date = new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
            OutputStream out;
            out = context.openFileOutput("log" + date  + ".text", Context.MODE_PRIVATE|Context.MODE_APPEND);
            writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
            writer.println(line);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

}
