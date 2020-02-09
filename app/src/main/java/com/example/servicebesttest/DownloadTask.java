package com.example.servicebesttest;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String,Integer,Integer> {

    public static final int TYPE_SUCCESS=0;
    public static final int TYPE_FAILED=1;
    public static final int TYPE_PAUSED=2;
    public static final int TYPE_CANCELED=3;

    private DwonloadListener listener;
    private boolean isCanceled=false;
    private boolean isPaused=false;
    private int lastProgress;

    public DownloadTask(DwonloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... strings) {

        InputStream is=null;
        RandomAccessFile savedFile=null;
        File file=null;

        try {
            long downloadedLength=0;//记录已下载的文件长度
            String downloadUrl=strings[0];//获取参数的URL地址
            String fileName=downloadUrl.substring(downloadUrl.lastIndexOf("/"));//URL地址解析出下载的文件名
            String directory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();//将文件下载到SD卡的Download目录下
            file=new File(directory+fileName);
            if (file.exists()){//判断文件是否已经存在
                downloadedLength=file.length();//读取已下载的字节数
            }
            long contentLength=getContentLength(downloadUrl);//获取待下载的文件总长度
            if (contentLength==0){
                //如果文件长度为零则说明文件有问题，返回错误
                return TYPE_FAILED;
            }else if (contentLength==downloadedLength){
                //已下载字节和文件总字节相等，说明已经下载完成
                return TYPE_SUCCESS;
            }
            //使用OKHttp发送网络请求
            OkHttpClient client=new OkHttpClient();
            Request request=new Request.Builder()
                    //定义header，断点下载，指定从哪个字节开始下载
                    .addHeader("RANGE","bytes="+downloadedLength+"-")
                    .url(downloadUrl)
                    .build();
            //使用Java文件流的形式读取服务器数据
            Response response=client.newCall(request).execute();
            if (response!=null){
                is=response.body().byteStream();
                savedFile=new RandomAccessFile(file,"rw");
                savedFile.seek(downloadedLength);//跳过已下载的字节
                byte[] b=new byte[1024];
                int total=0;
                int len;
                while ((len=is.read(b))!=-1){
                    //判断用户是否取消
                    if (isCanceled){
                        return TYPE_CANCELED;
                    }else if (isPaused){
                        //判断用户是否暂停
                        return TYPE_PAUSED;
                    }else {
                        //计算当前的下载速度
                        total+=len;
                        savedFile.write(b,0,len);
                        //计算已下载的百分比
                        int progress=(int)((total+downloadedLength)*100/contentLength);
                        //进行通知
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //关闭操作
            try {
                if (is!=null){
                    is.close();
                }
                if (savedFile!=null){
                    savedFile.close();
                }
                if (isCanceled && file!=null){
                    file.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress=values[0];//从参数中获取当前下载进度
        if(progress>lastProgress){//与上一次进度进行比较
            listener.onProgress(progress);//通知下载进度更新
            lastProgress=progress;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer){
            case TYPE_SUCCESS:
                listener.onSuccess();;
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPause();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            default:
                break;
        }
    }

    //暂停和取消操作都是使用一个布尔变量来进行控制，调用以下两种方法
    public void pauseDownload(){
        isPaused=true;
    }

    public void cancelDownload(){
        isCanceled=true;
    }

    private long getContentLength(String downloadUrl) throws IOException{
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response=client.newCall(request).execute();
        if (response!=null && response.isSuccessful()){
            long contentLength=response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }
}
