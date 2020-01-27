package com.blademaster.p2pLocalTransmission;

import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;

import com.blademaster.p2pLocalTransmission.netManager.HotspotManager;
import com.blademaster.p2pLocalTransmission.netManager.WiFiManager;

public class Main extends JFrame{
	private int message_POST=30001;
	private int file_POST=30002;
	private String host=null;
	
	private Object synchronizeReceiveMessage=new Object();//ͬ�����ֽ���
	private Object synchronizeSendFile=new Object();//ͬ���ļ�����
	private Object synchronizeReceiveFile=new Object();//ͬ���ļ�����
	
	private ArrayList<String> globalHostName=new ArrayList<String>();
	private ArrayList<String> globalConnecterIP=new ArrayList<String>();
	private ArrayList<String> globalMyIP=new ArrayList<String>();
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main frame = new Main();//��ĳ�ʼ������frame
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
	}	
	

	/**
	 * Create the frame.
	 */
	public Main() {
		//********************UI�ؼ�������***********************
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 448, 231);
		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		//�������ڴ洢��ѡ�ؼ��Ķ���
		RadioButtonScrollPane radioButtonScrollPane=new RadioButtonScrollPane();
		radioButtonScrollPane.setBounds(34, 80, 300, 70);
		contentPane.add(radioButtonScrollPane);
		
		JTextField textField_send = new JTextField();
		textField_send.setBounds(34, 15, 292, 44);
		textField_send.setText("�뽫�ļ��϶����˴�");
		
		textField_send.setTransferHandler(new TransferHandler() {//�����ļ��϶���ȡ·��
			@Override
			public boolean importData(JComponent comp, Transferable t) {
				// TODO �Զ����ɵķ������
				try {
                    Object o = t.getTransferData(DataFlavor.javaFileListFlavor);
                    
                    String filepath = o.toString();
                    System.err.println(filepath);
                    if (filepath.startsWith("[")) {
                        filepath = filepath.substring(1);
                    }
                    if (filepath.endsWith("]")) {
                        filepath = filepath.substring(0, filepath.length() - 1);
                    }
                    textField_send.setText(filepath);
                    
                    return true;
                }
                catch (Exception e) {
                	
                }
                return false;
			}
			@Override
            public boolean canImport(JComponent comp, DataFlavor[] flavors) {
                for (int i = 0; i < flavors.length; i++) {
                    if (DataFlavor.javaFileListFlavor.equals(flavors[i])) {
                        return true;
                    }
                }
                return false;
            }
		});
		
		contentPane.add(textField_send);
		textField_send.setColumns(10);
		
		JTextField textField_information = new JTextField();
		textField_information.setColumns(10);
		textField_information.setBounds(140, 157, 198, 23);
		contentPane.add(textField_information);
		
		JButton send_button = new JButton("����");
		send_button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO �Զ����ɵķ������
				if (radioButtonScrollPane.indexSelectedRadioButton() != -1) {
					System.out.println(globalConnecterIP.size());
					host = globalConnecterIP.get(radioButtonScrollPane.indexSelectedRadioButton());
					
					SendMessageThread sendMessageThread = new SendMessageThread(host, message_POST);
					sendMessageThread.setMessage("�������ļ�");
					sendMessageThread.start();
					
					//���������ļ��߳�
					SendFilesThread sendFilesThread = new SendFilesThread(host, file_POST,synchronizeSendFile);
					ArrayList<String> filesPath = new ArrayList<>();
					filesPath.add(textField_send.getText());
					sendFilesThread.setFilePath(filesPath);
					sendFilesThread.start();
					
					new Thread(new Runnable() {//���ļ����ڷ���ʱ
	                    @Override
	                    public void run() {
	                        while (sendFilesThread.getCurrentLocal()==0||sendFilesThread.getFile_length()>sendFilesThread.getCurrentLocal()){
	                        	textField_information.setText("���ڷ��ͣ�"+sendFilesThread.getCurrentLocal()+"/"+sendFilesThread.getFile_length());
	                        }
	                        textField_information.setText("");
	                    }
	                }).start();
				}
			}
		});
		send_button.setBounds(336, 27, 80, 23);
		contentPane.add(send_button);
		
		JTextArea textArea = new JTextArea();
		textArea.setText("������Ϣ��");
		textArea.setBounds(74, 157, 60, 24);
		contentPane.add(textArea);
		
		JTextArea txtip = new JTextArea();
		txtip.setText("����״̬��   �Է�ip                  ����ip                �Է�����");
		txtip.setColumns(10);
		txtip.setBounds(7, 60, 305, 23);
		txtip.setBackground(null);
		txtip.setEditable(false);
		
		contentPane.add(txtip);
		
		
		//*******************�̴߳�����*********************
		// ��ȡhost�߳�
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO �Զ����ɵķ������
				while (true) {
					ArrayList<String> hostName=new ArrayList<String>();
					ArrayList<String> connecterIP=new ArrayList<String>();
					ArrayList<String> myIP=new ArrayList<String>();

					HotspotManager hotspotManager = new HotspotManager();
					if (hotspotManager.getConnecterIP().size() > 0) {// ������ipʱ
						hostName = hotspotManager.getConnecterName();
						connecterIP = hotspotManager.getConnecterIP();
						myIP = hotspotManager.getMyIP();
						// host=hotspotManager.getConnecterIP().get(0);
					}

					WiFiManager wiFiManager = new WiFiManager();
					if (wiFiManager.isWiFiConnected()) {
						hostName.add(wiFiManager.getConnectingWiFiSSID());
						connecterIP.add(wiFiManager.getConnecterIP());
						myIP.add(wiFiManager.getMyIP());
						// host=wiFiManager.getConnecterIP();
					} 

					// ���ݻ�õ���Ϣ�ػ�RadioButtonScrollPane
					String[] radioButtonName = new String[hostName.size()];
					for (int i = 0; i < hostName.size(); i++) {
						radioButtonName[i] = connecterIP.get(i) + "   " + myIP.get(i) + "   " + hostName.get(i);
					}
					
					//������״̬�����ı䣬�޸�ȫ�ֵ�����
					if(!globalHostName.equals(hostName)||!globalConnecterIP.equals(connecterIP)||!globalMyIP.equals(myIP)) {
						globalHostName=hostName;
						globalConnecterIP=connecterIP;
						globalMyIP=myIP;
					}
					
					radioButtonScrollPane.setRadioButtonsKeepStatus(radioButtonName);
					// System.out.println(host);
					
					//�߳�˯��
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO �Զ����ɵ� catch ��
						e.printStackTrace();
					}
				}
			}
		}).start();

		ReceiveMessageLoopThread receiveMessageThread = new ReceiveMessageLoopThread(message_POST,
				synchronizeReceiveMessage);
		receiveMessageThread.start();

		
		//**************************ͬ���߳���**************************
		
		//����ͬ��������Ϣ�̵߳��̣߳������߳̽��յ���Ϣ��֪ͨ���߳�ִ����Ӧ�Ĳ�����
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO �Զ����ɵķ������

				while (true) {// ע������synchronized�������ܳ�����ͬһ���߳��ڣ���Ȼ���ǻụ�����������ܶ�������
					synchronized (synchronizeReceiveMessage) {// ����������Ϣͬ����
						try {
							synchronizeReceiveMessage.wait();// ����

							// �����ļ�����
							String message = receiveMessageThread.getMessage();
							host = receiveMessageThread.getConnecterIP();//���ڻظ����ɹ������ļ���
							
							//�����ļ�����
							if (message.equals("�������ļ�")) {// ���յ��Է�"�������ļ�"ʱ��׼�������ļ�
							
								textField_information.setText("��ʼ�����ļ�");
								// �ȴ�����
								ReceiveFilesThread receiveFilesThread = new ReceiveFilesThread(file_POST,
										synchronizeReceiveFile);
								receiveFilesThread.start();
								
								new Thread(new Runnable() {//���ļ����ڽ���ʱ
		                            @Override
		                            public void run() {
		                                while (receiveFilesThread.getCurrentLocal()==0||receiveFilesThread.getFile_length()>receiveFilesThread.getCurrentLocal()){
		                                    textField_information.setText("���ڽ��գ�"+receiveFilesThread.getCurrentLocal()+"/"+receiveFilesThread.getFile_length());
		                                }
		                                textField_information.setText("");
		                            }
		                        }).start();
								
							}
							//�����ļ�����
		                    if(message.equals("�ɹ������ļ�")) {//���յ��Է�"�ɹ������ļ�"ʱ����ʾ�ɹ�����
		                    	textField_information.setText("�ɹ������ļ�");
		                    }

						} catch (InterruptedException e) {
							// TODO �Զ����ɵ� catch ��0
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
		
		//����ͬ�������ļ��̵߳��߳�
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO �Զ����ɵķ������
				while (true) {
					synchronized (synchronizeSendFile) {// �����ļ�ͬ����
						try {
							synchronizeSendFile.wait();
							// ��ʼ�����ļ���
							textField_information.setText("��ʼ�����ļ�");
						} catch (InterruptedException e) {
							// TODO �Զ����ɵ� catch ��
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
		
		//����ͬ�������ļ��̵߳��߳�
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO �Զ����ɵķ������
				while (true) {
					synchronized (synchronizeReceiveFile) {// �����ļ�ͬ����
						try {
							synchronizeReceiveFile.wait();
							// ���յ��ļ���
							textField_information.setText("�ɹ������ļ�");
							SendMessageThread sendMessageThread=new SendMessageThread(host,message_POST);//hostӦ��ǰ��Ľ����̻߳�ȡ��Ϊ��������Ϣ��������ip��
		                    sendMessageThread.setMessage("�ɹ������ļ�");
		                    sendMessageThread.start();
						} catch (InterruptedException e) {
							// TODO �Զ����ɵ� catch ��
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
	}

}
