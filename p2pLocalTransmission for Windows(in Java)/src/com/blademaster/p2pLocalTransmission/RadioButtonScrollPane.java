package com.blademaster.p2pLocalTransmission;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
/**
 * 这是在JScrollPaned上用setViewportView方法添加Panel面板，然后在Panel面板的基础上动态添加单选框的组件。
 * <p>通过 {@link #setRadioButtons(String[])}可以重设所有的按钮。
 * <p>通过 {@link #setRadioButtonsKeepStatus(String[])}在重设的同时，将会保持上一次按键按下的状态（当有同名的按键时使其按下）。
 * <p>通过 {@link #indexSelectedRadioButton()}可获取当前被按下的按键的索引，反回-1时说明无按键被按下或无按键存在。
 * 
 * @see #setRadioButtons(String[])
 * @see #setRadioButtonsKeepStatus(String[])
 * @see #indexSelectedRadioButton()
 * 
 * @author blademaster
 *
 */
public class RadioButtonScrollPane extends JScrollPane{
	
	private JPanel jPanel;
	private JRadioButton[] jRadioButtons;
	
	public RadioButtonScrollPane() {
		// TODO 自动生成的构造函数存根
		
		//新建Panel面板，用于放置按钮
		jPanel=new JPanel();
		jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));//使用垂直方向的Box布局
		
		this.setBounds(10, 100, 300, 100);//对父组件JScrollPane设置位置，或在外部调用RadioButtonScrollPane类的setBounds（者也说明是调用this的setBounds方法），注意JFrame调成绝对布局，不然不会生效
		this.setViewportView(jPanel);//为JScrollPane添加jPanel面板(注意不要用“元件父类”的add方法，不起作用)
	}
	
	/**
	 * 由String数组创建按钮，按钮的个数为Name的长度，每次设置后刷新UI（相当于第一次进行set）
	 * @param name
	 */

	public void setRadioButtons(String[] name) {
		jPanel.removeAll();//删除所有原来的组件，因为后面是用add方法添加，不remove的话会保留原本的组件
		
		jRadioButtons = new JRadioButton[name.length];// 生成单选框组
		ButtonGroup BG = new ButtonGroup();// ButtonGroup不是组件，其只是管理按钮的互斥关系的一个类（用add方法添加到ButtonGroup的按钮将互斥），无需添加到frame
		for (int i = 0; i < name.length; i++) {
			jRadioButtons[i] = new JRadioButton(name[i]);
			
			jPanel.add(jRadioButtons[i]);
			BG.add(jRadioButtons[i]);
		}
		
		jPanel.updateUI();//更新panel的UI
	}
	
	/**
	 * 若有相同名字的按钮被按下，保持按下状态（可能受上一次set和用户操作的影响）
	 * @param name
	 */
	
	public void setRadioButtonsKeepStatus(String[] name) {
		jPanel.removeAll();//删除所有原来的组件，因为后面是用add方法添加，不remove的话会保留原本的组件
		
		String str = null;
		if(indexSelectedRadioButton()!=-1) {
			str = jRadioButtons[indexSelectedRadioButton()].getText();//获取之前被选择的按钮的内容
		}
		
		jRadioButtons = new JRadioButton[name.length];// 生成单选框组
		ButtonGroup BG = new ButtonGroup();// ButtonGroup不是组件，其只是管理按钮的互斥关系的一个类（用add方法添加到ButtonGroup的按钮将互斥），无需添加到frame
		for (int i = 0; i < name.length; i++) {
			jRadioButtons[i] = new JRadioButton(name[i]);
			
			jPanel.add(jRadioButtons[i]);
			BG.add(jRadioButtons[i]);
		}
		
		if (str != null) {
			for (int i = 0; i < name.length; i++) {// 查找是否有之前被按下的同名按键
				if (name[i].equals(str)) {
					jRadioButtons[i].setSelected(true);
				}
			}
		}
		
		jPanel.updateUI();//更新panel的UI
	}
	
	/**
	 * 返回被按下的按钮的索引，未找到或无RadioButton时返回-1
	 * @return
	 */

	public int indexSelectedRadioButton() {
		int index = -1;
		if (jRadioButtons != null) {
			for (int i = 0; i < jRadioButtons.length; i++) {// 对原来被按下的按钮进行查找
				if (jRadioButtons[i].isSelected()) {
					index = i;
				}
			}
		}
		return index;
	}
}
