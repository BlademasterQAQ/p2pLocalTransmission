package com.blademaster.p2plocaltransmission;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

/**
 * 发送文件线程
 * <p>
 * 该线程将在发送一次信息后结束
 * 在调用<code>start</code>方法开启线程前，应使用<code>setFilePath</code>发送设置发送的文件的绝对路径
 *
 * @see #setFilePath(String)
 *
 * @author blademaster
 */
public class SendFileThread extends Thread{
    private String host;
    private int sendFile_POST;
    private ServerSocket serverSocket;
    private String filePath;//文件路径
    private String ServiceOrClient;//由初始化时只有post和既有host又有post区分是服务端还是客户端

    public SendFileThread(int sendFile_POST){//作为服务端
        this.sendFile_POST =sendFile_POST;
        this.ServiceOrClient="Service";
        try {
            this.serverSocket = new ServerSocket(sendFile_POST);//只需在初始化时设定一次
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SendFileThread(String host, int sendFile_POST){//作为客户端
        this.host=host;
        this.sendFile_POST =sendFile_POST;
        this.ServiceOrClient="Client";
    }

    @Override
    public void run() {
        Socket socket = null;
        try {
            if (ServiceOrClient == "Service") {
                System.out.println("等待客户端连接");
                socket = serverSocket.accept();//在客户端未连接（发送数据）时产生中断（阻塞）
            }

            if (ServiceOrClient == "Client") {
                //建立连接
                System.out.println("等待服务器...");
                socket = new Socket();//在服务端未建立时产生中断（阻塞）
                SocketAddress socketAddress = new InetSocketAddress(host, sendFile_POST);
                socket.connect(socketAddress, 10000);// 10s为超时时间(超时将抛出异常)，如果用socket.connect(socketAddress);将在服务器未建立时产生阻塞，避免产生过多阻塞线程
                //阻塞位置
                System.out.println("建立socket");
            }

            //获取输出流，通过这个流发送消息
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            //发送文字消息
            outputFile(out, filePath);
            out.flush();//强迫输出流(或缓冲的流)发送数据
            out.close();
            socket.close();
            if(ServiceOrClient == "Service") {//当为服务端时，结束线程前应关闭serverSocket，不然下次建立serverSocket会出行端口占用的错误
                serverSocket.close();
            }
        }catch (SocketTimeoutException s){//接收服务器未建立时超时1s的异常处理
            System.out.println("（已超时）接收服务器未建立 receiveServer isn't established");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void outputFile(DataOutputStream out,String FilePath) throws IOException{
        if(FilePath!=null){
            File file=new File(FilePath);
            //发送文件名
            String fileName=file.getName();
            byte[] bytes=fileName.getBytes();
            int byteLength = bytes.length;
            out.writeInt(byteLength);//输出文件名的byte长度
            out.write(bytes);//输出文件名的二进制

            System.out.println("文件是否存在："+file.exists());
            System.out.println("文件是否可读："+file.canRead());
            //发送文件
            ByteArrayOutputStream bout=null;
            try {
                FileInputStream fileInputStream = new FileInputStream(file);//从文件输入到程序中，为输入流（针对程序而言）
                bout = new ByteArrayOutputStream();
                bytes=new byte[1024];
                while(fileInputStream.read(bytes)!=-1) {//将文件数据读入到二进制数组bytes中，没有数据时将返回-1
                    bout.write(bytes);//二进制数组写入输出流缓冲区
                }
                fileInputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("文件错误","文件路径不是绝对路径或者未开启文件读写权限");
            }

            int len = bout.size();
            //这里打印一下发送的长度
            Log.i("sendFile", "len: "+len);
            out.writeInt(len);
            out.write(bout.toByteArray());//由ByteArrayOutputStream获取输出流的二进制
        }
    }

    public void setFilePath(String FilePath) {
        this.filePath = FilePath;
    }

}
