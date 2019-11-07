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
    private String ServiceOrClient=null;//�ɳ�ʼ��ʱֻ��post�ͼ���host����post�����Ƿ���˻��ǿͻ���
    
    public SendMessageThread(int sendMessage_POST){//��Ϊ�����
        this.sendMessage_POST =sendMessage_POST;
        this.ServiceOrClient="Service";
        try {
            this.serverSocket = new ServerSocket(sendMessage_POST);//ֻ���ڳ�ʼ��ʱ�趨һ��
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SendMessageThread(String host, int sendMessage_POST){//��Ϊ�ͻ���
        this.host=host;
        this.sendMessage_POST =sendMessage_POST;
        this.ServiceOrClient="Client";
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
                SocketAddress socketAddress = new InetSocketAddress(host, sendMessage_POST);
                socket.connect(socketAddress, 1000);// 10sΪ��ʱʱ��(��ʱ���׳��쳣)�������socket.connect(socketAddress);���ڷ�����δ����ʱ��������������������������߳�
                //����λ��
                System.out.println("����socket");
            }

            //��ȡ�������ͨ�������������Ϣ
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            //����������Ϣ
            outputMessage(out, message);
            out.flush();//ǿ�������(�򻺳����)��������
            out.close();
            socket.close();
            if(ServiceOrClient == "Service") {//��Ϊ�����ʱ�������߳�ǰӦ�ر�serverSocket����Ȼ�´ν���serverSocket����ж˿�ռ�õĴ���
            	serverSocket.close();
            }
        }catch (SocketTimeoutException s){//���շ�����δ����ʱ��ʱ1s���쳣����
            System.out.println("���շ�����δ���� receiveServer isn't established");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void outputMessage(DataOutputStream out, String message) throws IOException {//��String������Ϊ���String���ݷ���
        if(message!=null) {//��Ϣ��Ϊ��ʱ
            //����������
            byte[] bytes = message.getBytes("UTF-8");//��Stringת��Ϊ�ֽ����ͣ���UTF-8�����ʽ����
            int byteLength = bytes.length;
            out.writeInt(byteLength);//���message��byte����
            out.write(bytes);//���message�Ķ�����
        }
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
