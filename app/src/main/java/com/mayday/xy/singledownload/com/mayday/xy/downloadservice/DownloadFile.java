package com.mayday.xy.singledownload.com.mayday.xy.downloadservice;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.mayday.xy.singledownload.com.mayday.xy.com.mayday.xy.sqlutil.ThreadSqlDao;
import com.mayday.xy.singledownload.com.mayday.xy.toolutils.FileInfo;
import com.mayday.xy.singledownload.com.mayday.xy.toolutils.ThreadInfo;

import org.apache.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * 下载任务类
 * Created by xy-pc on 2017/3/17.
 */

public class DownloadFile {
    private Context context;
    private FileInfo fileInfo;
    private ThreadSqlDao threadDao;
    private long mFinished=0;
    //暂停变量
    boolean isPause=false;

    private static final String TAG=DownloadFile.class.getSimpleName();

    public DownloadFile(Context context,FileInfo fileInfo) {
        this.context = context;
        this.fileInfo=fileInfo;

        threadDao=new ThreadSqlDao(context);
    }

    //开启下载线程方法
    public void download(){
        //读取数据库的线程信息
        List<ThreadInfo> threadInfos = threadDao.selectData(fileInfo.getUrl());

        //如果是第一次下载(数据库中没有信息)
        ThreadInfo threadInfo=null;
        if(threadInfos.size()==0){
            threadInfo=new ThreadInfo(0,fileInfo.getUrl(),0,fileInfo.getLength(),0);
        }else {
            //不是就直接获取第一条，因为我们这里是单线程下载
            threadInfo=threadInfos.get(0);
        }
        new mDownThread(threadInfo).start();
    }

    /**
     * 下载线程
     */
    class mDownThread extends Thread{
        private ThreadInfo mThreadInfo;

        public mDownThread(ThreadInfo threadInfo) {
            this.mThreadInfo = threadInfo;
        }

        @Override
        public void run() {
            HttpURLConnection conn=null;
            InputStream input=null;
            RandomAccessFile raf=null;
            try {
                //如果不存在则向线程数据库中插入线程信息
                if(!threadDao.isExist(mThreadInfo.getUrl(),mThreadInfo.getId())){
                    threadDao.insertData(mThreadInfo);
                }
                //请求网络连接
                URL url=new URL(mThreadInfo.getUrl());
                conn= (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5*1000);
                conn.setRequestMethod("GET");
                //设置当前下载位置
                int start = mThreadInfo.getStart() + mThreadInfo.getFinished();
                //从当前的start到结束
                conn.setRequestProperty("Range","bytes="+start+"-"+mThreadInfo.getEnd());
                //设置文件写入位置
                File file=new File(DownloadService.SDPath,fileInfo.getFileName());
                //随机访问文件的一个类
                raf=new RandomAccessFile(file,"rwd");
                raf.seek(start);

                Intent intent=new Intent(DownloadService.DOWN_FINISHED);
                //获取当前进度(把当前进度通过广播发送给Activity来更新SeekBar)
                mFinished+=mThreadInfo.getFinished();
                //开始下载
                if(conn.getResponseCode()== HttpStatus.SC_PARTIAL_CONTENT){
                    //读取数据
                    input = conn.getInputStream();
                    byte[] buffer=new byte[1024];
                    int len=-1;
                    while((len=input.read(buffer))!=-1){
                        //写入文件
                        raf.write(buffer,0,len);
                        //把下载进度通过广播发送给Activity
                        mFinished+=len;
                        //这里应该把当前完成的进度类型设置为long
                        intent.putExtra("finished",mFinished*100/fileInfo.getLength());
                        context.sendBroadcast(intent);
                        //在下载暂停时，保存下载进度
                        if(isPause){
                            //保存当前进度，并跳出循环
                            threadDao.updatable(mThreadInfo.getId(),mThreadInfo.getUrl(),(int)mFinished);
                            return;
                        }
                    }
                    //下载完成后删除线程信息
                    threadDao.deleData(mThreadInfo.getId(),mThreadInfo.getUrl());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                try {
                    conn.disconnect();
                    input.close();
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
