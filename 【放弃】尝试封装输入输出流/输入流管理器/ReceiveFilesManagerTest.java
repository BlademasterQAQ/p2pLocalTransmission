package com.blademaster.p2pLocalTransmission;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class ReceiveFilesManagerTest extends Thread{
	private String host;
    private int receiveFile_POST;
    private ServerSocket serverSocket;
    private String ServiceOrClient=null;//由初始化时只有post和既有host又有post区分是服务端还是客户端
    private int file_length;//文件长度
    private int currentLocal;//正在传输的位置
	
    private Object synchronizeReceiveFile;

    
    public ReceiveFilesManagerTest(int receiveFile_POST,Object synchronizeReceiveFile){//作为服务端
        this.receiveFile_POST =receiveFile_POST;
        this.ServiceOrClient="Service";
        try {
            this.serverSocket = new ServerSocket(receiveFile_POST);//只需在初始化时设定一次
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.synchronizeReceiveFile=synchronizeReceiveFile;
    }

    public ReceiveFilesManagerTest(String host, int receiveFile_POST,Object synchronizeReceiveFile){//作为客户端
        this.host=host;
        this.receiveFile_POST =receiveFile_POST;
        this.ServiceOrClient="Client";
        this.synchronizeReceiveFile=synchronizeReceiveFile;
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
                SocketAddress socketAddress = new InetSocketAddress(host, receiveFile_POST);
                socket.connect(socketAddress, 10000);// 10s为超时时间(超时将抛出异常)，如果用socket.connect(socketAddress);将在服务器未建立时产生阻塞，避免产生过多阻塞线程
                //阻塞位置
                System.out.println("建立socket");
            }

            //获取输入流，通过这个流读取消息
            DataInputStream input = new DataInputStream(socket.getInputStream());
            //发送文字消息
            inputFile(input);
            
            synchronized (synchronizeReceiveFile) {
            	synchronizeReceiveFile.notify();
			}
            
            input.close();
            socket.close();
            if(ServiceOrClient == "Service") {//当为服务端时，结束线程前应关闭serverSocket，不然下次建立serverSocket会出行端口占用的错误
            	serverSocket.close();
            }
        }catch (SocketTimeoutException s){//接收服务器未建立时超时1s的异常处理
            System.out.println("发送客户端未建立 sendClient isn't established");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void inputFile(DataInputStream input) throws IOException {
    	
    	InputStreamManager inputStreamManager = new InputStreamManager();
    	inputStreamManager.read(input);

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
