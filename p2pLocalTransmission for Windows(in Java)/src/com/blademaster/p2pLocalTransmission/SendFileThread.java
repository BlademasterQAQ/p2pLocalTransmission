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
    private String ServiceOrClient=null;//�ɳ�ʼ��ʱֻ��post�ͼ���host����post�����Ƿ���˻��ǿͻ���
    private String filePath;//�ļ�·��
    private int file_length;
    private int currentLocal = 0;//���ڴ����λ��
    
    private Object synchronizeSendFile;
    
    public SendFileThread(int sendFile_POST,Object synchronizeSendFile){//��Ϊ�����
        this.sendFile_POST =sendFile_POST;
        this.ServiceOrClient="Service";
        try {
            this.serverSocket = new ServerSocket(sendFile_POST);//ֻ���ڳ�ʼ��ʱ�趨һ��
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.synchronizeSendFile=synchronizeSendFile;
    }

    public SendFileThread(String host, int sendFile_POST,Object synchronizeSendFile){//��Ϊ�ͻ���
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
                System.out.println("�ȴ��ͻ�������");
                socket = serverSocket.accept();//�ڿͻ���δ���ӣ��������ݣ�ʱ�����жϣ�������
            }

            if (ServiceOrClient == "Client") {
                //��������
                System.out.println("�ȴ�������...");
                socket = new Socket();//�ڷ����δ����ʱ�����жϣ�������
                SocketAddress socketAddress = new InetSocketAddress(host, sendFile_POST);
                socket.connect(socketAddress, 10000);// 1sΪ��ʱʱ��(��ʱ���׳��쳣)�������socket.connect(socketAddress);���ڷ�����δ����ʱ��������������������������߳�
                //����λ��
                System.out.println("����socket");
            }

            //��ȡ�������ͨ���������ȡ��Ϣ
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            //����������Ϣ
            outputFile(out,filePath);
            
            out.flush();//ǿ�������(�򻺳����)��������
            
            //��ʼ���ͺ�
            synchronized (synchronizeSendFile) {
            	synchronizeSendFile.notify();//�߳̽���
            }
            
            out.close();
            socket.close();
            if(ServiceOrClient == "Service") {//��Ϊ�����ʱ�������߳�ǰӦ�ر�serverSocket����Ȼ�´ν���serverSocket����ж˿�ռ�õĴ���
                serverSocket.close();
            }
		} catch (SocketTimeoutException s) {// ���շ�����δ����ʱ��ʱ1s���쳣����
			System.err.println("�����ļ���ʱ");
			s.printStackTrace();

		} catch (ConnectException c) {
			System.err.println("���ʱ��ܾ������ܱ�����ǽ������");
			c.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private void outputFile(DataOutputStream out,String filePath) throws IOException {
    	if(filePath!=null){
            File file=new File(filePath);
            //�����ļ���
            String fileName=file.getName();
            byte[] bytes=fileName.getBytes("UTF-8");
            int byteLength = bytes.length;
            out.writeInt(byteLength);//����ļ�����byte����
            out.write(bytes);//����ļ����Ķ�����
            
            System.out.println("�ļ��Ƿ���ڣ�"+file.exists());
            System.out.println("�ļ��Ƿ�ɶ���"+file.canRead());
            if(!file.canRead()) {//����ļ����ɶ���ȡ������
            	return;
            }
            
            file_length=(int)file.length();
            System.out.println("len:"+file_length);
            out.writeInt(file_length);
            
            //�����ļ�
            //д��2����file.length()��ȡ�ļ������Ƴ��ȣ�Ȼ��߶�byte�߷�(���ַ������ֻ�������߲ŷ�)
            FileInputStream fileInputStream = new FileInputStream(file);//���ļ����뵽�����У�Ϊ����������Գ�����ԣ�
            bytes=new byte[1024];//1k1k�ķ�
            int n;
            while((n = fileInputStream.read(bytes))!=-1) {//���ļ����ݶ��뵽����������bytes�У�û������ʱ������-1
                try {
                	out.write(bytes,0,n);//����������д�������������������������ʱ���˴�������������ֱ���ͻ���ʹ��read����
                }catch(SocketException s) {
                	if(s.getMessage().equals("Connection reset by peer: socket write error")) {//���Է�ȡ������ʱ����
                		System.err.println("�Է�����ֹͣ����");
                		s.printStackTrace();
                		//System.err.println("\tat "+Thread.currentThread().getStackTrace()[1]);//�������λ��
                	}
                	break;//����whileѭ��
                }
                currentLocal = currentLocal+n;
                System.out.println(currentLocal);
            }
            fileInputStream.close();
            
            //д��1����ByteArrayOutputStream���ȵõ�һ��������������ȡ�����Ƴ��Ⱥͱ��أ����⣺�ļ�̫��ʱ�����
            /*
            ByteArrayOutputStream bout=null;
            try {
                FileInputStream fileInputStream = new FileInputStream(file);//���ļ����뵽�����У�Ϊ����������Գ�����ԣ�
                bout = new ByteArrayOutputStream();
                bytes=new byte[1024];
                while(fileInputStream.read(bytes)!=-1) {//���ļ����ݶ��뵽����������bytes�У�û������ʱ������-1
                    bout.write(bytes);//����������д�������������
                }
                fileInputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println("�ļ�·�����Ǿ���·������δ�����ļ���дȨ��");
            }

            int len = bout.size();
            //�����ӡһ�·��͵ĳ���
            System.out.println("len: "+len);
            out.writeInt(len);
            out.write(bout.toByteArray());//��ByteArrayOutputStream��ȡ������Ķ�����
            */
        }
    }
    
    /**
     * ���÷����ļ���·��
     * @return
     */
    public void setFilePath(String filePath) {
    	this.filePath=filePath;
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