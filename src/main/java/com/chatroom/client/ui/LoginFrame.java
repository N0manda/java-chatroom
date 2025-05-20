package com.chatroom.client.ui;

import com.chatroom.client.ChatClient;
import com.chatroom.common.network.ChatResponse;
import com.chatroom.common.network.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 登录界面
 */
public class LoginFrame extends JFrame implements com.chatroom.client.MessageHandler.ResponseListener {
    private static final Logger logger = LoggerFactory.getLogger(LoginFrame.class);
    
    // 配置文件路径
    private static final String CONFIG_PATH = "client_config.properties";
    private static final int MAX_HISTORY_SIZE = 5;
    
    /**
     * 客户端引用
     */
    private ChatClient client;
    
    /**
     * 用户名输入框
     */
    private JTextField usernameField;
    
    /**
     * 密码输入框
     */
    private JPasswordField passwordField;
    
    /**
     * 服务器地址和端口输入框
     */
    private JTextField serverAddressField;
    private JSpinner portSpinner;
    private JComboBox<ServerInfo> historyServersComboBox;
    
    /**
     * 登录按钮
     */
    private JButton loginButton;
    
    /**
     * 状态标签
     */
    private JLabel statusLabel;
    
    /**
     * 连接历史
     */
    private List<ServerInfo> connectionHistory;
    
    /**
     * 构造方法
     */
    public LoginFrame() {
        connectionHistory = loadConnectionHistory();
        initComponents();
        setupListeners();
        loadLastServerSettings();
    }
    
    /**
     * 构造方法 - 用于注销后重新登录
     * 
     * @param client 客户端引用
     */
    public LoginFrame(ChatClient client) {
        this.client = client;
        connectionHistory = loadConnectionHistory();
        initComponents();
        setupListeners();
        loadLastServerSettings();
        
        // 预填充服务器信息
        serverAddressField.setText(client.getServerHost());
        portSpinner.setValue(client.getServerPort());
    }
    
