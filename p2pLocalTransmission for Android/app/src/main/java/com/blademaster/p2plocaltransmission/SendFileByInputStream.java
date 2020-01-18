package com.blademaster.p2plocaltransmission;
import android.os.Handler;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Timer;

/**
 * 发送文件线程
 * <p>
 * 该线程将在发送一次信息后结束
 * 在调用<code>start</code>方法开启线程前，应使用 {@link #setFileInputStream(InputStream)}方法设置发送的文件的输入流，使用{@link #setFileName(String)}设置文件名
 *
 * @author blademaster
 * @see #setFileInputStream(InputStream)
 * @see #setFileName(String)
 */
public class SendFileByInputStream extends Thread {
    private String host;
    private int sendFile_POST;
    private ServerSocket serverSocket;
    private InputStream fileInputStream;//文件输入流
    private String fileName;
    private String ServiceOrClient;//由初始化时只有post和既有host又有post区分是服务端还是客户端
    private Handler handler_MainActivity;
    private int sendMessage_FLAG=4;//接受标志
    private int currentLocal;
    private int file_length;

    public SendFileByInputStream(int sendFile_POST, Handler handlerMainActivity) {//作为服务端
        this.sendFile_POST = sendFile_POST;
        this.ServiceOrClient = "Service";
        try {
            this.serverSocket = new ServerSocket(sendFile_POST);//只需在初始化时设定一次
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.handler_MainActivity=handlerMainActivity;
    }

    public SendFileByInputStream(String host, int sendFile_POST, Handler handlerMainActivity) {//作为客户端
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
            //发送文字消息
            outputFile(out, fileInputStream);
            out.flush();//强迫输出流(或缓冲的流)发送数据

            //发送完消息，发送信息给MainActivity以进行进一步处理
            handler_MainActivity.sendEmptyMessage(sendMessage_FLAG);

            out.close();
            socket.close();
            if (ServiceOrClient == "Service") {//当为服务端时，结束线程前应关闭serverSocket，不然下次建立serverSocket会出行端口占用的错误
                serverSocket.close();
            }

        } catch (SocketTimeoutException s) {//接收服务器未建立时超时1s的异常处理
            System.out.println("（已超时）接收服务器未建立 receiveServer isn't established");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void outputFile(DataOutputStream out, InputStream fileInputStream) throws IOException {
        if (fileInputStream != null && fileName!=null) {

            //发送文件名
            byte[] bytes = fileName.getBytes();
            int byteLength = bytes.length;
            out.writeInt(byteLength);//输出文件名的byte长度
            out.write(bytes);//输出文件名的二进制

            //发送文件
            file_length=fileInputStream.available();//获取InputStream的长度（文件长度），对于本地文件可以正常获得，但是网络文件考虑网络问题可能会得到0
            System.out.println("文件大小:"+file_length);
            out.writeInt(file_length);//文件长度
            bytes=new byte[1024];//1k1k的发
            int n;
            while((n = fileInputStream.read(bytes))!=-1) {//将文件数据读入到二进制数组bytes中，没有数据时将返回-1
                try {
                    out.write(bytes,0,n);//二进制数组写入输出缓冲区，当缓冲区以满时，此处将产生阻塞，直到客户端使用read接收
                }catch(SocketException s) {
                    if(s.getMessage().equals("Connection reset by peer: socket write error")) {//当对方取消接受时触发
                        System.err.println("对方主动停止连接");
                        s.printStackTrace();
                    }
                    break;//跳出while循环
                }
                currentLocal = currentLocal+n;
                System.out.println(currentLocal);
            }
            fileInputStream.close();
            
        }
    }

    /**
     * 设置文件输入流，发送的数据将从该输入流获取（目的是发一点数据从外存读一点文件，避免一次性将文件读入内存使内存溢出）
     * @param fileInputStream
     */
    public void setFileInputStream(InputStream fileInputStream) {
        this.fileInputStream = fileInputStream;
    }

    /**
     * 设置文件的名字
     * @param fileName
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
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

