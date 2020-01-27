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
import java.util.ArrayList;

public class ReceiveFilesThread extends Thread{
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

    public ReceiveFilesThread(int receiveFile_POST, Handler handlerMainActivity, Context context){//作为服务端
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

    public ReceiveFilesThread(String host, int receiveFile_POST, Handler handlerMainActivity, Context context){//作为客户端
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
        //读文件数量
        int fileCount = input.readInt();

        //读文件名
        ArrayList<String> filesName = new ArrayList<>(0);
        for(int i=0;i<fileCount;i++) {
            int byteLength = input.readInt();
            byte[] bytes = new byte[byteLength];
            input.readFully(bytes);
            String str = new String(bytes, "UTF-8");
            filesName.add(str);
        }

        //读文件大小
        ArrayList<Integer> filesLength = new ArrayList<>(0);
        for(int i=0;i<fileCount;i++){
            filesLength.add(input.readInt());
        }

        //读文件最后修改日期
        ArrayList<Long> fileLastModified = new ArrayList<>(0);
        for(int i=0;i<fileCount;i++){
            fileLastModified.add(input.readLong());//输出文件的长度
        }

        //读文件路径（用于回传时复原）
        ArrayList<String> filesPath = new ArrayList<>(0);
        for(int i=0;i<fileCount;i++){
            int byteLength = input.readInt();
            byte[] bytes = new byte[byteLength];
            input.readFully(bytes);
            String str = new String(bytes, "UTF-8");
            if(str.equals("null")){
                str = null;
            }
            filesPath.add(str);
        }
        // TODO: 2020/1/26  更新接收文件后显示的路径
        //读文件
        for(int i=0;i<fileCount;i++) {
            file_length = filesLength.get(i);//第i个文件的大小
            System.out.println("len = " + file_length);
            System.out.println("文件名：" + filesName.get(i)+"第"+i+"个");
            //为文件创建输出流，输出到文件
            File file;
            if(filesPath.get(i) != null){
                file = new File(filesPath.get(i));//已包含.png
                System.err.println(filesPath.get(i));
            }else {
                file = new File(Environment.getExternalStorageDirectory().getPath()+"/Download/LocalTransmission/"+filesName.get(i));//已包含.png
            }
            if(!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();//创建父文件夹（们）
                //file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);//从程序输出到文件中

            currentLocal = 0;
            while(currentLocal<filesLength.get(i)-1024) {
                byte[] bytes=new byte[1024];//1M1M的发
                input.readFully(bytes);//读输入流1M，用readFully读取的大小确实是bytes的大小
                fileOutputStream.write(bytes,0,1024);//写文件
                currentLocal = currentLocal+1024;
            }
            byte[] bytes=new byte[filesLength.get(i)-currentLocal];//剩余小于等于1024的部分
            input.readFully(bytes);//读输入流1M
            fileOutputStream.write(bytes,0,filesLength.get(i)-currentLocal);//写剩余文件
            currentLocal = filesLength.get(i);

            fileOutputStream.close();//关闭文件流，文件写入完成

            if(fileLastModified.get(i) != 0) {
                file.setLastModified(fileLastModified.get(i));//修改文件的最后修改时间
            }

            //通过广播刷新相册
            Uri contentUri = Uri.fromFile(file);
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,contentUri);
            context.sendBroadcast(mediaScanIntent);
            //MediaScannerConnection.scanFile(context,new String[]{filePath},new String[]{"image/png"},null);

            System.out.println("ok");
        }


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
