package com.blademaster.p2plocaltransmission;

import android.os.Handler;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;


/**
 * 循环接受文字信息线程
 * <p>
 * 该线程一经<code>start</code>就将一直允许，接受信息并存到<code>message</code>变量处
 * <p>
 * 一旦接受到String数据时，将通过handler通知主线程
 * <p>
 * 当调用<code>getMessage</code>方法时，将返上一次接受的String类型数据
 * <p>
 * 当调用<code>close</code>方法时，该线程将结束
 *
 * @see #start()
 * @see #getMessage()
 * @see #close()
 *
 * @author blademaster
 */

public class ReceiveMessageLoopThread extends Thread {
    private String host;
    private int receiveMessage_POST;
    private ServerSocket serverSocket;
    private String message;
    private String ServiceOrClient;//由初始化时只有post和既有host又有post区分是服务端还是客户端
    private boolean isclose =false;//控制循环读取信息是否停止
    private Handler handler_MainActivity;
    private int receiveMessage_FLAG=3;//接受标志

    private String connecterIP;
    /**
     * 由端口建立服务端
     * @param receiveMessage_POST
     * @param handlerMainActivity
     */
    public ReceiveMessageLoopThread(int receiveMessage_POST, Handler handlerMainActivity){//作为服务端
        this.receiveMessage_POST=receiveMessage_POST;
        this.ServiceOrClient="Service";
        this.handler_MainActivity =handlerMainActivity;
        try {
            this.serverSocket = new ServerSocket(receiveMessage_POST);//只需在初始化时设定一次
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 由ip地址和端口建立客户端
     * @param host
     * @param receiveMessage_POST
     * @param handlerMainActivity
     */

    public ReceiveMessageLoopThread(String host, int receiveMessage_POST, Handler handlerMainActivity){//作为客户端
        this.host=host;
        this.receiveMessage_POST=receiveMessage_POST;
        this.ServiceOrClient="Client";
        this.handler_MainActivity =handlerMainActivity;
    }

    @Override
    public void run() {
        while (!isclose) {//循环等待服务器发送消息
            Socket socket = null;
            try {
                if (ServiceOrClient == "Service") {
                    System.out.println("等待客户端连接");
                    socket = serverSocket.accept();//在客户端未连接（发送数据）时产生中断（阻塞）
                }
                if (ServiceOrClient == "Client") {
                    // 当为客户端时，等待服务端建立
                    if (socket == null || (!socket.isConnected())) {// 当socket=null和未connect时运行创建socket并等待的代码，这个判断是必要的，不然成功连接后可能会重置连接
                        System.out.println("等待服务器...");
                        socket = new Socket();// 建立空socket，目的是用connect方法以超时方法连接服务器
                        SocketAddress socketAddress = new InetSocketAddress(host, receiveMessage_POST);
                        socket.connect(socketAddress, 1000);// 1s为超时时间(超时将抛出异常)，如果用socket.connect(socketAddress);将在服务器未建立时产生阻塞
                        //阻塞位置
                        System.out.println("已连接服务器，等待接收数据");
                    }
                }
                //获取输入流
                DataInputStream input = new DataInputStream(socket.getInputStream());
                //读取长度，也即是消息头
                message = getTextMsg(input);
                connecterIP = socket.getInetAddress().getHostAddress();//获取发送者的ip，并储存
                //System.out.println(socket.getLocalAddress().getHostAddress());//获取本机ip
                //System.out.println(socket.getInetAddress().getHostAddress());//获取连接方（发送方）的ip

                //接受到消息，发送信息给MainActivity以进行进一步处理
                handler_MainActivity.sendEmptyMessage(receiveMessage_FLAG);

                System.out.println("msg: " + message);

                input.close();
                socket.close();
            } catch (SocketTimeoutException s) {//发送服务器未建立时超时1s的异常处理
                System.out.println("（已超时）发送服务器未建立 sendServer isn't established");

            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                System.out.println("活动线程数：" + Thread.activeCount());
            }
            //注意：被包含的异常应该放在前面（SocketException被IOException包含）
        }
    }

    private String getTextMsg(DataInputStream input) throws IOException {//由输入流读取String

        int byteLength=input.readInt();//读取一个String的长度
        //创建这个长度的字节数组
        byte[] bytes = new byte[byteLength];
        //再读取这个长度的字节数，也就是真正的消息体
        input.read(bytes);
        //将字节数组转为String
        message = new String(bytes);

        return message;
    }

    /**
     * 获取上一次接受的String数据
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取上一次数据来源的ip
     * @return
     */
    public String getConnecterIP(){
        return connecterIP;
    }

    /**
     * 关闭当前线程
     */
    public void close() {//停止循环
        this.isclose = true;
    }

    /**
     * 重设host
     * @param host
     */

    public void setHost(String host) {
        this.host = host;
    }


}

