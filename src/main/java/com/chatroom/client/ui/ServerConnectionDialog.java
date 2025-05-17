package com.chatroom.client.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 服务器连接对话框
 * 提供服务器地址输入和连接历史功能
 */
public class ServerConnectionDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(ServerConnectionDialog.class);
    
    // 配置文件路径
    private static final String CONFIG_PATH = "client_config.properties";
    
    // 默认服务器设置
    private static final String DEFAULT_SERVER = "localhost";
    private static final int DEFAULT_PORT = 9999;
    private static final int MAX_HISTORY_SIZE = 10;
    
    // UI组件
    private JTextField serverAddressField;
    private JSpinner portSpinner;
    private JComboBox<ServerInfo> historyServersComboBox;
    private JButton connectButton;
    private JButton cancelButton;
    private JLabel statusLabel;
    
    // 修复：将标签声明为类成员变量
    private JLabel serverAddressLabel;
    private JLabel portLabel;
    private JLabel historyLabel;
    
    // 连接历史
    private List<ServerInfo> connectionHistory;
    
    // 对话框结果
    private boolean connectPressed = false;
    private String serverAddress;
    private int serverPort;
    
    /**
     * 构造方法
     * 
     * @param parent 父窗口
     */
    public ServerConnectionDialog(Frame parent) {
        super(parent, "连接到聊天服务器", true);
        
        // 初始化
        connectionHistory = loadConnectionHistory();
        
        // 设置UI
        initComponents();
        setupLayout();
        setupListeners();
        
        // 加载上次使用的服务器设置
        loadLastServerSettings();
        
        // 对话框设置
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(parent);
        setResizable(false);
    }
    
    /**
     * 初始化组件
     */
    private void initComponents() {
        // 服务器地址输入
        serverAddressLabel = new JLabel("服务器地址:");
        serverAddressField = new JTextField(DEFAULT_SERVER, 20);
        
        // 端口输入
        portLabel = new JLabel("端口:");
        SpinnerNumberModel portModel = new SpinnerNumberModel(DEFAULT_PORT, 1024, 65535, 1);
        portSpinner = new JSpinner(portModel);
        
        // 历史服务器下拉框
        historyLabel = new JLabel("连接历史:");
        historyServersComboBox = new JComboBox<>();
        
        // 刷新连接历史
        refreshHistoryComboBox();
        
        // 按钮
        connectButton = new JButton("连接");
        cancelButton = new JButton("取消");
        
        // 状态标签
        statusLabel = new JLabel("请输入服务器地址和端口");
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 服务器设置面板
        JPanel serverPanel = new JPanel(new GridBagLayout());
        serverPanel.setBorder(BorderFactory.createTitledBorder("服务器设置"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 添加组件到服务器面板
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        serverPanel.add(serverAddressLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        serverPanel.add(serverAddressField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        serverPanel.add(portLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        serverPanel.add(portSpinner, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        serverPanel.add(historyLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        serverPanel.add(historyServersComboBox, gbc);
        
        // 状态面板
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.add(statusLabel, BorderLayout.NORTH);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        
        // 添加到主面板
        mainPanel.add(serverPanel, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 设置内容面板
        setContentPane(mainPanel);
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 连接按钮
        connectButton.addActionListener(this::connectButtonActionPerformed);
        
        // 取消按钮
        cancelButton.addActionListener(e -> dispose());
        
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
     * 
     * @return 连接历史列表
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
                    int port = Integer.parseInt(props.getProperty("history." + i + ".port", String.valueOf(DEFAULT_PORT)));
                    
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
     * 
     * @param address 服务器地址
     * @param port 服务器端口
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
     * 连接按钮点击事件
     * 
     * @param evt 事件对象
     */
    private void connectButtonActionPerformed(ActionEvent evt) {
        // 获取服务器地址和端口
        serverAddress = serverAddressField.getText().trim();
        serverPort = (int) portSpinner.getValue();
        
        // 验证输入
        if (serverAddress.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入服务器地址", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 添加到连接历史
        addToHistory(serverAddress, serverPort);
        
        // 设置对话框结果
        connectPressed = true;
        
        // 关闭对话框
        dispose();
    }
    
    /**
     * 获取服务器地址
     * 
     * @return 服务器地址
     */
    public String getServerAddress() {
        return serverAddress;
    }
    
    /**
     * 获取服务器端口
     * 
     * @return 服务器端口
     */
    public int getServerPort() {
        return serverPort;
    }
    
    /**
     * 是否点击了连接按钮
     * 
     * @return 是否点击了连接按钮
     */
    public boolean isConnectPressed() {
        return connectPressed;
    }
    
    /**
     * 显示对话框并获取结果
     * 
     * @param parent 父窗口
     * @return 包含服务器信息的数组，如果用户取消则返回null
     */
    public static String[] showDialog(Frame parent) {
        ServerConnectionDialog dialog = new ServerConnectionDialog(parent);
        dialog.setVisible(true);
        
        // 对话框关闭后检查结果
        if (dialog.isConnectPressed()) {
            return new String[] {
                    dialog.getServerAddress(),
                    String.valueOf(dialog.getServerPort())
            };
        } else {
            return null;
        }
    }
    
    /**
     * 连接诊断
     * 检查连接是否可用，并提供错误信息
     * 
     * @param host 主机地址
     * @param port 端口
     * @return 诊断结果
     */
    public static String diagnoseConnection(String host, int port) {
        StringBuilder diagnosis = new StringBuilder();
        
        try {
            // 检查主机是否可达
            InetAddress address = InetAddress.getByName(host);
            boolean reachable = address.isReachable(2000);
            
            if (!reachable) {
                diagnosis.append("无法连接到服务器主机，可能的原因：\n");
                diagnosis.append("- 服务器地址错误\n");
                diagnosis.append("- 服务器主机未启动\n");
                diagnosis.append("- 服务器和客户端不在同一网络\n");
                diagnosis.append("- 防火墙阻止了连接\n");
                return diagnosis.toString();
            }
            
            // 尝试连接到指定端口
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 2000);
                
                // 连接成功
                return null;
            } catch (IOException e) {
                diagnosis.append("服务器主机可达，但无法连接到指定端口，可能的原因：\n");
                diagnosis.append("- 服务器程序未启动\n");
                diagnosis.append("- 端口号错误\n");
                diagnosis.append("- 防火墙阻止了该端口的连接\n");
                return diagnosis.toString();
            }
        } catch (IOException e) {
            diagnosis.append("连接诊断出错：").append(e.getMessage()).append("\n");
            diagnosis.append("可能的原因：\n");
            diagnosis.append("- 网络连接问题\n");
            diagnosis.append("- 无效的服务器地址\n");
            return diagnosis.toString();
        }
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
} 