    /**
     * 初始化界面组件
     */
    private void initComponents() {
        setTitle("聊天室 - 登录");
        setSize(400, 400);  // 增加窗口高度，容纳密码输入框
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // 创建面板
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 标题标签
        JLabel titleLabel = new JLabel("网络聊天室");
        titleLabel.setFont(new Font("宋体", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(titleLabel, gbc);
        
        // 服务器地址标签和输入框
        JLabel serverAddressLabel = new JLabel("服务器地址:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(serverAddressLabel, gbc);
        
        serverAddressField = new JTextField("localhost", 15);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(serverAddressField, gbc);
        
        // 端口标签和输入框
        JLabel portLabel = new JLabel("端口:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(portLabel, gbc);
        
        SpinnerNumberModel portModel = new SpinnerNumberModel(9999, 1024, 65535, 1);
        portSpinner = new JSpinner(portModel);
        JComponent editor = portSpinner.getEditor();
        JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor)editor;
        spinnerEditor.getTextField().setColumns(5);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(portSpinner, gbc);
        
        // 连接历史
        JLabel historyLabel = new JLabel("连接历史:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(historyLabel, gbc);
        
        historyServersComboBox = new JComboBox<>();
        refreshHistoryComboBox();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(historyServersComboBox, gbc);
        
        // 用户名标签
        JLabel usernameLabel = new JLabel("用户名:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(usernameLabel, gbc);
        
        // 用户名输入框
        usernameField = new JTextField(15);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(usernameField, gbc);
        
        // 密码标签
        JLabel passwordLabel = new JLabel("密码:");
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(passwordLabel, gbc);
        
        // 密码输入框
        passwordField = new JPasswordField(15);
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(passwordField, gbc);
        
        // 状态标签
        statusLabel = new JLabel("请输入服务器信息、用户名和密码");
        statusLabel.setForeground(Color.BLUE);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(statusLabel, gbc);
        
        // 登录按钮
        loginButton = new JButton("登录");
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(loginButton, gbc);
        
        setContentPane(panel);
        
        // 默认按钮
        getRootPane().setDefaultButton(loginButton);
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 登录按钮点击事件
        loginButton.addActionListener(e -> login());
        
        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (client != null && client.isConnected()) {
                    client.disconnect();
                }
            }
        });
        
        // 历史服务器选择
        historyServersComboBox.addActionListener(e -> {
            ServerInfo selectedServer = (ServerInfo) historyServersComboBox.getSelectedItem();
            if (selectedServer != null) {
                serverAddressField.setText(selectedServer.getAddress());
                portSpinner.setValue(selectedServer.getPort());
            }
        });
    }
    
    /**
     * 执行登录操作
     */
    private void login() {
        // 获取服务器信息
        String serverAddress = serverAddressField.getText().trim();
        int serverPort = (Integer) portSpinner.getValue();
        
        // 获取用户名和密码
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        logger.info("尝试登录: 用户={}, 服务器={}:{}", username, serverAddress, serverPort);
        
        // 验证输入
        if (serverAddress.isEmpty()) {
            statusLabel.setText("请输入服务器地址");
            statusLabel.setForeground(Color.RED);
            return;
        }
        
        if (username.isEmpty()) {
            statusLabel.setText("请输入用户名");
            statusLabel.setForeground(Color.RED);
            return;
        }
        
        // 禁用登录按钮
        loginButton.setEnabled(false);
        statusLabel.setText("正在连接服务器...");
        statusLabel.setForeground(Color.BLUE);
        
        // 如果客户端为空或已断连，创建新客户端
        if (client == null || !client.isConnected()) {
            try {
                client = new ChatClient(serverAddress, serverPort);
                
                // 连接到服务器
                boolean connected = client.connect();
                if (!connected) {
                    loginButton.setEnabled(true);
                    statusLabel.setText("连接服务器失败");
                    statusLabel.setForeground(Color.RED);
                    logger.error("连接服务器失败");
                    return;
                }
                
                // 注册响应监听器
                client.getMessageHandler().addResponseListener(this);
            } catch (Exception e) {
                // 连接失败
                loginButton.setEnabled(true);
                statusLabel.setText("连接服务器失败: " + e.getMessage());
                statusLabel.setForeground(Color.RED);
                logger.error("连接服务器失败", e);
                return;
            }
        }
        
        // 保存连接历史
        addToHistory(serverAddress, serverPort);
        
        // 发送登录请求
        boolean loginSent = client.login(username, password);
        
        if (!loginSent) {
            loginButton.setEnabled(true);
            statusLabel.setText("发送登录请求失败");
            statusLabel.setForeground(Color.RED);
            logger.error("发送登录请求失败");
            client.disconnect();
        }
    }
    
    @Override
    public void onResponseReceived(ChatResponse response) {
        logger.info("收到响应: 类型={}, 成功={}, 消息={}", response.getType(), response.isSuccess(), response.getMessage());
        
        // 处理登录响应
        if (response.getType() == ResponseType.LOGIN_RESULT) {
            SwingUtilities.invokeLater(() -> {
                if (response.isSuccess()) {
                    // 登录成功，打开主界面
                    logger.info("登录成功，准备打开主界面");
                    statusLabel.setText("登录成功");
                    statusLabel.setForeground(Color.GREEN);
                    client.getMessageHandler().removeResponseListener(this);
                    
                    // 获取响应数据（用户和公共聊天室）
                    Object[] data = (Object[]) response.getData();
                    if (data != null && data.length > 0) {
                        // 设置当前用户
                        if (data[0] instanceof com.chatroom.common.model.User) {
                            client.setCurrentUser((com.chatroom.common.model.User) data[0]);
                            logger.info("已设置当前用户: {}", client.getCurrentUser().getUsername());
                        } else {
                            logger.error("响应数据中未找到用户对象");
                            statusLabel.setText("登录失败: 服务器响应格式错误");
                            statusLabel.setForeground(Color.RED);
                            loginButton.setEnabled(true);
                            return;
                        }
                        
                        // 如果存在公共聊天室，将其传递给主窗口
                        com.chatroom.common.model.ChatGroup publicChatRoom = null;
                        if (data.length > 1 && data[1] instanceof com.chatroom.common.model.ChatGroup) {
                            publicChatRoom = (com.chatroom.common.model.ChatGroup) data[1];
                            logger.info("收到公共聊天室: {}", publicChatRoom.getGroupName());
                        }
                        
                        // 打开主聊天窗口，传递公共聊天室
                        openChatMainFrame(publicChatRoom);
                    } else {
                        logger.error("响应数据为空");
                        statusLabel.setText("登录失败: 服务器响应格式错误");
                        statusLabel.setForeground(Color.RED);
                        loginButton.setEnabled(true);
                    }
                } else {
                    // 登录失败
                    logger.warn("登录失败: {}", response.getMessage());
                    statusLabel.setText("登录失败: " + response.getMessage());
                    statusLabel.setForeground(Color.RED);
                    loginButton.setEnabled(true);
                }
            });
        } else {
            logger.debug("忽略非登录响应: {}", response.getType());
        }
    }
    
    /**
     * 打开主聊天窗口
     * 
     * @param publicChatRoom 公共聊天室（可为null）
     */
    private void openChatMainFrame(com.chatroom.common.model.ChatGroup publicChatRoom) {
        // 关闭登录窗口
        setVisible(false);
        dispose();
        
        // 打开主聊天窗口
        SwingUtilities.invokeLater(() -> {
            ChatMainFrame mainFrame = new ChatMainFrame(client, publicChatRoom);
            mainFrame.setVisible(true);
        });
    }
    
    /**
     * 刷新历史服务器下拉框
     */
    private void refreshHistoryComboBox() {
        historyServersComboBox.removeAllItems();
        
        if (connectionHistory.isEmpty()) {
            historyServersComboBox.setEnabled(false);
        } else {
            historyServersComboBox.setEnabled(true);
            for (ServerInfo server : connectionHistory) {
                historyServersComboBox.addItem(server);
            }
        }
    }
    
    /**
     * 加载上次使用的服务器设置
     */
    private void loadLastServerSettings() {
        if (!connectionHistory.isEmpty()) {
            ServerInfo lastServer = connectionHistory.get(0);
            serverAddressField.setText(lastServer.getAddress());
            portSpinner.setValue(lastServer.getPort());
        }
    }
    
    /**
     * 加载连接历史
     */
    private List<ServerInfo> loadConnectionHistory() {
        List<ServerInfo> history = new ArrayList<>();
        
        Properties props = new Properties();
        File configFile = new File(CONFIG_PATH);
        
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                
                int historySize = Integer.parseInt(props.getProperty("history.size", "0"));
                for (int i = 0; i < historySize; i++) {
                    String name = props.getProperty("history." + i + ".name", "服务器 " + (i + 1));
                    String address = props.getProperty("history." + i + ".address");
                    int port = Integer.parseInt(props.getProperty("history." + i + ".port", "9999"));
                    
                    if (address != null && !address.isEmpty()) {
                        history.add(new ServerInfo(name, address, port));
                    }
                }
                
                logger.info("已加载 {} 个历史连接记录", history.size());
            } catch (IOException | NumberFormatException e) {
                logger.error("加载连接历史失败: {}", e.getMessage());
            }
        }
        
        return history;
    }
    
