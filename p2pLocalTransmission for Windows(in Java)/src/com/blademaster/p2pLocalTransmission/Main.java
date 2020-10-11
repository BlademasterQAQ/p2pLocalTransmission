package com.blademaster.p2pLocalTransmission;

import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;

import com.blademaster.p2pLocalTransmission.netManager.HotspotManager;
import com.blademaster.p2pLocalTransmission.netManager.WiFiManager;
import java.awt.SystemColor;
import javax.swing.JProgressBar;

public class Main extends JFrame {
	private int message_POST = 30001;
	private int file_POST = 30002;
	private String host = null;

	private Object synchronizeReceiveMessage = new Object();// 同步文字接收
	private Object synchronizeSendFile = new Object();// 同步文件发送
	private Object synchronizeReceiveFile = new Object();// 同步文件接收

	private ArrayList<String> globalHostName = new ArrayList<String>();
	private ArrayList<String> globalConnecterIP = new ArrayList<String>();
	private ArrayList<String> globalMyIP = new ArrayList<String>();

	java.util.List<File> files;// 从鼠标拖动文件抓取的File对象
	private JTextField textField_information;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main frame = new Main();// 类的初始化生成frame
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
		// ********************UI控件创建区***********************
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 437, 256);
		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		// 创建用于存储单选控件的对象
		RadioButtonScrollPane radioButtonScrollPane = new RadioButtonScrollPane();
		radioButtonScrollPane.setBounds(34, 80, 300, 70);
		contentPane.add(radioButtonScrollPane);

		JTextArea textArea_send = new JTextArea();
		textArea_send.setDragEnabled(true);
