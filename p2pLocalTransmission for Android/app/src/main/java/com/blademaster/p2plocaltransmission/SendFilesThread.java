package com.blademaster.p2plocaltransmission;

import android.os.Handler;

import androidx.documentfile.provider.DocumentFile;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * 发送文件线程
 * <p>
 * 该线程将在发送一次信息后结束
 * 在调用<code>start</code>方法开启线程前，应使用<code>setFilePath</code>发送设置发送的文件的绝对路径
 *
 * @see #setFilesPath(ArrayList)
 *
 * @author blademaster
 */
public class SendFilesThread extends Thread{
    private String host;
    private int sendFile_POST;
    private ServerSocket serverSocket;
    private ArrayList<String> filesPath;//文件路径
    private String ServiceOrClient;//由初始化时只有post和既有host又有post区分是服务端还是客户端
    private Handler handler_MainActivity;
    private int sendFile_FLAG =4;//接受标志

    //用于UI显示
    private int currentLocal;//传输某个文件的当前位置
    private int file_length;//传输某个文件的长度
    private int currentFileCount;//当前传输到第几个文件

    public SendFilesThread(int sendFile_POST, Handler handlerMainActivity) {//作为服务端
        this.sendFile_POST = sendFile_POST;
        this.ServiceOrClient = "Service";
        try {
            this.serverSocket = new ServerSocket(sendFile_POST);//只需在初始化时设定一次
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.handler_MainActivity=handlerMainActivity;
    }

    public SendFilesThread(String host, int sendFile_POST, Handler handlerMainActivity) {//作为客户端
        this.host = host;
        this.sendFile_POST = sendFile_POST;
        this.ServiceOrClient = "Client";
        this.handler_MainActivity=handlerMainActivity;
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
                long start_time = System.currentTimeMillis();//开始时间
                boolean i = true;
                while (i) {
                    try {
                        socket = new Socket();//在服务端未建立时产生中断（阻塞）
                        SocketAddress socketAddress = new InetSocketAddress(host, sendFile_POST);
                        socket.connect(socketAddress, 10000);// 10s为超时时间(超时将抛出异常)，如果用socket.connect(socketAddress);将在服务器未建立时产生阻塞，避免产生过多阻塞线程
                        i = false;
                    } catch (ConnectException c) {
                        System.err.println("ConnectException:等待服务器");
                        System.err.println("\tat"+Thread.currentThread().getStackTrace()[1]);//输出错误位置
                        if(System.currentTimeMillis()-start_time > 10000){//超时10000ms后
                            throw new SocketTimeoutException();//抛出超时异常
                        }
                    }

                }

                //阻塞位置
                System.out.println("建立socket");
            }

            //获取输出流，通过这个流发送消息
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            //发送文件
            outputFile(out, filesPath);

            //发送完文件，发送信息给MainActivity以进行进一步处理
            handler_MainActivity.sendEmptyMessage(sendFile_FLAG);

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

    private void outputFile(DataOutputStream out,ArrayList<String> filesPath) throws IOException{
        ArrayList<File> files = new ArrayList<>(0);
        if(filesPath!=null && filesPath.size()!=0){
            for(int i=0;i<filesPath.size();i++){
                File file=new File(filesPath.get(i));
                if(file.exists()){
                    files.add(file);
                }
            }

            //发送文件数量
            out.writeInt(files.size());

            //发送文件名
            for(int i=0;i<files.size();i++){
                byte[] bytes = files.get(i).getName().getBytes();
                int byteLength = bytes.length;
                out.writeInt(byteLength);//输出文件名的byte长度
                out.write(bytes);//输出文件名的二进制
            }

            //发送文件大小
            for(int i=0;i<files.size();i++){
                out.writeInt((int)files.get(i).length());//输出文件的长度
            }

            //发送文件最后修改日期
            for(int i=0;i<files.size();i++){
                out.writeLong(files.get(i).lastModified());//输出文件的长度
            }

            //发送文件路径（用于回传时复原）
            for(int i=0;i<files.size();i++){
                byte[] bytes = files.get(i).getPath().getBytes();
                int byteLength = bytes.length;
                out.writeInt(byteLength);//输出文件名的byte长度
                out.write(bytes);//输出文件名的二进制
            }

            //发送文件
            for(int i=0;i<files.size();i++) {
                file_length = (int)files.get(i).length();//获取文件长度
                FileInputStream fileInputStream = new FileInputStream(files.get(i));//获取文件输入流
                byte[] bytes = new byte[1024];//1k1k的发
                int n;
                currentLocal = 0;
                while ((n = fileInputStream.read(bytes)) != -1) {//将文件数据读入到二进制数组bytes中，没有数据时将返回-1
                    try {
                        out.write(bytes, 0, n);//二进制数组写入输出缓冲区，当缓冲区以满时，此处将产生阻塞，直到客户端使用read接收
                    } catch (SocketException s) {
                        if (s.getMessage().equals("Connection reset by peer: socket write error")) {//当对方取消接受时触发
                            System.err.println("对方主动停止连接");
                            s.printStackTrace();
                        }
                        break;//跳出while循环
                    }
                    currentLocal = currentLocal + n;//当前传输的数据量
                }
                fileInputStream.close();
                currentFileCount = i+1;
            }
        }
    }

    /**
     * 设置文件的路径
     * @return
     */
    public void setFilesPath(ArrayList<String> filesPath) {
        this.filesPath = filesPath;
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
