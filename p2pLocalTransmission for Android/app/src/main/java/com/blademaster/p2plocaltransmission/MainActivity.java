package com.blademaster.p2plocaltransmission;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView textView_path;
    private String filePath;
    private ArrayList<Uri> fileUri = new ArrayList<>(0);//文件Uri，由intend获取
    private EditText editText;
    private ReceiveMessageLoopThread receiveMessageThread;//接受线程

    //标志
    private int PERMISSION_REQUEST_FLAG =1;
    private int FILE_SELECT_FLAG =2;
    private int receiveMessage_FLAG=3;//接受信息标志
    private int sendFile_FLAG=4;//发送文件标志
    private int receiveFile_FLAG=5;//接收文件标志

    //socket变量
    private String host="0.0.0.0";
    private int message_POST =30001;//常开
    private int file_POST =30002;
    /**
     * @declare 在p2p网络中，每台设备都可以作为服务器和客户端，由于应在发送数据时选择接受方而不是在接收
     * 时选择发送端（后者会出现先接收文件再根据文件内容分析是否接受的情况，且发送端开启端口相当于广播，
     * 任何有发送端ip地址的设备都可以接收数据，在单服务器多客户端模式中会出现这种情况），所以双方都应该
     * 开启相同的接收端口，发送时选择ip，端口即用接受端口
     */

    private Handler handler_MainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //需要的权限
        String[] PermissionName={Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION};//源码中Manifest.permission.储存权限的String字符串“android.permission.”，Manifest.permission.本身就一个String对象
        //获取权限
        getSystemPermission getSystemPermission=new getSystemPermission(MainActivity.this,PermissionName, PERMISSION_REQUEST_FLAG);

        final TextView textView_WiFiState=findViewById(R.id.textView_WiFiState);
        final TextView textView_WiFiName=findViewById(R.id.textView_WiFiName);
        final TextView textView_WiFiIP=findViewById(R.id.textView_WiFiIP);
        final TextView textView_MobileIP=findViewById(R.id.textView_MobileIP);
        final TextView textView_process=findViewById(R.id.textView_process);
        final TextView textView_realpath=findViewById(R.id.textView_realpath);
        final Switch switch_time=findViewById(R.id.switch_time);
        switch_time.setChecked(true);

        Button select_button = findViewById(R.id.select_button);
        textView_path=findViewById(R.id.textView_path);
        Button send_button = findViewById(R.id.send_button);
        editText=findViewById(R.id.editText);

        select_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);//允许多选
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, FILE_SELECT_FLAG);
            }
        });

        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host = "192.168.0.103";//临时
                if (fileUri != null) {
                    SendMessageThread sendMessageThread = new SendMessageThread(host, message_POST);
                    sendMessageThread.setMessage("请求发送文件");
                    sendMessageThread.start();

                    if(!switch_time.isChecked()) {
                        //开启发送文件线程
                        final SendFilesByInputStream sendFilesByInputStream = new SendFilesByInputStream(host, file_POST, handler_MainActivity);
                        //解析文件输入流
                        ArrayList<InputStream> fileInputStream = new ArrayList<>(0);
                        try {
                            for (int i = 0; i < fileUri.size(); i++) {
                                fileInputStream.add(getContentResolver().openInputStream(fileUri.get(i)));//由Uri获取文件流
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        sendFilesByInputStream.setFilesInputStream(fileInputStream);//设置文件输入流
                        //解析文件名
                        ArrayList<String> filesName = new ArrayList<>(0);

                        for (int i = 0; i < fileUri.size(); i++) {
                            filesName.add(Uri2File.Uri2Filename(MainActivity.this, fileUri.get(i)));
                        }
                        sendFilesByInputStream.setFilesName(filesName);//设置文件名
                        // TODO: 2020/1/20 提供可修改文件名的功能（根据editText获取最终发送的文件名）
                        //String path=MainActivity.this.getApplicationContext().getFilesDir().toString()+"/basedata.txt";
                        //sendFileThread.setFilePath(filePath);
                        sendFilesByInputStream.start();

                        new Thread(new Runnable() {//当文件正在发送时
                            @Override
                            public void run() {
                                while (sendFilesByInputStream.getCurrentLocal() == 0 || sendFilesByInputStream.getFile_length() > sendFilesByInputStream.getCurrentLocal()) {
                                    textView_process.setText("正在发送：" + sendFilesByInputStream.getCurrentLocal() + "/" + sendFilesByInputStream.getFile_length());
                                }
                                textView_process.setText("");
                            }
                        }).start();
                    }else{
                        //同步修改时间进行发送
                        final SendFilesThread sendFilesThread = new SendFilesThread(host, file_POST, handler_MainActivity);
                        //解析文件名
                        ArrayList<String> filesName = new ArrayList<>(0);
                        Uri2RealPath uri2RealPath = new Uri2RealPath();
                        for (int i = 0; i < fileUri.size(); i++) {
                            filesName.add(uri2RealPath.getFilePathByUri(MainActivity.this,fileUri.get(i)));//由Uri获取文件流
                        }
                        sendFilesThread.setFilesPath(filesName);
                        sendFilesThread.start();

                        new Thread(new Runnable() {//当文件正在发送时
                            @Override
                            public void run() {
                                while (sendFilesThread.getCurrentLocal() == 0 || sendFilesThread.getFile_length() > sendFilesThread.getCurrentLocal()) {
                                    textView_process.setText("正在发送：" + sendFilesThread.getCurrentLocal() + "/" + sendFilesThread.getFile_length());
                                }
                                textView_process.setText("");
                            }
                        }).start();
                    }
                }
            }
        });

        handler_MainActivity=new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //接收到信息
                if(msg.what == receiveMessage_FLAG) {
                    String message = receiveMessageThread.getMessage();
                    host = receiveMessageThread.getConnecterIP();//用于回复“成功接收文件”

                    //接收文件部分
                    if(message.equals("请求发送文件")) {//接收到对方"请求发送文件"时，准备接收文件
                        Toast.makeText(MainActivity.this,"开始接收文件",Toast.LENGTH_LONG).show();
                        //等待接收
                        final ReceiveFilesThread receiveFilesThread=new ReceiveFilesThread(file_POST,handler_MainActivity,MainActivity.this);
                        receiveFilesThread.start();

                        new Thread(new Runnable() {//当文件正在接受时
                            @Override
                            public void run() {
                                while (receiveFilesThread.getCurrentLocal()==0||receiveFilesThread.getFile_length()>receiveFilesThread.getCurrentLocal()){
                                    textView_process.setText("正在接收："+receiveFilesThread.getCurrentLocal()+"/"+receiveFilesThread.getFile_length());
                                }
                                textView_process.setText("");
                            }
                        }).start();

                    }
                    //发送文件部分
                    if(message.equals("成功接收文件")) {//接收到对方"成功接收文件"时，显示成功发送
                        Toast.makeText(MainActivity.this,"成功发送文件",Toast.LENGTH_LONG).show();
                    }
                }

                if(msg.what == sendFile_FLAG){
                    Toast.makeText(MainActivity.this,"开始发送文件",Toast.LENGTH_LONG).show();
                }
                //接收到文件
                if(msg.what == receiveFile_FLAG) {
                    Toast.makeText(MainActivity.this,"成功接收文件，保存至："+Environment.getExternalStorageDirectory().getPath()+"/Download/LocalTransmission/",Toast.LENGTH_LONG).show();
                    SendMessageThread sendMessageThread=new SendMessageThread(host,message_POST);//host应从前面的接收线程获取（为请求发送消息的主机的ip）
                    sendMessageThread.setMessage("成功接收文件");
                    sendMessageThread.start();
                }
            }
        };