//		textArea_send.setDropMode(DropMode.INSERT);
//		DropTarget dropTarget = new DropTarget(textArea_send, new DropTargetListener() {
//			
//			@Override
//			public void dropActionChanged(DropTargetDragEvent dtde) {
//				// TODO 自动生成的方法存根
//				
//			}
//			
//			@Override
//			public void drop(DropTargetDropEvent dtde) {
//				// TODO 自动生成的方法存根
//				Transferable data = dtde.getTransferable();
//		        if (data.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
//		            try {
//						java.util.List<File> list = (java.util.List<File>) data.getTransferData(
//						    DataFlavor.javaFileListFlavor);
//						System.err.println(list.size());
//					} catch (UnsupportedFlavorException | IOException e) {
//						// TODO 自动生成的 catch 块
//						e.printStackTrace();
//					}
//		           
//		        }
//				textArea_send.setText(String.valueOf(dtde.getTransferable()));
//			}
//			
//			@Override
//			public void dragOver(DropTargetDragEvent dtde) {
//				// TODO 自动生成的方法存根
//				
//			}
//			
//			@Override
//			public void dragExit(DropTargetEvent dte) {
//				// TODO 自动生成的方法存根
//				
//			}
//			
//			@Override
//			public void dragEnter(DropTargetDragEvent dtde) {
//				// TODO 自动生成的方法存根
//				
//			}
//		});
//		textArea_send.setDropTarget(dropTarget);
//		dropTarget.
		
		textArea_send.setEditable(false);
		textArea_send.setBounds(34, 15, 292, 44);
		textArea_send.setText("请将文件拖动到此处");
		textArea_send.setColumns(10);

		JScrollPane jScrollPane = new JScrollPane(textArea_send);// 相当于new
																	// JScrollPane()再jScrollPane.setViewportView(textArea_send);
		jScrollPane.setBounds(34, 15, 292, 44);
		// jScrollPane.setViewportView(textArea_send);
		contentPane.add(jScrollPane);

		textArea_send.setTransferHandler(new TransferHandler() {// 设置文件拖动获取路径
			@Override
			public boolean importData(JComponent comp, Transferable t) {
				// TODO 自动生成的方法存根
				try {
					files = (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);// 只有List类型可以成功强制转换文件们

					String string = "";
					for (File f : files) {
						string = string + f.getPath() + "\n";
					}
					textArea_send.setText(string);

					return true;
				} catch (Exception e) {

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

					// 开启发送文件线程
					SendFilesThread sendFilesThread = new SendFilesThread(host, file_POST, synchronizeSendFile);
					ArrayList<String> filesPath = new ArrayList<>();
					for (File f : files) {
						filesPath.add(f.getPath());
					}
					// filesPath.add(textArea_send.getText());
					sendFilesThread.setFilePath(filesPath);
					sendFilesThread.start();

					new Thread(new Runnable() {// 当文件正在发送时
						@Override
						public void run() {
							while (sendFilesThread.getCurrentLocal() == 0
									|| sendFilesThread.getFile_length() > sendFilesThread.getCurrentLocal()) {
								textField_information.setText("正在发送：" + sendFilesThread.getCurrentLocal() + "/"
										+ sendFilesThread.getFile_length());
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
		textArea.setEditable(false);
		textArea.setBackground(SystemColor.control);
		textArea.setText("\u4F20\u8F93\u72B6\u6001\uFF1A");
		textArea.setBounds(7, 185, 60, 24);
		contentPane.add(textArea);

		JTextArea txtip = new JTextArea();
		txtip.setText("网络状态：   对方ip                  本机ip                对方名称");
		txtip.setColumns(10);
		txtip.setBounds(7, 60, 305, 23);
		txtip.setBackground(null);
		txtip.setEditable(false);

		contentPane.add(txtip);
		
		textField_information = new JTextField();
		textField_information.setEditable(false);
		textField_information.setBounds(311, 187, 80, 21);
		contentPane.add(textField_information);
		textField_information.setColumns(10);
		
		JPanel panel = new JPanel();
		panel.setBounds(71, 155, 209, 62);
		contentPane.add(panel);
		panel.setLayout(null);
		
		JProgressBar progressBar_single = new JProgressBar();
		progressBar_single.setBounds(0, 21, 209, 17);
		panel.add(progressBar_single);
		
		JProgressBar progressBar_total = new JProgressBar();
		progressBar_total.setBounds(0, 45, 209, 17);
		panel.add(progressBar_total);
		
		JTextArea textArea_filesStatus = new JTextArea();
		textArea_filesStatus.setBounds(169, 0, 46, 23);
		panel.add(textArea_filesStatus);
		textArea_filesStatus.setBackground(SystemColor.control);
		textArea_filesStatus.setText("0/0");
		textArea_filesStatus.setEditable(false);
		
		JTextArea textArea_remainTime = new JTextArea();
		textArea_remainTime.setBackground(SystemColor.control);
		textArea_remainTime.setBounds(296, 160, 101, 24);
		contentPane.add(textArea_remainTime);

		// *******************线程创建区*********************
		// 获取host线程
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO 自动生成的方法存根
				while (true) {
					ArrayList<String> hostName = new ArrayList<String>();
					ArrayList<String> connecterIP = new ArrayList<String>();
					ArrayList<String> myIP = new ArrayList<String>();

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

					// 若网络状态发生改变，修改全局的属性
					if (!globalHostName.equals(hostName) || !globalConnecterIP.equals(connecterIP)
							|| !globalMyIP.equals(myIP)) {
						globalHostName = hostName;
						globalConnecterIP = connecterIP;
						globalMyIP = myIP;
					}

					radioButtonScrollPane.setRadioButtonsKeepStatus(radioButtonName);
					// System.out.println(host);

					// 线程睡眠
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

		// **************************同步线程区**************************

		// 用于同步接收信息线程的线程（接收线程接收到信息后通知该线程执行相应的操作）
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
							host = receiveMessageThread.getConnecterIP();// 用于回复“成功接收文件”

							// 接收文件部分
							if (message.equals("请求发送文件")) {// 接收到对方"请求发送文件"时，准备接收文件

								textField_information.setText("开始接收文件");
								// 等待接收
								ReceiveFilesThread receiveFilesThread = new ReceiveFilesThread(file_POST,
										synchronizeReceiveFile);
								receiveFilesThread.start();

								new Thread(new Runnable() {// 当文件正在接受时
									@Override
									public void run() {
										int timeout = 5000;//延时5秒
										while(--timeout != 0) {
											if(receiveFilesThread.getTotalCurrentLocal() != 0 && receiveFilesThread.getTotalFilesLength() != 0)
												break;
											else {
												try {
													Thread.sleep(1);
												} catch (InterruptedException e) {
													// TODO 自动生成的 catch 块
													e.printStackTrace();
												}
											}
										}
										if(timeout == 0) {
											textField_information.setText("接收文件超时");
											return;//退出
										}
										Calendar start_calendar = Calendar.getInstance();
										//开始接收文件
										while (receiveFilesThread.getTotalCurrentLocal() != receiveFilesThread.getTotalFilesLength()) {
											textArea_filesStatus.setText(receiveFilesThread.getSingleFileCount() + "/" + receiveFilesThread.getTotalFilesCount());//正在发送的文件和文件总数
											//单个文件进度
											progressBar_single.setStringPainted(true);
											progressBar_single.setMaximum(receiveFilesThread.getSingleFileLength());
											progressBar_single.setValue(receiveFilesThread.getSingleCurrentLocal());
											//总体进度
											progressBar_total.setStringPainted(true);
											progressBar_total.setMaximum(receiveFilesThread.getTotalFilesLength());
											progressBar_total.setValue(receiveFilesThread.getTotalCurrentLocal());
											//剩余时间
											textArea_remainTime.setText("剩余时间：" + remainTimeCalculate(start_calendar, receiveFilesThread.getTotalCurrentLocal(), receiveFilesThread.getTotalFilesLength()));
										}
										//接收完成
										//progressBar_single.setString("成功接收");
										//textField_information.setText("");
									}
								}).start();

							}
							// 发送文件部分
							if (message.equals("成功接收文件")) {// 接收到对方"成功接收文件"时，显示成功发送
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

		// 用于同步发送文件线程的线程
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

		// 用于同步接收文件线程的线程
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
							SendMessageThread sendMessageThread = new SendMessageThread(host, message_POST);// host应从前面的接收线程获取（为请求发送消息的主机的ip）
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
	private String remainTimeCalculate(Calendar start_calendar, int totalCurrentLocal, int totalFilesLength) {
		Calendar calendar = Calendar.getInstance();
		long costTimeInMill = calendar.getTimeInMillis() - start_calendar.getTimeInMillis();//已经消耗的时间
		long remainTimeInMill = (totalFilesLength - totalCurrentLocal) * costTimeInMill / totalCurrentLocal;
		calendar.setTimeInMillis(remainTimeInMill);
		String remainMin = Integer.valueOf(calendar.get(Calendar.MINUTE)).toString();
		String remainSec = Integer.valueOf(calendar.get(Calendar.SECOND)).toString();
		return "0".repeat(2 - remainMin.length()) + remainMin + ":" + "0".repeat(2 - remainSec.length()) + remainSec;
	}
}
