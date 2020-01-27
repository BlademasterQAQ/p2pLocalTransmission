package com.blademaster.p2pLocalTransmission;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class SendFilesThread extends Thread{
	private String host;
    private int sendFile_POST;
    private ServerSocket serverSocket;
    private String ServiceOrClient=null;//�ɳ�ʼ��ʱֻ��post�ͼ���host����post�����Ƿ���˻��ǿͻ���
    private ArrayList<String> filesPath;//�ļ�·��
    
    private Object synchronizeSendFile;
    
    //����UI��ʾ
    private int currentLocal;//����ĳ���ļ��ĵ�ǰλ��
    private int file_length;//����ĳ���ļ��ĳ���
    private int currentFileCount;//��ǰ���䵽�ڼ����ļ�
    
    public SendFilesThread(int sendFile_POST,Object synchronizeSendFile){//��Ϊ�����
        this.sendFile_POST =sendFile_POST;
        this.ServiceOrClient="Service";
        try {
            this.serverSocket = new ServerSocket(sendFile_POST);//ֻ���ڳ�ʼ��ʱ�趨һ��
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.synchronizeSendFile=synchronizeSendFile;
    }

    public SendFilesThread(String host, int sendFile_POST,Object synchronizeSendFile){//��Ϊ�ͻ���
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
                long start_time = System.currentTimeMillis();//��ʼʱ��
                boolean i = true;
                while (i) {
                    try {
                        socket = new Socket();//�ڷ����δ����ʱ�����жϣ�������
                        SocketAddress socketAddress = new InetSocketAddress(host, sendFile_POST);
                        socket.connect(socketAddress, 10000);// 10sΪ��ʱʱ��(��ʱ���׳��쳣)�������socket.connect(socketAddress);���ڷ�����δ����ʱ��������������������������߳�
                        i = false;
                    } catch (ConnectException c) {
                        System.err.println("ConnectException:�ȴ�������");
                        System.err.println("\tat"+Thread.currentThread().getStackTrace()[1]);//�������λ��
                        if(System.currentTimeMillis()-start_time > 10000){//��ʱ10000ms��
                            throw new SocketTimeoutException();//�׳���ʱ�쳣
                        }
                    }

                }

                //����λ��
                System.out.println("����socket");
            }

            //��ȡ�������ͨ�������������Ϣ
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            //�����ļ�
            outputFile(out, filesPath);
            
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
        }catch (SocketTimeoutException s){//���շ�����δ����ʱ��ʱ1s���쳣����
            System.out.println("���ѳ�ʱ�����շ�����δ���� receiveServer isn't established");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void outputFile(DataOutputStream out,ArrayList<String> filesPath) throws IOException{
        ArrayList<File> files = new ArrayList<>(0);
        if(filesPath!=null && filesPath.size()!=0){
            for(int i=0;i<filesPath.size();i++){
                File file=new File(filesPath.get(i));
                if(file.exists()){
                    files.add(file);
                }
            }

            //�����ļ�����
            out.writeInt(files.size());

            //�����ļ���
            for(int i=0;i<files.size();i++){
                byte[] bytes = files.get(i).getName().getBytes("UTF-8");
                int byteLength = bytes.length;
                out.writeInt(byteLength);//����ļ�����byte����
                out.write(bytes);//����ļ����Ķ�����
            }

            //�����ļ���С
            for(int i=0;i<files.size();i++){
                out.writeInt((int)files.get(i).length());//����ļ��ĳ���
            }

            //�����ļ�����޸�����
            for(int i=0;i<files.size();i++){
                out.writeLong(files.get(i).lastModified());//����ļ��ĳ���
            }

            //�����ļ�·�������ڻش�ʱ��ԭ��
            ArrayList<String> fileName = new ArrayList<>();
            ArrayList<String> filePath = new ArrayList<>();
            try {
    			FileInputStream fileInputStream = new FileInputStream("p2pLocalTransmission·���浵.txt");//���ļ�ĩβд��
    			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
    			BufferedReader bufferedReader =new BufferedReader(inputStreamReader);
        	
    			String string;
            	int n = 0;
    			while((string = bufferedReader.readLine()) != null) {
    				String[] string_array = string.split("\t");
    				if((n = fileName.indexOf(string_array[0])) == -1) {
        				fileName.add(string_array[0]);
        				filePath.add(string_array[1]);
    				}else {
    					filePath.set(n, string_array[1]);//����ͬ����·��
    				}
    			}
    			bufferedReader.close();
    		} catch (IOException e) {
    			// TODO �Զ����ɵ� catch ��
    			e.printStackTrace();
    		}
            
            for(int i=0;i<files.size();i++){
            	String string = "null";
            	int n = 0;
            	if((n = fileName.indexOf(files.get(i).getName())) != -1) {
            		string = filePath.get(n);
            	}
            	System.err.println(string);
                byte[] bytes = string.getBytes("UTF-8");
                int byteLength = bytes.length;
                out.writeInt(byteLength);//����ļ�����byte����
                out.write(bytes);//����ļ����Ķ�����
            }
            
            //�����ļ�
            for(int i=0;i<files.size();i++) {
                file_length = (int)files.get(i).length();//��ȡ�ļ�����
                FileInputStream fileInputStream = new FileInputStream(files.get(i));//��ȡ�ļ�������
                byte[] bytes = new byte[1024];//1k1k�ķ�
                int n;
                currentLocal = 0;
                while ((n = fileInputStream.read(bytes)) != -1) {//���ļ����ݶ��뵽����������bytes�У�û������ʱ������-1
                    try {
                        out.write(bytes, 0, n);//����������д�������������������������ʱ���˴�������������ֱ���ͻ���ʹ��read����
                    } catch (SocketException s) {
                        if (s.getMessage().equals("Connection reset by peer: socket write error")) {//���Է�ȡ������ʱ����
                            System.err.println("�Է�����ֹͣ����");
                            s.printStackTrace();
                        }
                        break;//����whileѭ��
                    }
                    currentLocal = currentLocal + n;//��ǰ�����������
                }
                fileInputStream.close();
                currentFileCount = i+1;
            }
            
            //����·���ļ�
            FileOutputStream fileOutputStream = new FileOutputStream("p2pLocalTransmission·���浵.txt");
        	OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        	BufferedWriter bufferedWriter =new BufferedWriter(outputStreamWriter);
        	
        	for(int i=0;i<fileName.size();i++) {
        		bufferedWriter.write(fileName.get(i)+"\t"+filePath.get(i));
        		bufferedWriter.newLine();
        	}
        	
        	bufferedWriter.close();
        }
    }
    
    /**
     * ���÷����ļ���·��
     * @return
     */
    public void setFilePath(ArrayList<String> filesPath) {
    	this.filesPath = filesPath;
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