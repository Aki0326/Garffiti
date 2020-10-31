package org.ntlab.graffiti.common.helpers;

import android.content.Context;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;


public class WriteLogThreadHelpers extends Thread{
    private Context context;

    public WriteLogThreadHelpers(Context context) {
        this.context = context;
    }

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