package com.blademaster.p2pLocalTransmission;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

public class SendMessageThread extends Thread{
    private String host;
    private int sendMessage_POST;
    private ServerSocket serverSocket;
    private String message;
    private String ServiceOrClient=null;//由初始化时只有post和既有host又有post区分是服务端还是客户端
    
    public SendMessageThread(int sendMessage_POST){//作为服务端
        this.sendMessage_POST =sendMessage_POST;
        this.ServiceOrClient="Service";
        try {
            this.serverSocket = new ServerSocket(sendMessage_POST);//只需在初始化时设定一次
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SendMessageThread(String host, int sendMessage_POST){//作为客户端
        this.host=host;
        this.sendMessage_POST =sendMessage_POST;
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
                SocketAddress socketAddress = new InetSocketAddress(host, sendMessage_POST);
                socket.connect(socketAddress, 1000);// 10s为超时时间(超时将抛出异常)，如果用socket.connect(socketAddress);将在服务器未建立时产生阻塞，避免产生过多阻塞线程
                //阻塞位置
                System.out.println("建立socket");
            }

            //获取输出流，通过这个流发送消息
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            //发送文字消息
            outputMessage(out, message);
            out.flush();//强迫输出流(或缓冲的流)发送数据
            out.close();
            socket.close();
            if(ServiceOrClient == "Service") {//当为服务端时，结束线程前应关闭serverSocket，不然下次建立serverSocket会出行端口占用的错误
            	serverSocket.close();
            }
        }catch (SocketTimeoutException s){//接收服务器未建立时超时1s的异常处理
            System.out.println("接收服务器未建立 receiveServer isn't established");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void outputMessage(DataOutputStream out, String message) throws IOException {//将String数组拆分为多个String数据发送
        if(message!=null) {//信息不为空时
            //输出数组个数
            byte[] bytes = message.getBytes("UTF-8");//将String转化为字节类型，以UTF-8编码格式发送
            int byteLength = bytes.length;
            out.writeInt(byteLength);//输出message的byte长度
            out.write(bytes);//输出message的二进制
        }
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
