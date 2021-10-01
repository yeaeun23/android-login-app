package kr.co.seoul;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

public class getAPKThread extends Thread {
    private Context mContext;

    getAPKThread(Context mContext) {
        this.mContext = mContext;
    }

    @SuppressLint("NewApi")
    public void run() {
        deleteFile(Environment.getExternalStorageDirectory() + "/download/mobilesis.apk");

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse("https://mgate.seoul.co.kr/AppDown.aspx"));
        request.setTitle("모바일SIS");
        request.setDescription("앱 업데이트 중..");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "mobilesis.apk");

        ((DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
    }

    private boolean deleteFile(String fname) {
        File file = new File(fname);
        String[] fileList;

        if (file.isDirectory()) {
            fileList = file.list();

            for (String list : fileList) {
                deleteFile(fname + "/" + list);
            }
        }

        if (file.exists()) {
            return file.delete();
        } else {
            return false;
        }
    }
}