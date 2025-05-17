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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private final List<MessagePanel> chatPanels;
    
    /**
     * 聊天面板
     */
    private JTabbedPane tabbedPane;
    
    /**
     * 添加一个最近消息缓存，用于去重
     */
    private final java.util.Map<String, Boolean> recentMessageCache = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<String, Boolean>() {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, Boolean> eldest) {
                    return size() > 50; // 最多保留50条消息ID
                }
            });
    
    /**
     * 当前聊天室对象
     */
    private com.chatroom.common.model.ChatGroup publicChatRoom;
    
    /**
     * 构造方法
     * 
     * @param client 客户端引用
     * @param publicChatRoom 公共聊天室对象
     */
    public ChatMainFrame(ChatClient client, com.chatroom.common.model.ChatGroup publicChatRoom) {
        this.client = client;
        this.chatPanels = new ArrayList<>();
        this.publicChatRoom = publicChatRoom;
        
        initComponents();
        setupListeners();
        
        // 初始化时刷新用户列表
        refreshUserList();
        
        // 如果有公共聊天室，自动打开
        if (publicChatRoom != null) {
            openPublicChatRoom();
        }
    }
    
    /**
     * 初始化界面组件
     */
    private void initComponents() {
        setTitle("聊天室 - " + client.getCurrentUser().getUsername());
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        
        try {
            // 设置外观为系统外观
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.error("设置界面外观失败: " + e.getMessage());
        }
        
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
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(5);
        
        // 用户列表面板
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 创建用户信息面板
        JPanel userInfoPanel = new JPanel(new BorderLayout());
        userInfoPanel.setBackground(new Color(240, 240, 240));
        userInfoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // 用户头像和用户名
        JLabel avatarLabel = new JLabel();
        avatarLabel.setIcon(createDefaultAvatar(client.getCurrentUser().getUsername(), 40));
        avatarLabel.setHorizontalAlignment(JLabel.CENTER);
        
        JLabel usernameLabel = new JLabel(client.getCurrentUser().getUsername());
        usernameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        usernameLabel.setHorizontalAlignment(JLabel.CENTER);
        
        JPanel avatarPanel = new JPanel(new BorderLayout());
        avatarPanel.setOpaque(false);
        avatarPanel.add(avatarLabel, BorderLayout.CENTER);
        avatarPanel.add(usernameLabel, BorderLayout.SOUTH);
        
        userInfoPanel.add(avatarPanel, BorderLayout.CENTER);
        
        // 在线用户列表面板
        JPanel userListPanel = new JPanel(new BorderLayout());
        userListPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        // 列表标题
        JLabel titleLabel = new JLabel("在线用户");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 创建用户列表
        userList = new JList<>();
        userList.setCellRenderer(new UserListCellRenderer());
        userList.setFixedCellHeight(50);
        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // 刷新按钮
        JButton refreshButton = new JButton("刷新用户列表");
        refreshButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        refreshButton.addActionListener(e -> refreshUserList());
        
        userListPanel.add(titleLabel, BorderLayout.NORTH);
        userListPanel.add(userListScrollPane, BorderLayout.CENTER);
        userListPanel.add(refreshButton, BorderLayout.SOUTH);
        
        // 添加到左侧面板
        leftPanel.add(userInfoPanel, BorderLayout.NORTH);
        leftPanel.add(userListPanel, BorderLayout.CENTER);
        
        // 创建聊天选项卡面板
        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        
        // 选项卡切换监听器，清除新消息标记
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0) {
                String title = tabbedPane.getTitleAt(selectedIndex);
                if (title.startsWith("* ")) {
                    // 清除新消息标记
                    tabbedPane.setTitleAt(selectedIndex, title.substring(2));
                }
            }
        });
        
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
            MessagePanel panel = chatPanels.get(i);
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
     * 打开公共聊天室面板
     */
    private void openPublicChatRoom() {
        logger.info("打开公共聊天室: {}", publicChatRoom.getGroupName());
        
        // 创建聊天面板
        GroupChatPanel chatPanel = new GroupChatPanel(publicChatRoom.getGroupName(), publicChatRoom, client, userList);
        chatPanels.add(chatPanel);
        
        // 添加到选项卡
        tabbedPane.addTab(publicChatRoom.getGroupName(), null, chatPanel, "公共聊天室");
        tabbedPane.setIconAt(tabbedPane.getTabCount() - 1, createGroupIcon());
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }
    
    /**
     * 创建群组图标
     */
    private ImageIcon createGroupIcon() {
        // 创建一个简单的群组图标
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制背景
        g2d.setColor(new Color(70, 130, 180));  // 钢蓝色
        g2d.fillRect(0, 0, 16, 16);
        
        // 绘制群组符号
        g2d.setColor(Color.WHITE);
        g2d.fillOval(3, 3, 4, 4);
        g2d.fillOval(9, 3, 4, 4);
        g2d.fillOval(3, 9, 4, 4);
        g2d.fillOval(9, 9, 4, 4);
        
        g2d.dispose();
        
        return new ImageIcon(image);
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
                logger.debug("收到响应: {}", response);
                
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
                                if (users.isEmpty()) {
                                    logger.warn("服务器返回的用户列表为空");
                                } else {
                                    logger.info("用户列表不为空，共 {} 个用户", users.size());
                                }
                                
                                // 清除当前用户
                                users.removeIf(user -> user.getUserId().equals(client.getCurrentUser().getUserId()));
                                
                                userList.setModel(new DefaultListModel<User>() {
                                    {
                                        for (User user : users) {
                                            addElement(user);
                                        }
                                    }
                                });
                                logger.info("用户列表已更新，共 {} 个用户", users.size());
                            });
                        } else {
                            logger.warn("用户列表数据格式错误: {}", data);
                        }
                    } else {
                        logger.warn("获取用户列表失败: {}", response.getMessage());
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(ChatMainFrame.this, 
                                "获取用户列表失败: " + response.getMessage(), 
                                "错误", JOptionPane.ERROR_MESSAGE)
                        );
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
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(this, 
                    "发送获取用户列表请求失败，请检查网络连接", 
                    "错误", JOptionPane.ERROR_MESSAGE)
            );
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
        // 检查消息有效性
        if (message == null) {
            logger.error("收到无效消息");
            return;
        }
        
        // 创建消息唯一标识，用于去重
        String messageId = message.getSender() != null ? message.getSender().getUserId() : "system";
        messageId += "_" + (message.getTimestamp() != null ? message.getTimestamp().getTime() : System.currentTimeMillis());
        messageId += "_" + message.getContent();
        
        // 如果是最近处理过的消息，则忽略
        if (recentMessageCache.containsKey(messageId)) {
            logger.debug("忽略重复消息: {}", messageId);
            return;
        }
        
        // 添加到缓存
        recentMessageCache.put(messageId, Boolean.TRUE);
        
        logger.info("收到消息: 发送者={}, 接收者={}, 内容={}, 是否群消息={}", 
            message.getSender() != null ? message.getSender().getUsername() : "系统",
            message.getReceiverId(),
            message.getContent(),
            message.isGroupMessage());
            
        SwingUtilities.invokeLater(() -> {
            // 处理接收到的消息
            if (message.getType() == MessageType.SYSTEM) {
                // 判断是否是群组系统消息
                if (message.isGroupMessage() && publicChatRoom != null && 
                    message.getReceiverId() != null && 
                    message.getReceiverId().equals(publicChatRoom.getGroupId())) {
                    
                    // 找到公共聊天室面板
                    boolean foundPanel = false;
                    for (int i = 0; i < chatPanels.size(); i++) {
                        MessagePanel panel = chatPanels.get(i);
                        com.chatroom.common.model.ChatGroup group = panel.getChatGroup();
                        
                        if (group != null && group.getGroupId().equals(publicChatRoom.getGroupId())) {
                            panel.appendMessage(message);
                            foundPanel = true;
                            
                            // 如果不是当前选中的选项卡，标记为有新消息
                            if (i != tabbedPane.getSelectedIndex()) {
                                tabbedPane.setTitleAt(i, "* " + publicChatRoom.getGroupName());
                            }
                            break;
                        }
                    }
                    
                    // 如果没有找到面板，尝试打开
                    if (!foundPanel && publicChatRoom != null) {
                        openPublicChatRoom();
                        // 将消息添加到新打开的面板
                        chatPanels.get(chatPanels.size() - 1).appendMessage(message);
                    }
                } else {
                    // 其他系统消息显示在系统消息面板
                    logger.debug("处理系统消息");
                    if (!chatPanels.isEmpty()) {
                        chatPanels.get(0).appendMessage(message);
                    } else {
                        logger.error("系统消息面板不存在，无法显示系统消息");
                    }
                }
            } else if (message.isGroupMessage()) {
                // 处理群聊消息
                if (publicChatRoom != null && message.getReceiverId() != null && 
                    message.getReceiverId().equals(publicChatRoom.getGroupId())) {
                    
                    // 查找公共聊天室面板
                    boolean foundPanel = false;
                    for (int i = 0; i < chatPanels.size(); i++) {
                        MessagePanel panel = chatPanels.get(i);
                        com.chatroom.common.model.ChatGroup group = panel.getChatGroup();
                        
                        if (group != null && group.getGroupId().equals(publicChatRoom.getGroupId())) {
                            panel.appendMessage(message);
                            foundPanel = true;
                            
                            // 如果不是当前选中的选项卡，标记为有新消息
                            if (i != tabbedPane.getSelectedIndex()) {
                                tabbedPane.setTitleAt(i, "* " + publicChatRoom.getGroupName());
                            }
                            break;
                        }
                    }
                    
                    // 如果没有找到面板，尝试打开
                    if (!foundPanel && publicChatRoom != null) {
                        openPublicChatRoom();
                        // 将消息添加到新打开的面板
                        chatPanels.get(chatPanels.size() - 1).appendMessage(message);
                    }
                }
            } else {
                // 处理私聊消息
                boolean handled = false;
                User currentUser = client.getCurrentUser();
                User sender = message.getSender();
                String receiverId = message.getReceiverId();
                
                logger.debug("处理用户消息: 当前用户={}, 发送者={}, 接收者={}", 
                    currentUser != null ? currentUser.getUsername() : "未知",
                    sender != null ? sender.getUsername() : "未知",
                    receiverId);
                
                // 确定目标用户（与谁聊天）
                User targetUser = null;
                
                // 如果当前用户是接收者，那么发送者就是目标用户
                if (currentUser != null && receiverId != null && 
                    receiverId.equals(currentUser.getUserId())) {
                    targetUser = sender;
                    logger.debug("当前用户是接收者，发送者是目标用户: {}", 
                        targetUser != null ? targetUser.getUsername() : "未知");
                } 
                // 如果当前用户是发送者，那么接收者就是目标用户
                else if (currentUser != null && sender != null && 
                         currentUser.getUserId().equals(sender.getUserId())) {
                    // 这里我们需要找到接收者的User对象
                    // 由于接收者可能不在当前用户列表中，所以这里可能为空
                    // 在实际应用中，可能需要从服务器获取用户信息
                    for (int i = 0; i < userList.getModel().getSize(); i++) {
                        User user = userList.getModel().getElementAt(i);
                        if (user.getUserId().equals(receiverId)) {
                            targetUser = user;
                            break;
                        }
                    }
                    logger.debug("当前用户是发送者，接收者是目标用户: {}", 
                        targetUser != null ? targetUser.getUsername() : "未知ID:" + receiverId);
                }
                
                // 查找或创建聊天面板
                if (targetUser != null) {
                    // 查找现有面板
                    for (int i = 0; i < chatPanels.size(); i++) {
                        MessagePanel panel = chatPanels.get(i);
                        User panelTargetUser = panel.getTargetUser();
                        
                        if (panelTargetUser != null && panelTargetUser.getUserId() != null && 
                            panelTargetUser.getUserId().equals(targetUser.getUserId())) {
                            // 找到对应的聊天面板
                            logger.debug("找到聊天面板 [{}]，添加消息", i);
                            panel.appendMessage(message);
                            handled = true;
                            
                            // 如果不是当前选中的选项卡，标记为有新消息
                            if (i != tabbedPane.getSelectedIndex()) {
                                tabbedPane.setTitleAt(i, "* " + panelTargetUser.getUsername());
                            }
                            
                            break;
                        }
                    }
                    
                    // 没有找到面板，创建新的
                    if (!handled) {
                        logger.debug("未找到聊天面板，创建新面板");
                        openChatWithUser(targetUser);
                        
                        if (!chatPanels.isEmpty()) {
                            // 添加消息到新打开的面板
                            logger.debug("添加消息到新创建的聊天面板");
                            chatPanels.get(chatPanels.size() - 1).appendMessage(message);
                            handled = true;
                        } else {
                            logger.error("创建聊天面板失败，无法显示消息");
                        }
                    }
                } else {
                    logger.warn("无法确定目标用户，消息将显示在系统消息面板");
                    // 如果无法确定目标用户，显示在系统消息面板
                    if (!chatPanels.isEmpty()) {
                        chatPanels.get(0).appendMessage(message);
                        handled = true;
                    }
                }
                
                if (!handled) {
                    logger.error("消息未被处理: {}", message.getContent());
                }
            }
        });
    }
    
    /**
     * 用户列表单元格渲染器
     */
    private class UserListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout(10, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            if (isSelected) {
                panel.setBackground(new Color(240, 248, 255)); // 浅蓝色背景
                panel.setBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(52, 152, 219)));
            } else {
                panel.setBackground(Color.WHITE);
            }
            
            if (value instanceof User) {
                User user = (User) value;
                
                // 创建用户头像
                JLabel avatarLabel = new JLabel();
                avatarLabel.setIcon(createDefaultAvatar(user.getUsername(), 30));
                
                // 用户名和状态
                JPanel userInfoPanel = new JPanel(new BorderLayout());
                userInfoPanel.setOpaque(false);
                
                JLabel nameLabel = new JLabel(user.getUsername());
                nameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
                
                JLabel statusLabel = new JLabel("在线");
                statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
                statusLabel.setForeground(new Color(46, 204, 113)); // 绿色
                
                userInfoPanel.add(nameLabel, BorderLayout.NORTH);
                userInfoPanel.add(statusLabel, BorderLayout.SOUTH);
                
                // 添加到面板
                panel.add(avatarLabel, BorderLayout.WEST);
                panel.add(userInfoPanel, BorderLayout.CENTER);
                
                // 添加一个双击提示标签
                JLabel tipLabel = new JLabel("双击开始聊天");
                tipLabel.setFont(new Font("Microsoft YaHei", Font.ITALIC, 10));
                tipLabel.setForeground(Color.GRAY);
                tipLabel.setHorizontalAlignment(JLabel.RIGHT);
                
                panel.add(tipLabel, BorderLayout.EAST);
            }
            
            return panel;
        }
    }
    
    /**
     * 聊天面板
     */
    private static class ChatPanel extends JPanel implements MessagePanel {
        /**
         * 目标用户
         */
        private final User targetUser;
        
        /**
         * 聊天记录面板
         */
        private final JPanel chatArea;
        
        /**
         * 聊天记录滚动窗格
         */
        private final JScrollPane chatScrollPane;
        
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
            
            // 聊天记录区域 - 改为垂直BoxLayout的JPanel
            chatArea = new JPanel();
            chatArea.setLayout(new BoxLayout(chatArea, BoxLayout.Y_AXIS));
            chatArea.setBackground(Color.WHITE);
            
            chatScrollPane = new JScrollPane(chatArea);
            chatScrollPane.setPreferredSize(new Dimension(400, 300));
            chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
            
            // 输入区域
            inputArea = new JTextArea();
            inputArea.setLineWrap(true);
            inputArea.setWrapStyleWord(true);
            JScrollPane inputScrollPane = new JScrollPane(inputArea);
            inputScrollPane.setPreferredSize(new Dimension(400, 100));
            
            // 发送按钮
            JButton sendButton = new JButton("发送");
            sendButton.addActionListener(e -> sendMessage());
            
            // 文件发送按钮
            JButton fileButton = new JButton("发送文件");
            fileButton.addActionListener(e -> sendFile());
            
            // 输入面板
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.add(inputScrollPane, BorderLayout.CENTER);
            
            // 按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(fileButton); // 添加文件按钮
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
            
            // 初始欢迎消息
            try {
                if (targetUser == null) {
                    // 系统消息面板
                    inputArea.setEnabled(false);
                    sendButton.setEnabled(false);
                    fileButton.setEnabled(false); // 禁用文件按钮
                    
                    // 添加系统面板说明
                    addSystemMessage("这是系统消息面板，将显示系统通知和公告。");
                } else {
                    // 添加用户聊天欢迎消息
                    addSystemMessage("已开始与 " + targetUser.getUsername() + " 的聊天。");
                }
            } catch (Exception e) {
                System.out.println("设置初始消息失败: " + e.getMessage());
            }
        }
        
        /**
         * 添加系统消息
         * 
         * @param text 系统消息文本
         */
        private void addSystemMessage(String text) {
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            messagePanel.setBackground(Color.WHITE);
            
            JLabel messageLabel = new JLabel(text);
            messageLabel.setForeground(Color.GRAY);
            messageLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            
            messagePanel.add(messageLabel);
            chatArea.add(messagePanel);
            
            updateUI();
            scrollToBottom();
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
            
            if (client == null || client.getCurrentUser() == null) {
                JOptionPane.showMessageDialog(this, "无法发送消息：客户端未连接或未登录", "发送失败", JOptionPane.ERROR_MESSAGE);
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
            boolean sent = client.sendMessage(message);
            
            // 添加日志确认消息发送状态
            if (sent) {
                // 清空输入框
                inputArea.setText("");
                
                // 不再在本地直接添加消息，等待服务器回传
                // 这样可以避免消息重复显示
                System.out.println("消息已发送，等待服务器确认: " + message.getContent());
            } else {
                JOptionPane.showMessageDialog(this, "发送消息失败，请检查网络连接", "发送失败", JOptionPane.ERROR_MESSAGE);
                System.out.println("消息发送失败: " + message.getContent());
            }
        }
        
        /**
         * 发送文件
         */
        private void sendFile() {
            // 检查目标用户是否为空
            if (targetUser == null) {
                JOptionPane.showMessageDialog(this, "无法发送文件：目标用户不存在", "发送失败", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (client == null || client.getCurrentUser() == null) {
                JOptionPane.showMessageDialog(this, "无法发送文件：客户端未连接或未登录", "发送失败", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 打开文件选择器
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择要发送的文件");
            
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    // 获取选择的文件
                    File file = fileChooser.getSelectedFile();
                    
                    // 检查文件大小限制 (10MB)
                    if (file.length() > 10 * 1024 * 1024) {
                        JOptionPane.showMessageDialog(this, 
                            "文件过大，请选择小于10MB的文件", 
                            "文件过大", 
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    // 读取文件数据
                    byte[] fileData = readFileData(file);
                    
                    // 根据文件类型创建不同的消息
                    Message message;
                    String fileName = file.getName().toLowerCase();
                    // 私聊永远是false
                    boolean isGroupMessage = false;
                    
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                        fileName.endsWith(".png") || fileName.endsWith(".gif") || 
                        fileName.endsWith(".bmp")) {
                        // 图片消息
                        message = Message.createImageMessage(
                                client.getCurrentUser(),
                                targetUser.getUserId(),
                                file.getName(),
                                fileData,
                                isGroupMessage
                        );
                        addSystemMessage("正在发送图片：" + file.getName() + " (" + formatFileSize(file.length()) + ")");
                    } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") || 
                               fileName.endsWith(".mov") || fileName.endsWith(".wmv") || 
                               fileName.endsWith(".flv") || fileName.endsWith(".mkv")) {
                        // 视频消息
                        message = Message.createVideoMessage(
                                client.getCurrentUser(),
                                targetUser.getUserId(),
                                file.getName(),
                                fileData,
                                isGroupMessage
                        );
                        addSystemMessage("正在发送视频：" + file.getName() + " (" + formatFileSize(file.length()) + ")");
                    } else {
                        // 普通文件消息
                        message = Message.createFileMessage(
                                client.getCurrentUser(),
                                targetUser.getUserId(),
                                file.getName(),
                                fileData,
                                isGroupMessage
                        );
                        addSystemMessage("正在发送文件：" + file.getName() + " (" + formatFileSize(file.length()) + ")");
                    }
                    
                    // 发送消息
                    boolean sent = client.sendMessage(message);
                    
                    if (sent) {
                        addSystemMessage("文件发送成功：" + file.getName());
                    } else {
                        addSystemMessage("文件发送失败：" + file.getName());
                        JOptionPane.showMessageDialog(this, 
                            "发送文件失败，请检查网络连接", 
                            "发送失败", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, 
                        "读取文件失败：" + e.getMessage(), 
                        "读取失败", 
                        JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
        
        /**
         * 读取文件数据
         */
        private byte[] readFileData(File file) throws IOException {
            try (FileInputStream fis = new FileInputStream(file);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                
                return baos.toByteArray();
            }
        }
        
        /**
         * 格式化文件大小
         */
        private String formatFileSize(long size) {
            final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
            int unitIndex = 0;
            double fileSize = size;
            
            while (fileSize > 1024 && unitIndex < units.length - 1) {
                fileSize /= 1024;
                unitIndex++;
            }
            
            return String.format("%.2f %s", fileSize, units[unitIndex]);
        }
        
        /**
         * 添加消息到聊天区域
         */
        @Override
        public void appendMessage(Message message) {
            if (message == null) {
                System.out.println("警告：尝试添加空消息");
                return;
            }
            
            SwingUtilities.invokeLater(() -> {
                String time = dateFormat.format(message.getTimestamp() != null ? message.getTimestamp() : new Date());
                String sender = message.getSender() != null ? message.getSender().getUsername() : "系统";
                String content = message.getContent() != null ? message.getContent() : "";
                
                try {
                    if (message.getType() == MessageType.SYSTEM) {
                        // 系统消息居中显示
                        addSystemMessage(content);
                    } else if (message.getType() == MessageType.FILE) {
                        // 文件消息特殊处理
                        handleFileMessage(message, time, sender);
                    } else if (message.getType() == MessageType.IMAGE) {
                        // 图片消息直接显示
                        handleImageMessage(message, time, sender);
                    } else if (message.getType() == MessageType.VIDEO) {
                        // 视频消息处理
                        handleVideoMessage(message, time, sender);
                    } else {
                        // 判断是否是自己发送的消息
                        boolean isSelfMessage = message.getSender() != null && client.getCurrentUser() != null && 
                                               message.getSender().getUserId().equals(client.getCurrentUser().getUserId());
                        
                        // 创建消息面板
                        JPanel messagePanel = new JPanel();
                        messagePanel.setLayout(new BorderLayout());
                        messagePanel.setBackground(Color.WHITE);
                        
                        // 设置对齐方式
                        JPanel alignPanel = new JPanel();
                        alignPanel.setLayout(new FlowLayout(isSelfMessage ? FlowLayout.RIGHT : FlowLayout.LEFT));
                        alignPanel.setBackground(Color.WHITE);
                        
                        // 创建气泡面板
                        JPanel bubblePanel = new JPanel();
                        bubblePanel.setLayout(new BorderLayout());
                        bubblePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                        bubblePanel.setBackground(isSelfMessage ? new Color(144, 238, 144) : new Color(173, 216, 230));
                        
                        // 消息内容
                        JTextArea contentArea = new JTextArea(content);
                        contentArea.setEditable(false);
                        contentArea.setLineWrap(true);
                        contentArea.setWrapStyleWord(true);
                        contentArea.setBackground(isSelfMessage ? new Color(144, 238, 144) : new Color(173, 216, 230));
                        contentArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                        contentArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
                        
                        // 消息头：发送者和时间
                        JPanel headerPanel = new JPanel(new BorderLayout());
                        headerPanel.setOpaque(false);
                        
                        JLabel senderLabel = new JLabel(isSelfMessage ? "我" : sender);
                        senderLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
                        
                        JLabel timeLabel = new JLabel(time);
                        timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
                        timeLabel.setForeground(Color.GRAY);
                        
                        headerPanel.add(senderLabel, BorderLayout.WEST);
                        headerPanel.add(timeLabel, BorderLayout.EAST);
                        
                        // 组装气泡
                        bubblePanel.add(headerPanel, BorderLayout.NORTH);
                        bubblePanel.add(contentArea, BorderLayout.CENTER);
                        
                        // 限制气泡最大宽度
                        int maxWidth = 300;
                        Dimension prefSize = bubblePanel.getPreferredSize();
                        if (prefSize.width > maxWidth) {
                            prefSize.width = maxWidth;
                            bubblePanel.setPreferredSize(prefSize);
                        }
                        
                        alignPanel.add(bubblePanel);
                        messagePanel.add(alignPanel, BorderLayout.CENTER);
                        
                        // 添加到聊天区域
                        chatArea.add(messagePanel);
                        
                        System.out.println("添加了新消息气泡：" + content);
                    }
                    
                    // 更新UI并滚动到底部
                    chatArea.revalidate();
                    chatArea.repaint();
                    scrollToBottom();
                    
                } catch (Exception e) {
                    System.out.println("添加消息到聊天区域时出错：" + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
        
        /**
         * 处理文件消息
         */
        private void handleFileMessage(Message message, String time, String sender) {
            boolean isSelfMessage = message.getSender() != null && client.getCurrentUser() != null && 
                                  message.getSender().getUserId().equals(client.getCurrentUser().getUserId());
            
            // 创建消息面板
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BorderLayout());
            messagePanel.setBackground(Color.WHITE);
            
            // 设置对齐方式
            JPanel alignPanel = new JPanel();
            alignPanel.setLayout(new FlowLayout(isSelfMessage ? FlowLayout.RIGHT : FlowLayout.LEFT));
            alignPanel.setBackground(Color.WHITE);
            
            // 创建文件面板
            JPanel filePanel = new JPanel();
            filePanel.setLayout(new BorderLayout(5, 5));
            filePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10),
                BorderFactory.createLineBorder(new Color(200, 200, 200))
            ));
            filePanel.setBackground(Color.WHITE);
            
            // 文件图标
            JLabel fileIcon = new JLabel();
            fileIcon.setIcon(UIManager.getIcon("FileView.fileIcon"));
            if (fileIcon.getIcon() == null) {
                // 如果系统图标不可用，显示文本代替
                fileIcon.setText("📄");
                fileIcon.setFont(new Font("Dialog", Font.PLAIN, 24));
            }
            fileIcon.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
            
            // 文件信息面板
            JPanel fileInfoPanel = new JPanel(new BorderLayout());
            fileInfoPanel.setOpaque(false);
            
            // 文件名称和大小
            JLabel fileNameLabel = new JLabel(message.getFileName());
            fileNameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            
            JLabel fileSizeLabel = new JLabel(formatFileSize(message.getFileSize()));
            fileSizeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            fileSizeLabel.setForeground(Color.GRAY);
            
            fileInfoPanel.add(fileNameLabel, BorderLayout.NORTH);
            fileInfoPanel.add(fileSizeLabel, BorderLayout.CENTER);
            
            // 下载按钮 (如果不是自己发送的文件)
            if (!isSelfMessage) {
                JButton downloadButton = new JButton("下载");
                downloadButton.addActionListener(e -> saveReceivedFile(message));
                fileInfoPanel.add(downloadButton, BorderLayout.SOUTH);
            }
            
            // 发送者信息
            JPanel senderPanel = new JPanel(new BorderLayout());
            senderPanel.setOpaque(false);
            
            JLabel senderLabel = new JLabel(isSelfMessage ? "我" : sender);
            senderLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            
            JLabel timeLabel = new JLabel(time);
            timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            timeLabel.setForeground(Color.GRAY);
            
            senderPanel.add(senderLabel, BorderLayout.WEST);
            senderPanel.add(timeLabel, BorderLayout.EAST);
            
            // 组装文件面板
            filePanel.add(senderPanel, BorderLayout.NORTH);
            filePanel.add(fileIcon, BorderLayout.WEST);
            filePanel.add(fileInfoPanel, BorderLayout.CENTER);
            
            // 添加到对齐面板
            alignPanel.add(filePanel);
            messagePanel.add(alignPanel, BorderLayout.CENTER);
            
            // 添加到聊天区域
            chatArea.add(messagePanel);
            
            System.out.println("添加了文件消息：" + message.getFileName());
        }
        
        /**
         * 保存接收到的文件
         */
        private void saveReceivedFile(Message message) {
            if (message.getFileData() == null || message.getFileData().length == 0) {
                JOptionPane.showMessageDialog(this, 
                    "文件数据为空，无法保存", 
                    "保存失败", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 打开文件保存对话框
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("保存文件");
            fileChooser.setSelectedFile(new File(message.getFileName()));
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = fileChooser.getSelectedFile();
                    
                    // 写入文件数据
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(message.getFileData());
                    }
                    
                    addSystemMessage("文件保存成功：" + file.getName());
                    
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, 
                        "保存文件失败：" + e.getMessage(), 
                        "保存失败", 
                        JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
        
        /**
         * 处理图片消息
         */
        private void handleImageMessage(Message message, String time, String sender) {
            boolean isSelfMessage = message.getSender() != null && client.getCurrentUser() != null && 
                                  message.getSender().getUserId().equals(client.getCurrentUser().getUserId());
            
            // 创建消息面板
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BorderLayout());
            messagePanel.setBackground(Color.WHITE);
            
            // 设置对齐方式
            JPanel alignPanel = new JPanel();
            alignPanel.setLayout(new FlowLayout(isSelfMessage ? FlowLayout.RIGHT : FlowLayout.LEFT));
            alignPanel.setBackground(Color.WHITE);
            
            // 创建图片面板
            JPanel imagePanel = new JPanel();
            imagePanel.setLayout(new BorderLayout(5, 5));
            imagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10),
                BorderFactory.createLineBorder(new Color(200, 200, 200))
            ));
            imagePanel.setBackground(Color.WHITE);
            
            // 发送者信息
            JPanel senderPanel = new JPanel(new BorderLayout());
            senderPanel.setOpaque(false);
            
            JLabel senderLabel = new JLabel(isSelfMessage ? "我" : sender);
            senderLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            
            JLabel timeLabel = new JLabel(time);
            timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            timeLabel.setForeground(Color.GRAY);
            
            senderPanel.add(senderLabel, BorderLayout.WEST);
            senderPanel.add(timeLabel, BorderLayout.EAST);
            
            // 创建图片标签
            try {
                ImageIcon originalIcon = new ImageIcon(message.getFileData());
                // 限制图片最大尺寸为 300x300，保持宽高比
                int maxWidth = 300;
                int maxHeight = 300;
                int width = originalIcon.getIconWidth();
                int height = originalIcon.getIconHeight();
                
                if (width > maxWidth || height > maxHeight) {
                    double ratio = Math.min((double)maxWidth / width, (double)maxHeight / height);
                    width = (int)(width * ratio);
                    height = (int)(height * ratio);
                }
                
                Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                
                JLabel imageLabel = new JLabel(scaledIcon);
                imageLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                
                JPanel infoPanel = new JPanel(new BorderLayout());
                infoPanel.setOpaque(false);
                
                JLabel sizeLabel = new JLabel(formatFileSize(message.getFileSize()));
                sizeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
                sizeLabel.setForeground(Color.GRAY);
                infoPanel.add(sizeLabel, BorderLayout.NORTH);
                
                // 添加保存按钮
                if (!isSelfMessage) {
                    JButton saveButton = new JButton("保存图片");
                    saveButton.addActionListener(e -> saveReceivedFile(message));
                    infoPanel.add(saveButton, BorderLayout.SOUTH);
                }
                
                // 组装图片面板
                imagePanel.add(senderPanel, BorderLayout.NORTH);
                imagePanel.add(imageLabel, BorderLayout.CENTER);
                imagePanel.add(infoPanel, BorderLayout.SOUTH);
                
            } catch (Exception e) {
                // 如果图片无法显示，显示错误信息
                JLabel errorLabel = new JLabel("图片加载失败");
                errorLabel.setForeground(Color.RED);
                imagePanel.add(senderPanel, BorderLayout.NORTH);
                imagePanel.add(errorLabel, BorderLayout.CENTER);
                
                System.out.println("图片加载失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 添加到对齐面板
            alignPanel.add(imagePanel);
            messagePanel.add(alignPanel, BorderLayout.CENTER);
            
            // 添加到聊天区域
            chatArea.add(messagePanel);
            
            System.out.println("添加了图片消息：" + message.getFileName());
        }
        
        /**
         * 处理视频消息
         */
        private void handleVideoMessage(Message message, String time, String sender) {
            boolean isSelfMessage = message.getSender() != null && client.getCurrentUser() != null && 
                                  message.getSender().getUserId().equals(client.getCurrentUser().getUserId());
            
            // 创建消息面板
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BorderLayout());
            messagePanel.setBackground(Color.WHITE);
            
            // 设置对齐方式
            JPanel alignPanel = new JPanel();
            alignPanel.setLayout(new FlowLayout(isSelfMessage ? FlowLayout.RIGHT : FlowLayout.LEFT));
            alignPanel.setBackground(Color.WHITE);
            
            // 创建视频面板
            JPanel videoPanel = new JPanel();
            videoPanel.setLayout(new BorderLayout(5, 5));
            videoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10),
                BorderFactory.createLineBorder(new Color(200, 200, 200))
            ));
            videoPanel.setBackground(Color.WHITE);
            
            // 视频图标
            JLabel videoIcon = new JLabel();
            // 使用文本代替图标
            videoIcon.setText("▶");
            videoIcon.setFont(new Font("Dialog", Font.BOLD, 24));
            videoIcon.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
            
            // 视频信息面板
            JPanel videoInfoPanel = new JPanel(new BorderLayout());
            videoInfoPanel.setOpaque(false);
            
            // 视频名称和大小
            JLabel videoNameLabel = new JLabel(message.getFileName());
            videoNameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            
            JLabel videoSizeLabel = new JLabel(formatFileSize(message.getFileSize()));
            videoSizeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            videoSizeLabel.setForeground(Color.GRAY);
            
            JButton playButton = new JButton("播放");
            playButton.addActionListener(e -> playVideo(message));
            
            videoInfoPanel.add(videoNameLabel, BorderLayout.NORTH);
            videoInfoPanel.add(videoSizeLabel, BorderLayout.CENTER);
            videoInfoPanel.add(playButton, BorderLayout.SOUTH);
            
            // 发送者信息
            JPanel senderPanel = new JPanel(new BorderLayout());
            senderPanel.setOpaque(false);
            
            JLabel senderLabel = new JLabel(isSelfMessage ? "我" : sender);
            senderLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            
            JLabel timeLabel = new JLabel(time);
            timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            timeLabel.setForeground(Color.GRAY);
            
            senderPanel.add(senderLabel, BorderLayout.WEST);
            senderPanel.add(timeLabel, BorderLayout.EAST);
            
            // 组装视频面板
            videoPanel.add(senderPanel, BorderLayout.NORTH);
            videoPanel.add(videoIcon, BorderLayout.WEST);
            videoPanel.add(videoInfoPanel, BorderLayout.CENTER);
            
            // 添加到对齐面板
            alignPanel.add(videoPanel);
            messagePanel.add(alignPanel, BorderLayout.CENTER);
            
            // 添加到聊天区域
            chatArea.add(messagePanel);
            
            System.out.println("添加了视频消息：" + message.getFileName());
        }
        
        /**
         * 播放视频
         */
        private void playVideo(Message message) {
            if (message.getFileData() == null || message.getFileData().length == 0) {
                JOptionPane.showMessageDialog(this, 
                    "视频数据为空，无法播放", 
                    "播放失败", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                // 创建临时文件
                File tempFile = File.createTempFile("video_", "_" + message.getFileName());
                tempFile.deleteOnExit();
                
                // 写入视频数据
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(message.getFileData());
                }
                
                // 使用系统默认程序打开视频文件
                Desktop.getDesktop().open(tempFile);
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "播放视频失败：" + e.getMessage(), 
                    "播放失败", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
        
        /**
         * 滚动到底部
         */
        private void scrollToBottom() {
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        }
        
        /**
         * 获取目标用户
         * 
         * @return 目标用户
         */
        public User getTargetUser() {
            return targetUser;
        }
        
        /**
         * 获取群组，如果是私聊则返回null
         */
        public com.chatroom.common.model.ChatGroup getChatGroup() {
            return null;
        }
    }
    
    /**
     * 群聊面板
     */
    private static class GroupChatPanel extends JPanel implements MessagePanel {
        /**
         * 群组对象
         */
        private final com.chatroom.common.model.ChatGroup chatGroup;
        
        /**
         * 聊天记录面板
         */
        private final JPanel chatArea;
        
        /**
         * 聊天记录滚动窗格
         */
        private final JScrollPane chatScrollPane;
        
        /**
         * 消息输入框
         */
        private final JTextArea inputArea;
        
        /**
         * 客户端引用
         */
        private final ChatClient client;
        
        /**
         * 用户列表
         */
        private final JList<User> userList;
        
        /**
         * 日期格式化
         */
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        
        /**
         * 构造方法
         * 
         * @param title 面板标题
         * @param chatGroup 群组对象
         * @param client 客户端引用
         * @param userList 用户列表
         */
        public GroupChatPanel(String title, com.chatroom.common.model.ChatGroup chatGroup, ChatClient client, JList<User> userList) {
            this.chatGroup = chatGroup;
            this.client = client;
            this.userList = userList;
            
            // 检查客户端是否有效
            if (client == null) {
                throw new IllegalArgumentException("客户端引用不能为空");
            }
            
            setLayout(new BorderLayout());
            
            // 聊天记录区域 - 使用BoxLayout
            chatArea = new JPanel();
            chatArea.setLayout(new BoxLayout(chatArea, BoxLayout.Y_AXIS));
            chatArea.setBackground(Color.WHITE);
            
            chatScrollPane = new JScrollPane(chatArea);
            chatScrollPane.setPreferredSize(new Dimension(400, 300));
            chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
            
            // 输入区域
            inputArea = new JTextArea();
            inputArea.setLineWrap(true);
            inputArea.setWrapStyleWord(true);
            JScrollPane inputScrollPane = new JScrollPane(inputArea);
            inputScrollPane.setPreferredSize(new Dimension(400, 100));
            
            // 发送按钮
            JButton sendButton = new JButton("发送");
            sendButton.addActionListener(e -> sendMessage());
            
            // 文件发送按钮
            JButton fileButton = new JButton("发送文件");
            fileButton.addActionListener(e -> sendFile());
            
            // 成员列表按钮
            JButton membersButton = new JButton("成员列表");
            membersButton.addActionListener(e -> showMemberList());
            
            // 输入面板
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.add(inputScrollPane, BorderLayout.CENTER);
            
            // 按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(membersButton);
            buttonPanel.add(fileButton);
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
            
            // 添加欢迎消息
            addSystemMessage("欢迎加入" + title + "！");
            
            // 显示在线成员提示
            if (chatGroup.getMemberIds().size() > 0) {
                addSystemMessage("当前在线成员: " + chatGroup.getMemberIds().size() + " 人");
            }
        }
        
        /**
         * 获取群组对象
         */
        @Override
        public com.chatroom.common.model.ChatGroup getChatGroup() {
            return chatGroup;
        }
        
        /**
         * 获取目标用户，群聊返回null
         */
        @Override
        public User getTargetUser() {
            return null;
        }
        
        /**
         * 添加系统消息
         */
        private void addSystemMessage(String text) {
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            messagePanel.setBackground(Color.WHITE);
            
            JLabel messageLabel = new JLabel(text);
            messageLabel.setForeground(Color.GRAY);
            messageLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            
            messagePanel.add(messageLabel);
            chatArea.add(messagePanel);
            
            chatArea.revalidate();
            chatArea.repaint();
            scrollToBottom();
        }
        
        /**
         * 发送消息
         */
        private void sendMessage() {
            String content = inputArea.getText().trim();
            if (content.isEmpty()) return;
            
            // 检查群组是否有效
            if (chatGroup == null) {
                JOptionPane.showMessageDialog(this, "无法发送消息：群组不存在", "发送失败", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (client == null || client.getCurrentUser() == null) {
                JOptionPane.showMessageDialog(this, "无法发送消息：客户端未连接或未登录", "发送失败", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 创建群聊消息对象
            Message message = Message.createTextMessage(
                    client.getCurrentUser(),
                    chatGroup.getGroupId(),
                    content,
                    true  // 设置为群聊消息
            );
            
            // 发送消息
            boolean sent = client.sendMessage(message);
            
            // 添加日志确认消息发送状态
            if (sent) {
                // 清空输入框
                inputArea.setText("");
                
                // 不在本地添加消息，等待服务器回传
                System.out.println("群聊消息已发送: " + message.getContent());
            } else {
                JOptionPane.showMessageDialog(this, "发送消息失败，请检查网络连接", "发送失败", JOptionPane.ERROR_MESSAGE);
                System.out.println("群聊消息发送失败: " + message.getContent());
            }
        }
        
        /**
         * 发送文件
         */
        private void sendFile() {
            // 检查群组是否有效
            if (chatGroup == null) {
                JOptionPane.showMessageDialog(this, "无法发送文件：群组不存在", "发送失败", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (client == null || client.getCurrentUser() == null) {
                JOptionPane.showMessageDialog(this, "无法发送文件：客户端未连接或未登录", "发送失败", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 打开文件选择器
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择要发送的文件");
            
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    // 获取选择的文件
                    File file = fileChooser.getSelectedFile();
                    
                    // 检查文件大小限制 (10MB)
                    if (file.length() > 10 * 1024 * 1024) {
                        JOptionPane.showMessageDialog(this, 
                            "文件过大，请选择小于10MB的文件", 
                            "文件过大", 
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    // 读取文件数据
                    byte[] fileData = readFileData(file);
                    
                    // 根据文件类型创建不同的消息
                    Message message;
                    String fileName = file.getName().toLowerCase();
                    // 群聊永远是true
                    boolean isGroupMessage = true;
                    
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                        fileName.endsWith(".png") || fileName.endsWith(".gif") || 
                        fileName.endsWith(".bmp")) {
                        // 图片消息
                        message = Message.createImageMessage(
                                client.getCurrentUser(),
                                chatGroup.getGroupId(),
                                file.getName(),
                                fileData,
                                isGroupMessage
                        );
                        addSystemMessage("正在发送图片：" + file.getName() + " (" + formatFileSize(file.length()) + ")");
                    } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") || 
                               fileName.endsWith(".mov") || fileName.endsWith(".wmv") || 
                               fileName.endsWith(".flv") || fileName.endsWith(".mkv")) {
                        // 视频消息
                        message = Message.createVideoMessage(
                                client.getCurrentUser(),
                                chatGroup.getGroupId(),
                                file.getName(),
                                fileData,
                                isGroupMessage
                        );
                        addSystemMessage("正在发送视频：" + file.getName() + " (" + formatFileSize(file.length()) + ")");
                    } else {
                        // 普通文件消息
                        message = Message.createFileMessage(
                                client.getCurrentUser(),
                                chatGroup.getGroupId(),
                                file.getName(),
                                fileData,
                                isGroupMessage
                        );
                        addSystemMessage("正在发送文件：" + file.getName() + " (" + formatFileSize(file.length()) + ")");
                    }
                    
                    // 发送消息
                    boolean sent = client.sendMessage(message);
                    
                    if (sent) {
                        addSystemMessage("文件发送成功：" + file.getName());
                    } else {
                        addSystemMessage("文件发送失败：" + file.getName());
                        JOptionPane.showMessageDialog(this, 
                            "发送文件失败，请检查网络连接", 
                            "发送失败", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, 
                        "读取文件失败：" + e.getMessage(), 
                        "读取失败", 
                        JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
        
        /**
         * 读取文件数据
         */
        private byte[] readFileData(File file) throws IOException {
            try (FileInputStream fis = new FileInputStream(file);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                
                return baos.toByteArray();
            }
        }
        
        /**
         * 格式化文件大小
         */
        private String formatFileSize(long size) {
            final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
            int unitIndex = 0;
            double fileSize = size;
            
            while (fileSize > 1024 && unitIndex < units.length - 1) {
                fileSize /= 1024;
                unitIndex++;
            }
            
            return String.format("%.2f %s", fileSize, units[unitIndex]);
        }
        
        /**
         * 显示成员列表
         */
        private void showMemberList() {
            if (chatGroup == null || chatGroup.getMemberIds() == null || chatGroup.getMemberIds().isEmpty()) {
                JOptionPane.showMessageDialog(this, "暂无成员信息", "成员列表", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            StringBuilder memberList = new StringBuilder("<html><body>");
            memberList.append("<h3>").append(chatGroup.getGroupName()).append(" 成员列表</h3>");
            memberList.append("<ul>");
            
            for (String memberId : chatGroup.getMemberIds()) {
                String memberName = "未知用户";
                
                // 如果是当前用户
                if (client.getCurrentUser() != null && client.getCurrentUser().getUserId().equals(memberId)) {
                    memberName = client.getCurrentUser().getUsername() + " (我)";
                } else {
                    // 查找用户名
                    for (int i = 0; i < userList.getModel().getSize(); i++) {
                        User user = userList.getModel().getElementAt(i);
                        if (user.getUserId().equals(memberId)) {
                            memberName = user.getUsername();
                            break;
                        }
                    }
                }
                
                memberList.append("<li>").append(memberName).append("</li>");
            }
            
            memberList.append("</ul></body></html>");
            
            JOptionPane.showMessageDialog(
                    this,
                    new JLabel(memberList.toString()),
                    chatGroup.getGroupName() + " 成员列表",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
        
        /**
         * 滚动到底部
         */
        private void scrollToBottom() {
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        }
        
        /**
         * 添加消息到聊天区域
         */
        @Override
        public void appendMessage(Message message) {
            if (message == null) {
                System.out.println("警告：尝试添加空消息");
                return;
            }
            
            SwingUtilities.invokeLater(() -> {
                String time = dateFormat.format(message.getTimestamp() != null ? message.getTimestamp() : new Date());
                String sender = message.getSender() != null ? message.getSender().getUsername() : "系统";
                String content = message.getContent() != null ? message.getContent() : "";
                
                try {
                    if (message.getType() == MessageType.SYSTEM) {
                        // 系统消息居中显示
                        addSystemMessage(content);
                    } else if (message.getType() == MessageType.FILE) {
                        // 文件消息特殊处理
                        handleFileMessage(message, time, sender);
                    } else if (message.getType() == MessageType.IMAGE) {
                        // 图片消息直接显示
                        handleImageMessage(message, time, sender);
                    } else if (message.getType() == MessageType.VIDEO) {
                        // 视频消息处理
                        handleVideoMessage(message, time, sender);
                    } else {
                        // 判断是否是自己发送的消息
                        boolean isSelfMessage = message.getSender() != null && client.getCurrentUser() != null && 
                                               message.getSender().getUserId().equals(client.getCurrentUser().getUserId());
                        
                        // 创建消息面板
                        JPanel messagePanel = new JPanel();
                        messagePanel.setLayout(new BorderLayout());
                        messagePanel.setBackground(Color.WHITE);
                        
                        // 设置对齐方式
                        JPanel alignPanel = new JPanel();
                        alignPanel.setLayout(new FlowLayout(isSelfMessage ? FlowLayout.RIGHT : FlowLayout.LEFT));
                        alignPanel.setBackground(Color.WHITE);
                        
                        // 创建气泡面板
                        JPanel bubblePanel = new JPanel();
                        bubblePanel.setLayout(new BorderLayout());
                        bubblePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                        bubblePanel.setBackground(isSelfMessage ? new Color(144, 238, 144) : new Color(173, 216, 230));
                        
                        // 消息内容
                        JTextArea contentArea = new JTextArea(content);
                        contentArea.setEditable(false);
                        contentArea.setLineWrap(true);
                        contentArea.setWrapStyleWord(true);
                        contentArea.setBackground(isSelfMessage ? new Color(144, 238, 144) : new Color(173, 216, 230));
                        contentArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                        contentArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
                        
                        // 消息头：发送者和时间
                        JPanel headerPanel = new JPanel(new BorderLayout());
                        headerPanel.setOpaque(false);
                        
                        JLabel senderLabel = new JLabel(isSelfMessage ? "我" : sender);
                        senderLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
                        
                        JLabel timeLabel = new JLabel(time);
                        timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
                        timeLabel.setForeground(Color.GRAY);
                        
                        headerPanel.add(senderLabel, BorderLayout.WEST);
                        headerPanel.add(timeLabel, BorderLayout.EAST);
                        
                        // 组装气泡
                        bubblePanel.add(headerPanel, BorderLayout.NORTH);
                        bubblePanel.add(contentArea, BorderLayout.CENTER);
                        
                        // 限制气泡最大宽度
                        int maxWidth = 300;
                        Dimension prefSize = bubblePanel.getPreferredSize();
                        if (prefSize.width > maxWidth) {
                            prefSize.width = maxWidth;
                            bubblePanel.setPreferredSize(prefSize);
                        }
                        
                        alignPanel.add(bubblePanel);
                        messagePanel.add(alignPanel, BorderLayout.CENTER);
                        
                        // 添加到聊天区域
                        chatArea.add(messagePanel);
                        
                        System.out.println("群聊面板添加了新消息气泡：" + content);
                    }
                    
                    // 更新UI并滚动到底部
                    chatArea.revalidate();
                    chatArea.repaint();
                    scrollToBottom();
                    
                } catch (Exception e) {
                    System.out.println("添加消息到群聊区域时出错：" + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
        
        /**
         * 处理文件消息
         */
        private void handleFileMessage(Message message, String time, String sender) {
            boolean isSelfMessage = message.getSender() != null && client.getCurrentUser() != null && 
                                  message.getSender().getUserId().equals(client.getCurrentUser().getUserId());
            
            // 创建消息面板
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BorderLayout());
            messagePanel.setBackground(Color.WHITE);
            
            // 设置对齐方式
            JPanel alignPanel = new JPanel();
            alignPanel.setLayout(new FlowLayout(isSelfMessage ? FlowLayout.RIGHT : FlowLayout.LEFT));
            alignPanel.setBackground(Color.WHITE);
            
            // 创建文件面板
            JPanel filePanel = new JPanel();
            filePanel.setLayout(new BorderLayout(5, 5));
            filePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10),
                BorderFactory.createLineBorder(new Color(200, 200, 200))
            ));
            filePanel.setBackground(Color.WHITE);
            
            // 文件图标
            JLabel fileIcon = new JLabel();
            fileIcon.setIcon(UIManager.getIcon("FileView.fileIcon"));
            if (fileIcon.getIcon() == null) {
                // 如果系统图标不可用，显示文本代替
                fileIcon.setText("📄");
                fileIcon.setFont(new Font("Dialog", Font.PLAIN, 24));
            }
            fileIcon.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
            
            // 文件信息面板
            JPanel fileInfoPanel = new JPanel(new BorderLayout());
            fileInfoPanel.setOpaque(false);
            
            // 文件名称和大小
            JLabel fileNameLabel = new JLabel(message.getFileName());
            fileNameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            
            JLabel fileSizeLabel = new JLabel(formatFileSize(message.getFileSize()));
            fileSizeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            fileSizeLabel.setForeground(Color.GRAY);
            
            fileInfoPanel.add(fileNameLabel, BorderLayout.NORTH);
            fileInfoPanel.add(fileSizeLabel, BorderLayout.CENTER);
            
            // 下载按钮 (如果不是自己发送的文件)
            if (!isSelfMessage) {
                JButton downloadButton = new JButton("下载");
                downloadButton.addActionListener(e -> saveReceivedFile(message));
                fileInfoPanel.add(downloadButton, BorderLayout.SOUTH);
            }
            
            // 发送者信息
            JPanel senderPanel = new JPanel(new BorderLayout());
            senderPanel.setOpaque(false);
            
            JLabel senderLabel = new JLabel(isSelfMessage ? "我" : sender);
            senderLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            
            JLabel timeLabel = new JLabel(time);
            timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            timeLabel.setForeground(Color.GRAY);
            
            senderPanel.add(senderLabel, BorderLayout.WEST);
            senderPanel.add(timeLabel, BorderLayout.EAST);
            
            // 组装文件面板
            filePanel.add(senderPanel, BorderLayout.NORTH);
            filePanel.add(fileIcon, BorderLayout.WEST);
            filePanel.add(fileInfoPanel, BorderLayout.CENTER);
            
            // 添加到对齐面板
            alignPanel.add(filePanel);
            messagePanel.add(alignPanel, BorderLayout.CENTER);
            
            // 添加到聊天区域
            chatArea.add(messagePanel);
            
            System.out.println("添加了群聊文件消息：" + message.getFileName());
        }
        
        /**
         * 保存接收到的文件
         */
        private void saveReceivedFile(Message message) {
            if (message.getFileData() == null || message.getFileData().length == 0) {
                JOptionPane.showMessageDialog(this, 
                    "文件数据为空，无法保存", 
                    "保存失败", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 打开文件保存对话框
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("保存文件");
            fileChooser.setSelectedFile(new File(message.getFileName()));
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = fileChooser.getSelectedFile();
                    
                    // 写入文件数据
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(message.getFileData());
                    }
                    
                    addSystemMessage("文件保存成功：" + file.getName());
                    
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, 
                        "保存文件失败：" + e.getMessage(), 
                        "保存失败", 
                        JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
        
        /**
         * 处理图片消息
         */
        private void handleImageMessage(Message message, String time, String sender) {
            boolean isSelfMessage = message.getSender() != null && client.getCurrentUser() != null && 
                                  message.getSender().getUserId().equals(client.getCurrentUser().getUserId());
            
            // 创建消息面板
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BorderLayout());
            messagePanel.setBackground(Color.WHITE);
            
            // 设置对齐方式
            JPanel alignPanel = new JPanel();
            alignPanel.setLayout(new FlowLayout(isSelfMessage ? FlowLayout.RIGHT : FlowLayout.LEFT));
            alignPanel.setBackground(Color.WHITE);
            
            // 创建图片面板
            JPanel imagePanel = new JPanel();
            imagePanel.setLayout(new BorderLayout(5, 5));
            imagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10),
                BorderFactory.createLineBorder(new Color(200, 200, 200))
            ));
            imagePanel.setBackground(Color.WHITE);
            
            // 发送者信息
            JPanel senderPanel = new JPanel(new BorderLayout());
            senderPanel.setOpaque(false);
            
            JLabel senderLabel = new JLabel(isSelfMessage ? "我" : sender);
            senderLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            
            JLabel timeLabel = new JLabel(time);
            timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            timeLabel.setForeground(Color.GRAY);
            
            senderPanel.add(senderLabel, BorderLayout.WEST);
            senderPanel.add(timeLabel, BorderLayout.EAST);
            
            // 创建图片标签
            try {
                ImageIcon originalIcon = new ImageIcon(message.getFileData());
                // 限制图片最大尺寸为 300x300，保持宽高比
                int maxWidth = 300;
                int maxHeight = 300;
                int width = originalIcon.getIconWidth();
                int height = originalIcon.getIconHeight();
                
                if (width > maxWidth || height > maxHeight) {
                    double ratio = Math.min((double)maxWidth / width, (double)maxHeight / height);
                    width = (int)(width * ratio);
                    height = (int)(height * ratio);
                }
                
                Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                
                JLabel imageLabel = new JLabel(scaledIcon);
                imageLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                
                JPanel infoPanel = new JPanel(new BorderLayout());
                infoPanel.setOpaque(false);
                
                JLabel sizeLabel = new JLabel(formatFileSize(message.getFileSize()));
                sizeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
                sizeLabel.setForeground(Color.GRAY);
                infoPanel.add(sizeLabel, BorderLayout.NORTH);
                
                // 添加保存按钮
                if (!isSelfMessage) {
                    JButton saveButton = new JButton("保存图片");
                    saveButton.addActionListener(e -> saveReceivedFile(message));
                    infoPanel.add(saveButton, BorderLayout.SOUTH);
                }
                
                // 组装图片面板
                imagePanel.add(senderPanel, BorderLayout.NORTH);
                imagePanel.add(imageLabel, BorderLayout.CENTER);
                imagePanel.add(infoPanel, BorderLayout.SOUTH);
                
            } catch (Exception e) {
                // 如果图片无法显示，显示错误信息
                JLabel errorLabel = new JLabel("图片加载失败");
                errorLabel.setForeground(Color.RED);
                imagePanel.add(senderPanel, BorderLayout.NORTH);
                imagePanel.add(errorLabel, BorderLayout.CENTER);
                
                System.out.println("图片加载失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 添加到对齐面板
            alignPanel.add(imagePanel);
            messagePanel.add(alignPanel, BorderLayout.CENTER);
            
            // 添加到聊天区域
            chatArea.add(messagePanel);
            
            System.out.println("添加了图片消息：" + message.getFileName());
        }
        
        /**
         * 处理视频消息
         */
        private void handleVideoMessage(Message message, String time, String sender) {
            boolean isSelfMessage = message.getSender() != null && client.getCurrentUser() != null && 
                                  message.getSender().getUserId().equals(client.getCurrentUser().getUserId());
            
            // 创建消息面板
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BorderLayout());
            messagePanel.setBackground(Color.WHITE);
            
            // 设置对齐方式
            JPanel alignPanel = new JPanel();
            alignPanel.setLayout(new FlowLayout(isSelfMessage ? FlowLayout.RIGHT : FlowLayout.LEFT));
            alignPanel.setBackground(Color.WHITE);
            
            // 创建视频面板
            JPanel videoPanel = new JPanel();
            videoPanel.setLayout(new BorderLayout(5, 5));
            videoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10),
                BorderFactory.createLineBorder(new Color(200, 200, 200))
            ));
            videoPanel.setBackground(Color.WHITE);
            
            // 视频图标
            JLabel videoIcon = new JLabel();
            // 使用文本代替图标
            videoIcon.setText("▶");
            videoIcon.setFont(new Font("Dialog", Font.BOLD, 24));
            videoIcon.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
            
            // 视频信息面板
            JPanel videoInfoPanel = new JPanel(new BorderLayout());
            videoInfoPanel.setOpaque(false);
            
            // 视频名称和大小
            JLabel videoNameLabel = new JLabel(message.getFileName());
            videoNameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            
            JLabel videoSizeLabel = new JLabel(formatFileSize(message.getFileSize()));
            videoSizeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            videoSizeLabel.setForeground(Color.GRAY);
            
            JButton playButton = new JButton("播放");
            playButton.addActionListener(e -> playVideo(message));
            
            videoInfoPanel.add(videoNameLabel, BorderLayout.NORTH);
            videoInfoPanel.add(videoSizeLabel, BorderLayout.CENTER);
            videoInfoPanel.add(playButton, BorderLayout.SOUTH);
            
            // 发送者信息
            JPanel senderPanel = new JPanel(new BorderLayout());
            senderPanel.setOpaque(false);
            
            JLabel senderLabel = new JLabel(isSelfMessage ? "我" : sender);
            senderLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            
            JLabel timeLabel = new JLabel(time);
            timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            timeLabel.setForeground(Color.GRAY);
            
            senderPanel.add(senderLabel, BorderLayout.WEST);
            senderPanel.add(timeLabel, BorderLayout.EAST);
            
            // 组装视频面板
            videoPanel.add(senderPanel, BorderLayout.NORTH);
            videoPanel.add(videoIcon, BorderLayout.WEST);
            videoPanel.add(videoInfoPanel, BorderLayout.CENTER);
            
            // 添加到对齐面板
            alignPanel.add(videoPanel);
            messagePanel.add(alignPanel, BorderLayout.CENTER);
            
            // 添加到聊天区域
            chatArea.add(messagePanel);
            
            System.out.println("添加了视频消息：" + message.getFileName());
        }
        
        /**
         * 播放视频
         */
        private void playVideo(Message message) {
            if (message.getFileData() == null || message.getFileData().length == 0) {
                JOptionPane.showMessageDialog(this, 
                    "视频数据为空，无法播放", 
                    "播放失败", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                // 创建临时文件
                File tempFile = File.createTempFile("video_", "_" + message.getFileName());
                tempFile.deleteOnExit();
                
                // 写入视频数据
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(message.getFileData());
                }
                
                // 使用系统默认程序打开视频文件
                Desktop.getDesktop().open(tempFile);
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "播放视频失败：" + e.getMessage(), 
                    "播放失败", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 创建默认头像
     * 
     * @param text 头像文本
     * @param size 头像大小
     * @return 头像图标
     */
    private ImageIcon createDefaultAvatar(String text, int size) {
        // 创建一个圆形头像
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制圆形背景
        g2d.setColor(getColorFromString(text));
        g2d.fillOval(0, 0, size, size);
        
        // 绘制文本 (取首字母)
        String initial = text.substring(0, 1).toUpperCase();
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, size / 2));
        
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(initial);
        int textHeight = fm.getHeight();
        
        // 居中绘制文本
        g2d.drawString(initial, (size - textWidth) / 2, (size - textHeight) / 2 + fm.getAscent());
        
        g2d.dispose();
        
        return new ImageIcon(image);
    }
    
    /**
     * 从字符串生成固定颜色
     * 
     * @param text 文本
     * @return 颜色
     */
    private Color getColorFromString(String text) {
        // 使用文本的哈希值生成颜色
        int hash = text.hashCode();
        
        // 预定义的明亮颜色数组
        Color[] colors = {
            new Color(52, 152, 219),  // 蓝色
            new Color(231, 76, 60),   // 红色
            new Color(46, 204, 113),  // 绿色
            new Color(155, 89, 182),  // 紫色
            new Color(241, 196, 15),  // 黄色
            new Color(230, 126, 34),  // 橙色
            new Color(26, 188, 156),  // 青绿色
            new Color(149, 165, 166)  // 灰色
        };
        
        // 使用哈希值选择颜色
        return colors[Math.abs(hash % colors.length)];
    }
    
    /**
     * 消息面板接口，定义能处理消息的面板
     */
    private interface MessagePanel {
        /**
         * 添加消息到面板
         */
        void appendMessage(Message message);
        
        /**
         * 获取目标用户，如果是群聊则返回null
         */
        User getTargetUser();
        
        /**
         * 获取群组，如果是私聊则返回null
         */
        com.chatroom.common.model.ChatGroup getChatGroup();
    }
} 