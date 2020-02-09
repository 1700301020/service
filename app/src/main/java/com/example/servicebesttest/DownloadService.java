package com.example.servicebesttest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.util.Objects;

public class DownloadService extends Service {

    private DownloadTask downloadTask;
    private String downloadURL;

    private static final String channelId="1";//全局唯一
    private static final String channelName="First Notification";//渠道名称
    private static final String channelDescription="First Notification Channel";//设置描述

    private DwonloadListener listener=new DwonloadListener() {//匿名类实例
        @Override
        public void onProgress(int Progress) {//构件显示下载进度的通知
            getNotificationManager().notify(1,getNotification("Downloading...",Progress));
        }

        @Override
        public void onSuccess() {
            downloadTask=null;
            //下载成功时将前台服务通知关闭，并创建一个下载成功的通知
            stopForeground(true);
            //创建新的通知通知下载成功
            getNotificationManager().notify(1,getNotification("Download Success",-1));
            Toast.makeText(DownloadService.this,"Download Success",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            downloadTask=null;
            //下载失败时将前台服务通知关闭，并创建一个下载失败的通知
            stopForeground(true);
            //创建新的通知通知下载失败
            getNotificationManager().notify(1,getNotification("Download Failed",-1));
            Toast.makeText(DownloadService.this,"Download Failed",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPause() {
            downloadTask=null;
            //创建新的通知通知下载暂停
            Toast.makeText(DownloadService.this,"Download Paused",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downloadTask=null;
            //下载失败时将前台服务通知取消，并创建一个下载取消的通知
            stopForeground(true);
            //创建新的通知通知下载取消
            Toast.makeText(DownloadService.this,"Download Canceled",Toast.LENGTH_SHORT).show();
        }
    };

    private DownloadBinder mBinder=new DownloadBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    class DownloadBinder extends Binder{

        public void startDownload(String url){
            if(downloadTask==null){
                downloadURL=url;
                downloadTask=new DownloadTask(listener);
                downloadTask.execute(downloadURL);//开启下载
                startForeground(1,getNotification("Download...",0));//创建下载通知
                Toast.makeText(DownloadService.this,"Download...",Toast.LENGTH_SHORT).show();
            }
        }

        public void pauseDownload(){
            if(downloadTask!=null){
                downloadTask.pauseDownload();
            }
        }

        public void cancelDownload(){
            if(downloadTask!=null){
                downloadTask.cancelDownload();
            }
            if(downloadURL!=null){
                //取消下载时需将文件删除，并通知关闭
                String fileName=downloadURL.substring(downloadURL.lastIndexOf("/"));
                String directory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                File file=new File(directory+fileName);
                if (file.exists()){
                    file.delete();
                }
                getNotificationManager().cancel(1);
                stopForeground(true);
                Toast.makeText(DownloadService.this,"Canceled",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private NotificationManager getNotificationManager(){
        return (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title,int progress){
        Intent intent=new Intent(this,MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder=new NotificationCompat.Builder(getApplicationContext(), Objects.requireNonNull(createNotificationChannel(this)));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(title);
        if(progress>=0){
            //当progress大于或者等于0时才需显示下载进度
            builder.setContentText(progress+"%");
            builder.setProgress(100,progress,false);
        }
        return builder.build();
    }

    public static String createNotificationChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            //要显示横幅通知 ，NotificationChannel在创建的时候第三个参数还要设置成NotificationManager.IMPORTANCE_HIGH，不然也弹不出来
            NotificationChannel notificationChannel=new NotificationChannel(channelId,channelName,NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(channelDescription);
            NotificationManager notificationManager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
            return channelId;
        }else{
            return null;
        }
    }
}
