package com.blademaster.p2plocaltransmission;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

public class ReceiveFileThread extends Thread{
    private String host;
    private int receiveFile_POST;
    private ServerSocket serverSocket;
    private String ServiceOrClient=null;//由初始化时只有post和既有host又有post区分是服务端还是客户端
    private Handler handler_MainActivity;
    private Context context;
    private int file_length;//文件长度
    private int currentLocal = 0;//正在传输的位置
    private int receiveFile_FLAG=5;
    private int updateUI=6;

    public ReceiveFileThread(int receiveFile_POST, Handler handlerMainActivity, Context context){//作为服务端
        this.receiveFile_POST =receiveFile_POST;
        this.ServiceOrClient="Service";
        this.handler_MainActivity =handlerMainActivity;
        this.context=context;
        try {
            this.serverSocket = new ServerSocket(receiveFile_POST);//只需在初始化时设定一次
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ReceiveFileThread(String host, int receiveFile_POST, Handler handlerMainActivity, Context context){//作为客户端
        this.host=host;
        this.receiveFile_POST =receiveFile_POST;
        this.ServiceOrClient="Client";
        this.handler_MainActivity =handlerMainActivity;
        this.context=context;
    }

    @Override
    public void run() {
        Socket socket = null;
        try {
            if (ServiceOrClient == "Service") {
                System.out.println("等待客户端连接");
                if(serverSocket==null){
                    Log.e("serverSocket","serverSocket为空");
                    return;
                }
                socket = serverSocket.accept();//在客户端未连接（发送数据）时产生中断（阻塞）
            }

            if (ServiceOrClient == "Client") {
                //建立连接
                System.out.println("等待服务器...");
                socket = new Socket();//在服务端未建立时产生中断（阻塞）
                SocketAddress socketAddress = new InetSocketAddress(host, receiveFile_POST);
                socket.connect(socketAddress, 1000);// 1s为超时时间(超时将抛出异常)，如果用socket.connect(socketAddress);将在服务器未建立时产生阻塞，避免产生过多阻塞线程
                //阻塞位置
                System.out.println("建立socket");
            }

            //获取输入流，通过这个流读取消息
            DataInputStream input = new DataInputStream(socket.getInputStream());
            //发送文字消息

            inputFile(input);

            //接收完消息，发送信息给MainActivity以进行进一步处理
            handler_MainActivity.sendEmptyMessage(receiveFile_FLAG);

            input.close();
            socket.close();
            if(ServiceOrClient == "Service") {//当为服务端时，结束线程前应关闭serverSocket，不然下次建立serverSocket会出行端口占用的错误
                serverSocket.close();
            }
        }catch (SocketTimeoutException s){//接收服务器未建立时超时1s的异常处理
            System.out.println("接收文件超时");

        } catch (ConnectException c) {
            System.err.println("访问被拒绝（可能被防火墙阻拦）");
            c.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void inputFile(DataInputStream input) throws IOException {
        //读文件名
        int byteLength = input.readInt();
        byte[] bytes = new byte[byteLength];
        input.read(bytes);
        String fileName = new String(bytes, "UTF-8");
        String filePath = Environment.getExternalStorageDirectory().getPath()+"/Download/LocalTransmission/"+fileName;
        //判断路径是否存在，不存在则生成
        String folderPath=filePath.substring(0,filePath.lastIndexOf("/"));
        File folder=new File(folderPath);
        if(!folder.exists()){//文件路径不存在时
            folder.mkdir();//生成路径
        }

        //读文件
        file_length = input.readInt();
        System.out.println("len = " + file_length);
        //为文件创建输出流，输出到文件
        File file = new File(filePath);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        //RandomAccessFile randomFile = new RandomAccessFile(file, "rw");//创建可以到达文件任意位置的文件类
        bytes = new byte[1024];//1k1k的收
        int n;
        while ((n = input.read(bytes))!=-1) {//n为当前读到的byte的长度，若读到末尾，得-1(采用这种方式可以无需得知文件长度，读到末尾时将得到-1)
            fileOutputStream.write(bytes,0,n);//不能使用randomFile.write(bytes)，发送的数据错误（不知是不是b.length不对的原因）
            currentLocal = currentLocal+n;
        }
        fileOutputStream.close();

        System.out.println("ok");

        //通过广播刷新相册
        Uri contentUri = Uri.fromFile(file);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,contentUri);
        context.sendBroadcast(mediaScanIntent);
        //MediaScannerConnection.scanFile(context,new String[]{filePath},new String[]{"image/png"},null);
    }

    /**
     * 获取当前文件的长度
     * @return
     */
    public int getFile_length() {
        return file_length;
    }

    /**
     * 获取正在传输的位置
     * @return
     */

    public int getCurrentLocal() {
        return currentLocal;
    }
}
