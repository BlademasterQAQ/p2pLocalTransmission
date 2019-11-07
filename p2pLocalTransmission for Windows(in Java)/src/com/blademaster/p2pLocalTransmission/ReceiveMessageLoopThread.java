package com.blademaster.p2pLocalTransmission;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

/**
 * 一个循环接收信息的线程
 * <p>
 * 作为服务端时，由端口和同步对象初始化；作为客户端时，由host、端口和同步对象初始化。一般接收线程作为服务端（无需指定接收方ip）
 * <p>
 * 当接受到数据后，程序会运行到<code>inputMesage()</code>读取数据之后，此时会释放线程锁，在主线程的中可以据此得知接受到消息
 * <p>
 * 通过Thread类的 {@link #start()}方法可以开启循环接收线程
 * <p>
 * 通过 {@link #getMessage()}获得接收到的数据
 * <p>
 * 通过 {@link #getConnecterIP()}获得发送数据方的ip，目的是使本机在接受文件后通知发送方时知道对方的ip
 * <p>
 * 通过 {@link #Stop()}可以停止线程的循环执行（在下一次执行后结束）
 * 
 * @see #start()
 * @see #getMessage()
 * @see #getConnecterIP()
 * @see #Stop()
 * 
 * @author blademaster
 *
 */
public class ReceiveMessageLoopThread extends Thread {
	private String host;
    private int receiveMessage_POST;
    private ServerSocket serverSocket;
    private String message;
    private String ServiceOrClient=null;//由初始化时只有post和既有host又有post区分是服务端还是客户端
    private boolean isStop=false;//控制循环读取信息是否停止
    
    private String connecterIP;
    
    private Object synchronizeReceiveMessage;
    
    public ReceiveMessageLoopThread(int receiveMessage_POST,Object synchronizeReceiveMessage){//作为服务端
        this.receiveMessage_POST=receiveMessage_POST;
        this.ServiceOrClient="Service";
        try {
			this.serverSocket = new ServerSocket(receiveMessage_POST);//只需在初始化时设定一次
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
        this.synchronizeReceiveMessage=synchronizeReceiveMessage;
    }

    public ReceiveMessageLoopThread(String host,int receiveMessage_POST,Object synchronizeReceiveMessage){//作为客户端
        this.host=host;
        this.receiveMessage_POST=receiveMessage_POST;
        this.ServiceOrClient="Client";
        this.synchronizeReceiveMessage=synchronizeReceiveMessage;
    }

    @Override
    public void run() {
        while (!isStop) {//循环等待服务器发送消息
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

                message = inputMesage(input);
                connecterIP = socket.getInetAddress().getHostAddress();//获取发送者的ip，并储存
                //接受到消息，发送信息给MainActivity以进行进一步处理
                
                System.out.println("msg: " + message);
                synchronized (synchronizeReceiveMessage) {
                	synchronizeReceiveMessage.notify();//线程解锁
                }

                input.close();
                socket.close();
            } catch (SocketTimeoutException s) {//服务器未建立时超时1s的异常处理
            	System.out.println("发送客户端未建立 sendClient isn't established");

            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                System.out.println("活动线程数：" + Thread.activeCount());
            }
            //注意：被包含的异常应该放在前面（SocketException被IOException包含）
        }
    }

    private String inputMesage(DataInputStream input) throws IOException {//由输入流读取String
        
    	int byteLength=input.readInt();//读取一个String的长度
    	//创建这个长度的字节数组
    	byte[] bytes = new byte[byteLength];
    	//再读取这个长度的字节数，也就是真正的消息体
    	input.read(bytes);
    	//将字节数组转为String
    	message = new String(bytes, "UTF-8");

        return message;
    }

    /**
     * 获取接收的数据
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取发送数据方的ip
     * @return
     */
    public String getConnecterIP(){
        return connecterIP;
    }
    
    /**
     * 停止循环
     * @return
     */
    public void Stop() {//停止循环
    	this.isStop=true;
    }
}
