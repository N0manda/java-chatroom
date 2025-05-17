package com.chatroom.client.ui;

import com.chatroom.client.ChatClient;
import com.chatroom.client.MessageHandler;
import com.chatroom.common.model.Message;
import com.chatroom.common.model.MessageType;
import com.chatroom.common.model.User;
import com.chatroom.common.network.ChatRequest;
import com.chatroom.common.network.ChatResponse;
import com.chatroom.common.network.RequestType;
import com.chatroom.common.network.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 主聊天窗口
 */
public class ChatMainFrame extends JFrame implements MessageHandler.MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(ChatMainFrame.class);
    
    /**
     * 最大聊天窗口数
     */
    private static final int MAX_CHAT_WINDOWS = 5;
    
    /**
     * 客户端引用
     */
    private final ChatClient client;
    
    /**
     * 用户列表
     */
    private JList<User> userList;
    
    /**
     * 活跃窗口
     */
    private final List<ChatPanel> chatPanels;
    
    /**
     * 聊天面板
     */
    private JTabbedPane tabbedPane;
    
    /**
     * 构造方法
     * 
     * @param client 客户端引用
     */
    public ChatMainFrame(ChatClient client) {
        this.client = client;
        this.chatPanels = new ArrayList<>();
        
        initComponents();
        setupListeners();
        
        // 初始化时刷新用户列表
        refreshUserList();
    }
    
    /**
     * 初始化界面组件
     */
    private void initComponents() {
        setTitle("聊天室 - " + client.getCurrentUser().getUsername());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // 创建菜单栏
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("文件");
        JMenuItem logoutItem = new JMenuItem("注销");
        JMenuItem exitItem = new JMenuItem("退出");
        fileMenu.add(logoutItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        // 注销菜单项事件
        logoutItem.addActionListener(e -> logout());
        
        // 退出菜单项事件
        exitItem.addActionListener(e -> exit());
        
        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(5);
        
        // 用户列表面板
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("在线用户"));
        
        // 创建用户列表
        userList = new JList<>();
        userList.setCellRenderer(new UserListCellRenderer());
        JScrollPane userListScrollPane = new JScrollPane(userList);
        leftPanel.add(userListScrollPane, BorderLayout.CENTER);
        
        // 刷新按钮
        JButton refreshButton = new JButton("刷新用户列表");
        leftPanel.add(refreshButton, BorderLayout.SOUTH);
        refreshButton.addActionListener(e -> refreshUserList());
        
        // 创建聊天选项卡面板
        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        
        // 添加到分割面板
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(tabbedPane);
        
        // 添加到内容面板
        setContentPane(splitPane);
        
        // 创建默认的系统消息面板
        addSystemPanel();
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        
        // 用户列表双击事件
        userList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = userList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        User selectedUser = userList.getModel().getElementAt(index);
                        // 不能和自己聊天
                        if (!selectedUser.getUserId().equals(client.getCurrentUser().getUserId())) {
                            openChatWithUser(selectedUser);
                        }
                    }
                }
            }
        });
        
        // 注册消息监听器
        client.getMessageHandler().addMessageListener(this);
    }
    
    /**
     * 添加系统消息面板
     */
    private void addSystemPanel() {
        ChatPanel systemPanel = new ChatPanel("系统消息", null, client);
        chatPanels.add(systemPanel);
        tabbedPane.addTab("系统消息", null, systemPanel, "系统消息");
    }
    
    /**
     * 打开与用户的聊天
     * 
     * @param user 目标用户
     */
    private void openChatWithUser(User user) {
        // 检查用户参数
        if (user == null || user.getUserId() == null) {
            logger.error("无法打开聊天窗口：用户或用户ID为空");
            return;
        }
        
        logger.info("尝试打开与用户 {} 的聊天窗口", user.getUsername());
        
        // 检查是否已存在与该用户的聊天
        for (int i = 0; i < chatPanels.size(); i++) {
            ChatPanel panel = chatPanels.get(i);
            User targetUser = panel.getTargetUser();
            
            if (targetUser != null && targetUser.getUserId() != null && 
                targetUser.getUserId().equals(user.getUserId())) {
                // 切换到已有的选项卡
                tabbedPane.setSelectedIndex(i);
                logger.debug("已切换到已有的聊天窗口，索引: {}", i);
                return;
            }
        }
        
        // 检查是否超过最大窗口数
        if (chatPanels.size() >= MAX_CHAT_WINDOWS) {
            JOptionPane.showMessageDialog(this, "最多只能打开 " + MAX_CHAT_WINDOWS + " 个聊天窗口", "警告", JOptionPane.WARNING_MESSAGE);
            logger.warn("已达到最大聊天窗口数: {}", MAX_CHAT_WINDOWS);
            return;
        }
        
        // 创建新的聊天面板
        ChatPanel chatPanel = new ChatPanel(user.getUsername(), user, client);
        chatPanels.add(chatPanel);
        
        // 添加到选项卡
        tabbedPane.addTab(user.getUsername(), null, chatPanel, "与 " + user.getUsername() + " 聊天");
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        logger.info("已打开与用户 {} 的新聊天窗口", user.getUsername());
    }
    
    /**
     * 刷新用户列表
     */
    private void refreshUserList() {
        logger.info("正在刷新用户列表...");
        
        // 检查当前用户是否存在
        if (client.getCurrentUser() == null) {
            logger.warn("当前用户为空，无法获取用户列表");
            return;
        }
        
        // 发送获取用户列表的请求
        ChatRequest request = new ChatRequest(
                RequestType.GET_USERS,
                client.getCurrentUser(),
                null
        );
        
        logger.debug("发送获取用户列表请求: {}", request);
        
        // 使用独立的响应监听器处理响应
        final MessageHandler.ResponseListener responseListener = new MessageHandler.ResponseListener() {
            @Override
            public void onResponseReceived(ChatResponse response) {
                if (response.getType() == ResponseType.USER_LIST) {
                    if (response.isSuccess()) {
                        logger.info("收到用户列表响应：{}", response);
                        Object data = response.getData();
                        if (data instanceof Object[]) {
                            Object[] userArray = (Object[]) data;
                            // 转换为User列表
                            java.util.List<User> users = new java.util.ArrayList<>();
                            
                            for (Object obj : userArray) {
                                if (obj instanceof User) {
                                    users.add((User) obj);
                                    logger.debug("添加用户到列表: {}", ((User) obj).getUsername());
                                }
                            }
                            
                            // 更新UI上的用户列表
                            SwingUtilities.invokeLater(() -> {
                                userList.setListData(users.toArray(new User[0]));
                                logger.info("用户列表已更新，共 {} 个用户", users.size());
                            });
                        } else {
                            logger.warn("用户列表数据格式错误: {}", data);
                        }
                    } else {
                        logger.warn("获取用户列表失败: {}", response.getMessage());
                    }
                    
                    // 使用后移除监听器，防止重复处理
                    client.getMessageHandler().removeResponseListener(this);
                }
            }
        };
        
        // 先注册监听器，再发送请求
        client.getMessageHandler().addResponseListener(responseListener);
        if (!client.sendRequest(request)) {
            // 如果发送失败，移除监听器
            logger.error("发送获取用户列表请求失败");
            client.getMessageHandler().removeResponseListener(responseListener);
        }
    }
    
    /**
     * 注销
     */
    private void logout() {
        int result = JOptionPane.showConfirmDialog(this, "确定要注销吗？", "注销确认", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            client.logout();
            
            // 打开登录界面
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame(client);
                loginFrame.setVisible(true);
                dispose();
            });
        }
    }
    
    /**
     * 退出程序
     */
    private void exit() {
        int result = JOptionPane.showConfirmDialog(this, "确定要退出吗？", "退出确认", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            client.disconnect();
            System.exit(0);
        }
    }
    
    @Override
    public void onMessageReceived(Message message) {
        SwingUtilities.invokeLater(() -> {
            // 处理接收到的消息
            if (message.getType() == MessageType.SYSTEM) {
                // 系统消息，显示在系统消息面板
                ChatPanel systemPanel = chatPanels.get(0);
                systemPanel.appendMessage(message);
            } else {
                // 个人消息，找到对应的聊天面板
                User sender = message.getSender();
                boolean handled = false;
                
                if (sender != null) {
                    for (int i = 0; i < chatPanels.size(); i++) {
                        ChatPanel panel = chatPanels.get(i);
                        User targetUser = panel.getTargetUser();
                        
                        if (targetUser != null && targetUser.getUserId().equals(sender.getUserId())) {
                            // 找到对应的聊天面板
                            panel.appendMessage(message);
                            handled = true;
                            
                            // 如果不是当前选中的选项卡，标记为有新消息
                            if (i != tabbedPane.getSelectedIndex()) {
                                tabbedPane.setTitleAt(i, "* " + targetUser.getUsername());
                            }
                            
                            break;
                        }
                    }
                }
                
                if (!handled) {
                    // 如果没有对应的聊天面板，打开一个新的
                    if (sender != null && !sender.getUserId().equals(client.getCurrentUser().getUserId())) {
                        openChatWithUser(sender);
                        // 添加消息到新打开的面板
                        chatPanels.get(chatPanels.size() - 1).appendMessage(message);
                    }
                }
            }
        });
    }
    
    /**
     * 用户列表单元格渲染器
     */
    private static class UserListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof User) {
                User user = (User) value;
                setText(user.getUsername());
            }
            
            return c;
        }
    }
    
    /**
     * 聊天面板
     */
    private static class ChatPanel extends JPanel {
        /**
         * 目标用户
         */
        private final User targetUser;
        
        /**
         * 聊天记录
         */
        private final JTextPane chatArea;
        
        /**
         * 消息输入框
         */
        private final JTextArea inputArea;
        
        /**
         * 客户端引用
         */
        private final ChatClient client;
        
        /**
         * 日期格式化
         */
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        
        /**
         * 构造方法
         * 
         * @param title 面板标题
         * @param targetUser 目标用户
         * @param client 客户端引用
         */
        public ChatPanel(String title, User targetUser, ChatClient client) {
            this.targetUser = targetUser;
            this.client = client;
            
            // 检查客户端是否有效
            if (client == null) {
                throw new IllegalArgumentException("客户端引用不能为空");
            }
            
            setLayout(new BorderLayout());
            
            // 聊天记录区域
            chatArea = new JTextPane();
            chatArea.setEditable(false);
            chatArea.setContentType("text/html");
            JScrollPane chatScrollPane = new JScrollPane(chatArea);
            chatScrollPane.setPreferredSize(new Dimension(400, 300));
            
            // 输入区域
            inputArea = new JTextArea();
            inputArea.setLineWrap(true);
            inputArea.setWrapStyleWord(true);
            JScrollPane inputScrollPane = new JScrollPane(inputArea);
            inputScrollPane.setPreferredSize(new Dimension(400, 100));
            
            // 发送按钮
            JButton sendButton = new JButton("发送");
            sendButton.addActionListener(e -> sendMessage());
            
            // 输入面板
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.add(inputScrollPane, BorderLayout.CENTER);
            
            // 按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(sendButton);
            inputPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            // 添加组件
            add(chatScrollPane, BorderLayout.CENTER);
            add(inputPanel, BorderLayout.SOUTH);
            
            // 注册按键监听
            inputArea.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    // Ctrl+Enter 发送消息
                    if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        sendMessage();
                        e.consume();
                    }
                }
            });
            
            // 如果是系统消息面板，禁用输入
            if (targetUser == null) {
                inputArea.setEnabled(false);
                sendButton.setEnabled(false);
                
                // 添加系统面板说明
                String welcomeText = "<html><body><div style='color:blue; font-family:Microsoft YaHei;'>这是系统消息面板，将显示系统通知和公告。</div></body></html>";
                chatArea.setText(welcomeText);
            } else {
                // 添加用户聊天欢迎消息
                String welcomeText = "<html><body><div style='color:blue; font-family:Microsoft YaHei;'>已开始与 " + targetUser.getUsername() + " 的聊天。</div></body></html>";
                chatArea.setText(welcomeText);
            }
        }
        
        /**
         * 发送消息
         */
        private void sendMessage() {
            String content = inputArea.getText().trim();
            if (content.isEmpty()) return;
            
            // 检查目标用户是否为空
            if (targetUser == null) {
                JOptionPane.showMessageDialog(this, "无法发送消息：目标用户不存在", "发送失败", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 创建消息对象
            Message message = Message.createTextMessage(
                    client.getCurrentUser(),
                    targetUser.getUserId(),
                    content,
                    false
            );
            
            // 发送消息
            if (client.sendMessage(message)) {
                // 清空输入框
                inputArea.setText("");
                
                // 添加消息到聊天区域
                appendMessage(message);
            }
        }
        
        /**
         * 添加消息到聊天区域
         * 
         * @param message 消息对象
         */
        public void appendMessage(Message message) {
            String time = dateFormat.format(message.getTimestamp() != null ? message.getTimestamp() : new Date());
            String sender = message.getSender() != null ? message.getSender().getUsername() : "系统";
            String content = message.getContent();
            
            // 构建HTML消息
            String color = message.getType() == MessageType.SYSTEM ? "gray" : 
                          (message.getSender() != null && message.getSender().getUserId().equals(client.getCurrentUser().getUserId()) ? "green" : "blue");
            
            String htmlMessage = String.format("<div style='color:%s; font-family:Microsoft YaHei;'>[%s] %s: %s</div>", 
                    color, time, sender, content);
            
            // 添加到聊天区域
            String currentText = chatArea.getText();
            chatArea.setText(currentText.replace("</body></html>", "") + htmlMessage + "</body></html>");
            
            // 滚动到底部
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
        
        /**
         * 获取目标用户
         * 
         * @return 目标用户
         */
        public User getTargetUser() {
            return targetUser;
        }
    }
} 