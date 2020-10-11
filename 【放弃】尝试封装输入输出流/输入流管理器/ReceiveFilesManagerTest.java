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
    private String ServiceOrClient=null;//�ɳ�ʼ��ʱֻ��post�ͼ���host����post�����Ƿ���˻��ǿͻ���
    private int file_length;//�ļ�����
    private int currentLocal;//���ڴ����λ��
	
    private Object synchronizeReceiveFile;

    
    public ReceiveFilesManagerTest(int receiveFile_POST,Object synchronizeReceiveFile){//��Ϊ�����
        this.receiveFile_POST =receiveFile_POST;
        this.ServiceOrClient="Service";
        try {
            this.serverSocket = new ServerSocket(receiveFile_POST);//ֻ���ڳ�ʼ��ʱ�趨һ��
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.synchronizeReceiveFile=synchronizeReceiveFile;
    }

    public ReceiveFilesManagerTest(String host, int receiveFile_POST,Object synchronizeReceiveFile){//��Ϊ�ͻ���
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
                System.out.println("�ȴ��ͻ�������");
                socket = serverSocket.accept();//�ڿͻ���δ���ӣ��������ݣ�ʱ�����жϣ�������
            }

            if (ServiceOrClient == "Client") {
                //��������
                System.out.println("�ȴ�������...");
                socket = new Socket();//�ڷ����δ����ʱ�����жϣ�������
                SocketAddress socketAddress = new InetSocketAddress(host, receiveFile_POST);
                socket.connect(socketAddress, 10000);// 10sΪ��ʱʱ��(��ʱ���׳��쳣)�������socket.connect(socketAddress);���ڷ�����δ����ʱ��������������������������߳�
                //����λ��
                System.out.println("����socket");
            }

            //��ȡ��������ͨ���������ȡ��Ϣ
            DataInputStream input = new DataInputStream(socket.getInputStream());
            //����������Ϣ
            inputFile(input);
            
            synchronized (synchronizeReceiveFile) {
            	synchronizeReceiveFile.notify();
			}
            
            input.close();
            socket.close();
            if(ServiceOrClient == "Service") {//��Ϊ�����ʱ�������߳�ǰӦ�ر�serverSocket����Ȼ�´ν���serverSocket����ж˿�ռ�õĴ���
            	serverSocket.close();
            }
        }catch (SocketTimeoutException s){//���շ�����δ����ʱ��ʱ1s���쳣����
            System.out.println("���Ϳͻ���δ���� sendClient isn't established");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void inputFile(DataInputStream input) throws IOException {
    	
    	InputStreamManager inputStreamManager = new InputStreamManager();
    	inputStreamManager.read(input);

	}

    /**
     * ��ȡ��ǰ�ļ��ĳ���
     * @return
     */
    public int getFile_length() {
        return file_length;
    }

    /**
     * ��ȡ���ڴ����λ��
     * @return
     */

    public int getCurrentLocal() {
        return currentLocal;
    }    
}
