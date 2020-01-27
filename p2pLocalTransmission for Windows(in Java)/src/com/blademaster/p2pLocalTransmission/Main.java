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
	
	private Object synchronizeReceiveMessage=new Object();//同步文字接收
	private Object synchronizeSendFile=new Object();//同步文件发送
	private Object synchronizeReceiveFile=new Object();//同步文件接收
	
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
					Main frame = new Main();//类的初始化生成frame
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
		//********************UI控件创建区***********************
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 448, 231);
		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		//创建用于存储单选控件的对象
		RadioButtonScrollPane radioButtonScrollPane=new RadioButtonScrollPane();
		radioButtonScrollPane.setBounds(34, 80, 300, 70);
		contentPane.add(radioButtonScrollPane);
		
		JTextField textField_send = new JTextField();
		textField_send.setBounds(34, 15, 292, 44);
		textField_send.setText("请将文件拖动到此处");
		
		textField_send.setTransferHandler(new TransferHandler() {//设置文件拖动获取路径
			@Override
			public boolean importData(JComponent comp, Transferable t) {
				// TODO 自动生成的方法存根
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
		
		JButton send_button = new JButton("发送");
		send_button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO 自动生成的方法存根
				if (radioButtonScrollPane.indexSelectedRadioButton() != -1) {
					System.out.println(globalConnecterIP.size());
					host = globalConnecterIP.get(radioButtonScrollPane.indexSelectedRadioButton());
					
					SendMessageThread sendMessageThread = new SendMessageThread(host, message_POST);
					sendMessageThread.setMessage("请求发送文件");
					sendMessageThread.start();
					
					//开启发送文件线程
					SendFilesThread sendFilesThread = new SendFilesThread(host, file_POST,synchronizeSendFile);
					ArrayList<String> filesPath = new ArrayList<>();
					filesPath.add(textField_send.getText());
					sendFilesThread.setFilePath(filesPath);
					sendFilesThread.start();
					
					new Thread(new Runnable() {//当文件正在发送时
	                    @Override
	                    public void run() {
	                        while (sendFilesThread.getCurrentLocal()==0||sendFilesThread.getFile_length()>sendFilesThread.getCurrentLocal()){
	                        	textField_information.setText("正在发送："+sendFilesThread.getCurrentLocal()+"/"+sendFilesThread.getFile_length());
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
		textArea.setText("程序信息：");
		textArea.setBounds(74, 157, 60, 24);
		contentPane.add(textArea);
		
		JTextArea txtip = new JTextArea();
		txtip.setText("网络状态：   对方ip                  本机ip                对方名称");
		txtip.setColumns(10);
		txtip.setBounds(7, 60, 305, 23);
		txtip.setBackground(null);
		txtip.setEditable(false);
		
		contentPane.add(txtip);
		
		
		//*******************线程创建区*********************
		// 获取host线程
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO 自动生成的方法存根
				while (true) {
					ArrayList<String> hostName=new ArrayList<String>();
					ArrayList<String> connecterIP=new ArrayList<String>();
					ArrayList<String> myIP=new ArrayList<String>();

					HotspotManager hotspotManager = new HotspotManager();
					if (hotspotManager.getConnecterIP().size() > 0) {// 但存在ip时
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

					// 根据获得的信息重绘RadioButtonScrollPane
					String[] radioButtonName = new String[hostName.size()];
					for (int i = 0; i < hostName.size(); i++) {
						radioButtonName[i] = connecterIP.get(i) + "   " + myIP.get(i) + "   " + hostName.get(i);
					}
					
					//若网络状态发生改变，修改全局的属性
					if(!globalHostName.equals(hostName)||!globalConnecterIP.equals(connecterIP)||!globalMyIP.equals(myIP)) {
						globalHostName=hostName;
						globalConnecterIP=connecterIP;
						globalMyIP=myIP;
					}
					
					radioButtonScrollPane.setRadioButtonsKeepStatus(radioButtonName);
					// System.out.println(host);
					
					//线程睡眠
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
				}
			}
		}).start();

		ReceiveMessageLoopThread receiveMessageThread = new ReceiveMessageLoopThread(message_POST,
				synchronizeReceiveMessage);
		receiveMessageThread.start();

		
		//**************************同步线程区**************************
		
		//用于同步接收信息线程的线程（接收线程接收到信息后通知该线程执行相应的操作）
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO 自动生成的方法存根

				while (true) {// 注意两个synchronized锁定不能出现在同一个线程内，不然他们会互相阻塞，不能独立工作
					synchronized (synchronizeReceiveMessage) {// 接收文字信息同步块
						try {
							synchronizeReceiveMessage.wait();// 锁定

							// 接收文件部分
							String message = receiveMessageThread.getMessage();
							host = receiveMessageThread.getConnecterIP();//用于回复“成功接收文件”
							
							//接收文件部分
							if (message.equals("请求发送文件")) {// 接收到对方"请求发送文件"时，准备接收文件
							
								textField_information.setText("开始接收文件");
								// 等待接收
								ReceiveFilesThread receiveFilesThread = new ReceiveFilesThread(file_POST,
										synchronizeReceiveFile);
								receiveFilesThread.start();
								
								new Thread(new Runnable() {//当文件正在接受时
		                            @Override
		                            public void run() {
		                                while (receiveFilesThread.getCurrentLocal()==0||receiveFilesThread.getFile_length()>receiveFilesThread.getCurrentLocal()){
		                                    textField_information.setText("正在接收："+receiveFilesThread.getCurrentLocal()+"/"+receiveFilesThread.getFile_length());
		                                }
		                                textField_information.setText("");
		                            }
		                        }).start();
								
							}
							//发送文件部分
		                    if(message.equals("成功接收文件")) {//接收到对方"成功接收文件"时，显示成功发送
		                    	textField_information.setText("成功发送文件");
		                    }

						} catch (InterruptedException e) {
							// TODO 自动生成的 catch 块0
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
		
		//用于同步发送文件线程的线程
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO 自动生成的方法存根
				while (true) {
					synchronized (synchronizeSendFile) {// 接收文件同步块
						try {
							synchronizeSendFile.wait();
							// 开始发送文件后
							textField_information.setText("开始发送文件");
						} catch (InterruptedException e) {
							// TODO 自动生成的 catch 块
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
		
		//用于同步接收文件线程的线程
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO 自动生成的方法存根
				while (true) {
					synchronized (synchronizeReceiveFile) {// 接收文件同步块
						try {
							synchronizeReceiveFile.wait();
							// 接收到文件后
							textField_information.setText("成功接收文件");
							SendMessageThread sendMessageThread=new SendMessageThread(host,message_POST);//host应从前面的接收线程获取（为请求发送消息的主机的ip）
		                    sendMessageThread.setMessage("成功接收文件");
		                    sendMessageThread.start();
						} catch (InterruptedException e) {
							// TODO 自动生成的 catch 块
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
	}

}
