package com.blademaster.p2plocaltransmission;

import android.os.Handler;
import android.util.Log;

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

public class SendFilesManagerTest extends Thread{
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

    public SendFilesManagerTest(int sendFile_POST, Handler handlerMainActivity) {//作为服务端
        this.sendFile_POST = sendFile_POST;
        this.ServiceOrClient = "Service";
        try {
            this.serverSocket = new ServerSocket(sendFile_POST);//只需在初始化时设定一次
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.handler_MainActivity=handlerMainActivity;
    }

    public SendFilesManagerTest(String host, int sendFile_POST, Handler handlerMainActivity) {//作为客户端
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

    private void outputFile(DataOutputStream out,ArrayList<String> filePath) throws IOException{
        ArrayList<File> files = new ArrayList<>(0);
        if(filePath!=null && filePath.size()!=0){
            for(int i=0;i<filePath.size();i++){
                File file=new File(filePath.get(i));
                if(file.exists()){
                    files.add(file);
                }
            }

            OutputStreamManager outputStreamManager = new OutputStreamManager();

            //文件名
            ArrayList<Object> filesName = new ArrayList<>();
            for(int i=0;i<files.size();i++){
                filesName.add(files.get(i).getName());
            }
            outputStreamManager.addData_single(filesName,"filesName", null);

            //文件修改时间
            ArrayList<Object> filesLastModified = new ArrayList<>();
            for(int i=0;i<files.size();i++){
                filesLastModified.add(files.get(i).lastModified());
            }
            outputStreamManager.addData_single(filesLastModified,"filesLastModified", null);

            //文件路径
            ArrayList<Object> filesPath = new ArrayList<>();
            for(int i=0;i<files.size();i++){
                filesPath.add(files.get(i).getPath());
            }
            outputStreamManager.addData_single(filesPath,"filesPath", null);

            //文件内容
            ArrayList<Object> filesContent = new ArrayList<>();
            for(int i=0;i<files.size();i++){
                filesContent.add(files.get(i));
            }
            outputStreamManager.addData_single(filesContent,"filesContent", "filesName");

            outputStreamManager.write(out);

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