/**
 * **************************线 程 区****************************
 */
        //循环接收线程
        //由于此线程持续运行，当屏幕翻转等操作时将重新启动Activity（运行onCreate方法），此时需要判断是否为第一次启动Activity（第一次启动Activity时保存Activity状态的参数savedInstanceState为null），不然会多次对同一个端口建立socket，因此报错  （最好的方法应该是在onDestroy方法中先关闭端口，实测发现“屏幕翻转”和“后台运行时被分享调用”时都会调用onDestroy方法）
        receiveMessageThread = new ReceiveMessageLoopThread(message_POST, handler_MainActivity);
        receiveMessageThread.start();


        //获取WiFi信息线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                MobileWiFiInfo mobileWiFiInfo = new MobileWiFiInfo();
                while(true){
                    mobileWiFiInfo.getMobileWiFiInfo(MainActivity.this);
                    //WiFi判断
                    if(mobileWiFiInfo.isWiFiOpened()){
                        if(mobileWiFiInfo.isWiFiConnected()){
                            textView_WiFiState.setText("WiFi已连接");
                            textView_WiFiName.setText(mobileWiFiInfo.getWiFiName());
                            textView_WiFiIP.setText(mobileWiFiInfo.getWiFiIP());
                            textView_MobileIP.setText(mobileWiFiInfo.getMobileIP());
                            host=mobileWiFiInfo.getWiFiIP();//获取所连客户端的ip
                            receiveMessageThread.setHost(host);//更新host
//                            try {
//                                InetAddress inetAddress = InetAddress.getByName("192.168.0.101");
//                                System.err.println(inetAddress.isReachable(1000));
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
                            //System.err.println(mobileWiFiInfo.getConnectedHotspotIP().get(2));
                        }
                        else {
                            textView_WiFiState.setText("WiFi未连接");
                            textView_WiFiName.setText("");
                            textView_WiFiIP.setText("");
                            textView_MobileIP.setText("");
                            host="0.0.0.0";
                        }
                    }
                    else {
                        textView_WiFiState.setText("WiFi未开启");
                        textView_WiFiName.setText("");
                        textView_WiFiIP.setText("");
                        textView_MobileIP.setText("");
                    }
                    //热点判断
                    if(mobileWiFiInfo.isHotspotOpened()){
                        if(mobileWiFiInfo.isHotspotConnected()){
                            textView_WiFiState.setText("热点已连接");
                            textView_WiFiName.setText("");
                            textView_WiFiIP.setText("ip:"+mobileWiFiInfo.getConnectedHotspotIP().get(1));
                            textView_MobileIP.setText("热点连接数:"+mobileWiFiInfo.getHotspotConnectedCount());
                            host=mobileWiFiInfo.getConnectedHotspotIP().get(1);//第一个连接的ip
                            receiveMessageThread.setHost(host);//更新host
                        }
                        else {
                            textView_WiFiState.setText("热点未连接");
                            textView_WiFiName.setText("");
                            textView_WiFiIP.setText("");
                            textView_MobileIP.setText("");
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        //**********************接收分享区***************************
        Intent intent = getIntent();//返回启动本activity的intent（目的、意图），可以根据目的做进一步操作
        String intent_action = intent.getAction();//获取动作信息，即是传单个文件还是多个文件
        String intent_type = intent.getType();//获取MIME type，即文件的类型

        if (Intent.ACTION_SEND.equals(intent_action) && intent_type != null) {//intent为发送单个文件时
            fileUri.clear();
            fileUri.add((Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM));//获取单个Uri
            textView_path.setText(fileUri.get(0).getPath());
            editText.setText(Uri2File.Uri2Filename(MainActivity.this,fileUri.get(0)));
        }
        else if (Intent.ACTION_SEND_MULTIPLE.equals(intent_action) && intent_type != null) {//intent为发送多个文件时
            fileUri = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);//返回文件Uri组成的ArrayList
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == FILE_SELECT_FLAG) {
                fileUri.clear();
                fileUri.add(data.getData());
                //Toast.makeText(this, "文件路径："+uri.getPath().toString(), Toast.LENGTH_SHORT).show();
                //filePath = Uri2File.getFilePathByUri(MainActivity.this,uri);
                //filePath = "/storage/emulated/0/"+uri.getLastPathSegment().substring(8);
                //filePath = "/storage/emulated/0/Download/QQMail/Simulink基本模块介绍.pdf";
                textView_path.setText(fileUri.get(0).toString());//显示文件路径
                editText.setText(Uri2File.Uri2Filename(MainActivity.this,fileUri.get(0)));

                /*try {
                    System.out.println(readTextFromUri(uri));
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

                /*File file=new File(uri.getPath());
                System.out.println(file.getName());
                System.out.println(file.getAbsoluteFile().getName());*/
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        receiveMessageThread.Stop();//关闭被占用的端口
        //也可以先获取receiveMessageThread的serverSocket并关闭，之后通过receiveMessageThread.close()关闭线程来结束循环，效果一个样
    }
}
