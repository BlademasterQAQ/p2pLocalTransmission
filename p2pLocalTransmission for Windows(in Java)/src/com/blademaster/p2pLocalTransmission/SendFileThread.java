package com.blademaster.p2pLocalTransmission;

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

public class SendFileThread extends Thread{
	private String host;
    private int sendFile_POST;
    private ServerSocket serverSocket;
    private String ServiceOrClient=null;//由初始化时只有post和既有host又有post区分是服务端还是客户端
    private String filePath;//文件路径
    private int file_length;
    private int currentLocal = 0;//正在传输的位置
    
    private Object synchronizeSendFile;
    
    public SendFileThread(int sendFile_POST,Object synchronizeSendFile){//作为服务端
        this.sendFile_POST =sendFile_POST;
        this.ServiceOrClient="Service";
        try {
            this.serverSocket = new ServerSocket(sendFile_POST);//只需在初始化时设定一次
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.synchronizeSendFile=synchronizeSendFile;
    }

    public SendFileThread(String host, int sendFile_POST,Object synchronizeSendFile){//作为客户端
        this.host=host;
        this.sendFile_POST =sendFile_POST;
        this.ServiceOrClient="Client";
        this.synchronizeSendFile=synchronizeSendFile;
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
                socket.connect(socketAddress, 10000);// 1s为超时时间(超时将抛出异常)，如果用socket.connect(socketAddress);将在服务器未建立时产生阻塞，避免产生过多阻塞线程
                //阻塞位置
                System.out.println("建立socket");
            }

            //获取输出流，通过这个流读取消息
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            //发送文字消息
            outputFile(out,filePath);
            
            out.flush();//强迫输出流(或缓冲的流)发送数据
            
            //开始发送后
            synchronized (synchronizeSendFile) {
            	synchronizeSendFile.notify();//线程解锁
            }
            
            out.close();
            socket.close();
            if(ServiceOrClient == "Service") {//当为服务端时，结束线程前应关闭serverSocket，不然下次建立serverSocket会出行端口占用的错误
                serverSocket.close();
            }
		} catch (SocketTimeoutException s) {// 接收服务器未建立时超时1s的异常处理
			System.err.println("发送文件超时");
			s.printStackTrace();

		} catch (ConnectException c) {
			System.err.println("访问被拒绝（可能被防火墙阻拦）");
			c.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private void outputFile(DataOutputStream out,String filePath) throws IOException {
    	if(filePath!=null){
            File file=new File(filePath);
            //发送文件名
            String fileName=file.getName();
            byte[] bytes=fileName.getBytes("UTF-8");
            int byteLength = bytes.length;
            out.writeInt(byteLength);//输出文件名的byte长度
            out.write(bytes);//输出文件名的二进制
            
            System.out.println("文件是否存在："+file.exists());
            System.out.println("文件是否可读："+file.canRead());
            if(!file.canRead()) {//如果文件不可读，取消发送
            	return;
            }
            
            file_length=(int)file.length();
            System.out.println("len:"+file_length);
            out.writeInt(file_length);
            
            //发送文件
            //写法2：由file.length()获取文件二进制长度，然后边读byte边发(这种方法是手机收完这边才发)
            FileInputStream fileInputStream = new FileInputStream(file);//从文件输入到程序中，为输入流（针对程序而言）
            bytes=new byte[1024];//1k1k的发
            int n;
            while((n = fileInputStream.read(bytes))!=-1) {//将文件数据读入到二进制数组bytes中，没有数据时将返回-1
                try {
                	out.write(bytes,0,n);//二进制数组写入输出缓冲区，当缓冲区以满时，此处将产生阻塞，直到客户端使用read接收
                }catch(SocketException s) {
                	if(s.getMessage().equals("Connection reset by peer: socket write error")) {//当对方取消接受时触发
                		System.err.println("对方主动停止连接");
                		s.printStackTrace();
                		//System.err.println("\tat "+Thread.currentThread().getStackTrace()[1]);//输出错误位置
                	}
                	break;//跳出while循环
                }
                currentLocal = currentLocal+n;
                System.out.println(currentLocal);
            }
            fileInputStream.close();
            
            //写法1：用ByteArrayOutputStream，先得到一个输出流再由其获取二进制长度和比特（问题：文件太大时溢出）
            /*
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
                System.out.println("文件路径不是绝对路径或者未开启文件读写权限");
            }

            int len = bout.size();
            //这里打印一下发送的长度
            System.out.println("len: "+len);
            out.writeInt(len);
            out.write(bout.toByteArray());//由ByteArrayOutputStream获取输出流的二进制
            */
        }
    }
    
    /**
     * 设置发送文件的路径
     * @return
     */
    public void setFilePath(String filePath) {
    	this.filePath=filePath;
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