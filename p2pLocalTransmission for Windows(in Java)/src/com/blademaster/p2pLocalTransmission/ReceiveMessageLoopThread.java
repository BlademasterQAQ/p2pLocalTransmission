package com.blademaster.p2pLocalTransmission;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

/**
 * һ��ѭ��������Ϣ���߳�
 * <p>
 * ��Ϊ�����ʱ���ɶ˿ں�ͬ�������ʼ������Ϊ�ͻ���ʱ����host���˿ں�ͬ�������ʼ����һ������߳���Ϊ����ˣ�����ָ�����շ�ip��
 * <p>
 * �����ܵ����ݺ󣬳�������е�<code>inputMesage()</code>��ȡ����֮�󣬴�ʱ���ͷ��߳����������̵߳��п��Ծݴ˵�֪���ܵ���Ϣ
 * <p>
 * ͨ��Thread��� {@link #start()}�������Կ���ѭ�������߳�
 * <p>
 * ͨ�� {@link #getMessage()}��ý��յ�������
 * <p>
 * ͨ�� {@link #getConnecterIP()}��÷������ݷ���ip��Ŀ����ʹ�����ڽ����ļ���֪ͨ���ͷ�ʱ֪���Է���ip
 * <p>
 * ͨ�� {@link #Stop()}����ֹͣ�̵߳�ѭ��ִ�У�����һ��ִ�к������
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
    private String ServiceOrClient=null;//�ɳ�ʼ��ʱֻ��post�ͼ���host����post�����Ƿ���˻��ǿͻ���
    private boolean isStop=false;//����ѭ����ȡ��Ϣ�Ƿ�ֹͣ
    
    private String connecterIP;
    
    private Object synchronizeReceiveMessage;
    
    public ReceiveMessageLoopThread(int receiveMessage_POST,Object synchronizeReceiveMessage){//��Ϊ�����
        this.receiveMessage_POST=receiveMessage_POST;
        this.ServiceOrClient="Service";
        try {
			this.serverSocket = new ServerSocket(receiveMessage_POST);//ֻ���ڳ�ʼ��ʱ�趨һ��
		} catch (IOException e) {
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
		}
        this.synchronizeReceiveMessage=synchronizeReceiveMessage;
    }

    public ReceiveMessageLoopThread(String host,int receiveMessage_POST,Object synchronizeReceiveMessage){//��Ϊ�ͻ���
        this.host=host;
        this.receiveMessage_POST=receiveMessage_POST;
        this.ServiceOrClient="Client";
        this.synchronizeReceiveMessage=synchronizeReceiveMessage;
    }

    @Override
    public void run() {
        while (!isStop) {//ѭ���ȴ�������������Ϣ
            Socket socket = null;
            
            try {          	
            	if (ServiceOrClient == "Service") {
					System.out.println("�ȴ��ͻ�������");
			        socket = serverSocket.accept();//�ڿͻ���δ���ӣ��������ݣ�ʱ�����жϣ�������
				}
            	
				if (ServiceOrClient == "Client") {
					// ��Ϊ�ͻ���ʱ���ȴ�����˽���
					if (socket == null || (!socket.isConnected())) {// ��socket=null��δconnectʱ���д���socket���ȴ��Ĵ��룬����ж��Ǳ�Ҫ�ģ���Ȼ�ɹ����Ӻ���ܻ���������
						System.out.println("�ȴ�������...");
						socket = new Socket();// ������socket��Ŀ������connect�����Գ�ʱ�������ӷ�����
						SocketAddress socketAddress = new InetSocketAddress(host, receiveMessage_POST);
						socket.connect(socketAddress, 1000);// 1sΪ��ʱʱ��(��ʱ���׳��쳣)�������socket.connect(socketAddress);���ڷ�����δ����ʱ��������
						//����λ��
						System.out.println("�����ӷ��������ȴ���������");
					}
				}
				
            	
                //��ȡ������
                DataInputStream input = new DataInputStream(socket.getInputStream());

                message = inputMesage(input);
                connecterIP = socket.getInetAddress().getHostAddress();//��ȡ�����ߵ�ip��������
                //���ܵ���Ϣ��������Ϣ��MainActivity�Խ��н�һ������
                
                System.out.println("msg: " + message);
                synchronized (synchronizeReceiveMessage) {
                	synchronizeReceiveMessage.notify();//�߳̽���
                }

                input.close();
                socket.close();
            } catch (SocketTimeoutException s) {//������δ����ʱ��ʱ1s���쳣����
            	System.out.println("���Ϳͻ���δ���� sendClient isn't established");

            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                System.out.println("��߳�����" + Thread.activeCount());
            }
            //ע�⣺���������쳣Ӧ�÷���ǰ�棨SocketException��IOException������
        }
    }

    private String inputMesage(DataInputStream input) throws IOException {//����������ȡString
        
    	int byteLength=input.readInt();//��ȡһ��String�ĳ���
    	//����������ȵ��ֽ�����
    	byte[] bytes = new byte[byteLength];
    	//�ٶ�ȡ������ȵ��ֽ�����Ҳ������������Ϣ��
    	input.read(bytes);
    	//���ֽ�����תΪString
    	message = new String(bytes, "UTF-8");

        return message;
    }

    /**
     * ��ȡ���յ�����
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * ��ȡ�������ݷ���ip
     * @return
     */
    public String getConnecterIP(){
        return connecterIP;
    }
    
    /**
     * ֹͣѭ��
     * @return
     */
    public void Stop() {//ֹͣѭ��
    	this.isStop=true;
    }
}
