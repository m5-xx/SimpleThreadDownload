package com.mayday.xy.singledownload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mayday.xy.singledownload.com.mayday.xy.downloadservice.DownloadService;
import com.mayday.xy.singledownload.com.mayday.xy.toolutils.FileInfo;



public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView downloadProgress;
    private SeekBar seekBar;
    private Button start,pause;
    private FileInfo fileinfo;

    public static final String KGMusicPath="http://download.kugou.com/download/kugou_pc";
    public static final String DownloadFileName="kugou8141.exe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化控件
        initControl();
        seekBar.setMax(100);
        //初始化FielInfo
        initFileinfo();
        //注册广播接收器
        IntentFilter filter=new IntentFilter();
        filter.addAction(DownloadService.DOWN_FINISHED);
        registerReceiver(mReceiver,filter);

    }

    private void initFileinfo() {
        fileinfo=new FileInfo(0,KGMusicPath,DownloadFileName,0,0);
    }

    private void initControl() {
        downloadProgress= (TextView) findViewById(R.id.downloadProgress);
        seekBar= (SeekBar) findViewById(R.id.seekBar);
        start= (Button) findViewById(R.id.start);
        pause= (Button) findViewById(R.id.pause);

        start.setOnClickListener(this);
        pause.setOnClickListener(this);
    }

    /**
     * Activtiy----------->Service
     * 将我们的FileInfo的信息传给Service进行处理
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.start:
                Intent intent=new Intent(MainActivity.this, DownloadService.class);
                intent.setAction(DownloadService.DOWN_START);
                intent.putExtra("fileinfo",fileinfo);
                startService(intent);
                break;

            case R.id.pause:
                Intent intent1=new Intent(MainActivity.this, DownloadService.class);
                intent1.setAction(DownloadService.DOWN_STOP);
                intent1.putExtra("fileinfo",fileinfo);
                startService(intent1);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭广播
        unregisterReceiver(mReceiver);
    }

    /**
     * 定义广播接收器
     * 通过传过来的参数进行更新seekbar的进度
     */
    BroadcastReceiver mReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(DownloadService.DOWN_FINISHED.equals(intent.getAction())){
//                long finished = intent.getIntExtra("finished", 0);
//                seekBar.setProgress((int)finished);
                long finished = intent.getLongExtra("finished", 0l);
                seekBar.setProgress((int)finished);
                //下载完成后弹出提示信息
                if((int)finished==100){
                    Toast.makeText(context,"下载完成",Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

}
