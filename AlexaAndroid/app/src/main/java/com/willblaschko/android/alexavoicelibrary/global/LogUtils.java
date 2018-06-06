package com.willblaschko.android.alexavoicelibrary.global;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author:gaopan
 * Date:2018/3/19
 */
public class LogUtils {
    private final static String TAG = "LogUtils";

    private static boolean isDebug = true; // log开关

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss EEE");

    public static void setIsDebug(boolean b) {
        isDebug = b;
//        deleteLogFile("/sdcard/","mylog.log");
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static void log(Object msg) {
        log(TAG, msg);
    }

    //需要log文件 使用 adb pull /sdcard/mylog.log 获取log到当前路径
    public static void log(String LOG_CLASS_NAME, Object msg) {
        if (!TextUtils.isEmpty(LOG_CLASS_NAME) && null != msg) {
            if (isDebug) {
                Log.i(LOG_CLASS_NAME, "[INFO " + LOG_CLASS_NAME + "] " + String.valueOf(msg));
//                writeTextToFile(msg.toString(),"/sdcard/","mylog.log");
            }
        }
    }

    private static void deleteLogFile(String filePath,String fileName){
        String strFilePath = filePath + fileName;
        try {
            File file = new File(strFilePath);
            if (file.exists()) {
                file.delete();
            }
        }catch (Exception e){

        }

    }

    //将字符串写入到文本文件中
    public static void writeTextToFile(String strcontent,String filePath,String fileName){
        //先生成文件夹再生成文件，不然会报错
        makeFilePath(filePath,fileName);
        String strFilePath = filePath + fileName;
        //每次写入时都换行
        String strContent = format.format(new Date(System.currentTimeMillis()))+":"+strcontent + "\r\n";
        try {
            File file = new File(strFilePath);
            if (!file.exists()){
                Log.e("TestFile","Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            RandomAccessFile rf = new RandomAccessFile(file,"rwd");
            rf.seek(file.length());
            rf.write(strContent.getBytes());
            rf.close();
        }catch (Exception e){
            Log.e("TestFile","Error on write File:"+e);
        }
    }

    //生成文件
    private static File makeFilePath(String filePath,String fileName){
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath+fileName);
            if (!file.exists()){
                file.createNewFile();
                Log.e("cacacaca","新建文件成功,filePath：" + filePath +"fileName:"+fileName);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return file;
    }

    //生成文件夹
    private static void makeRootDirectory(String filePath){
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()){//判断指定的路径或者指定的目录文件是否已经存在。
                file.mkdir();//建立文件夹
            }
        }catch (Exception e){
            Log.e("error:", e+"");
        }
    }

}