    /**
     * 保存连接历史
     */
    private void saveConnectionHistory() {
        Properties props = new Properties();
        
        props.setProperty("history.size", String.valueOf(connectionHistory.size()));
        for (int i = 0; i < connectionHistory.size(); i++) {
            ServerInfo server = connectionHistory.get(i);
            props.setProperty("history." + i + ".name", server.getName());
            props.setProperty("history." + i + ".address", server.getAddress());
            props.setProperty("history." + i + ".port", String.valueOf(server.getPort()));
        }
        
        try (FileOutputStream fos = new FileOutputStream(CONFIG_PATH)) {
            props.store(fos, "Chat Client Configuration");
            logger.info("已保存 {} 个历史连接记录", connectionHistory.size());
        } catch (IOException e) {
            logger.error("保存连接历史失败: {}", e.getMessage());
        }
    }
    
    /**
     * 添加服务器到连接历史
     */
    private void addToHistory(String address, int port) {
        // 创建服务器信息对象
        String name = "服务器 (" + address + ")";
        ServerInfo newServer = new ServerInfo(name, address, port);
        
        // 检查是否已存在
        for (int i = 0; i < connectionHistory.size(); i++) {
            ServerInfo server = connectionHistory.get(i);
            if (server.equals(newServer)) {
                // 已存在，移到列表顶部
                connectionHistory.remove(i);
                connectionHistory.add(0, newServer);
                saveConnectionHistory();
                return;
            }
        }
        
        // 不存在，添加到列表顶部
        connectionHistory.add(0, newServer);
        
        // 如果超过最大历史记录数，移除最旧的
        if (connectionHistory.size() > MAX_HISTORY_SIZE) {
            connectionHistory.remove(connectionHistory.size() - 1);
        }
        
        // 保存连接历史
        saveConnectionHistory();
    }
    
    /**
     * 服务器信息类
     */
    public static class ServerInfo {
        private final String name;
        private final String address;
        private final int port;
        
        public ServerInfo(String name, String address, int port) {
            this.name = name;
            this.address = address;
            this.port = port;
        }
        
        public String getName() {
            return name;
        }
        
        public String getAddress() {
            return address;
        }
        
        public int getPort() {
            return port;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ServerInfo)) return false;
            
            ServerInfo other = (ServerInfo) obj;
            return address.equals(other.address) && port == other.port;
        }
        
        @Override
        public int hashCode() {
            int result = address.hashCode();
            result = 31 * result + port;
            return result;
        }
        
        @Override
        public String toString() {
            return name + " (" + address + ":" + port + ")";
        }
    }
    
    /**
     * 程序入口
     */
    public static void main(String[] args) {
        // 设置界面外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.error("设置界面外观失败: {}", e.getMessage());
        }
        
        // 启动登录界面
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
} 