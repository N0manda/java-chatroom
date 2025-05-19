package com.chatroom.server.ui;

import com.chatroom.server.ChatServer;
import com.chatroom.server.ChatServerLauncher;
import com.chatroom.server.config.ServerConfig;
import com.chatroom.server.config.ServerConfig.NetworkAddressInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务器配置界面
 * 用于配置服务器的IP地址和端口
 */
public class ServerConfigFrame extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(ServerConfigFrame.class);
    
    private final ServerConfig serverConfig;
    private JComboBox<NetworkAddressInfo> bindAddressComboBox;
    private JSpinner portSpinner;
    private JCheckBox autoSelectPortCheckBox;
    private JButton startButton;
    private JButton cancelButton;
    private JTextArea logArea;
    private ChatServer server;
    
    /**
     * 显示服务器配置界面
     */
    public static void showServerConfigFrame() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.error("设置界面外观失败", e);
        }
        
        SwingUtilities.invokeLater(() -> {
            ServerConfigFrame frame = new ServerConfigFrame();
            frame.setVisible(true);
        });
    }
    
    /**
     * 构造方法
     */
    public ServerConfigFrame() {
        this.serverConfig = ServerConfig.getInstance();
        initComponents();
        setupLayout();
        setupListeners();
        loadCurrentConfig();
    }
    
    /**
     * 初始化组件
     */
    private void initComponents() {
        setTitle("聊天室服务器配置");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        
        List<NetworkAddressInfo> addresses = serverConfig.getAvailableNetworkAddresses();
        bindAddressComboBox = new JComboBox<>(addresses.toArray(new NetworkAddressInfo[0]));
        
        SpinnerNumberModel portModel = new SpinnerNumberModel(serverConfig.getPort(), 1024, 65535, 1);
        portSpinner = new JSpinner(portModel);
        
        autoSelectPortCheckBox = new JCheckBox("端口被占用时自动选择其他端口");
        autoSelectPortCheckBox.setSelected(serverConfig.isAutoSelectPort());
        
        startButton = new JButton("启动服务器");
        cancelButton = new JButton("取消");
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 配置面板
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder("服务器配置"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 绑定地址
        gbc.gridx = 0;
        gbc.gridy = 0;
        configPanel.add(new JLabel("绑定地址:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        configPanel.add(bindAddressComboBox, gbc);
        
        // 端口
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        configPanel.add(new JLabel("端口:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        configPanel.add(portSpinner, gbc);
        
        // 自动选择端口
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        configPanel.add(autoSelectPortCheckBox, gbc);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(startButton);
        buttonPanel.add(cancelButton);
        
        // 日志面板
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("服务器日志"));
        logScrollPane.setPreferredSize(new Dimension(480, 200));
        
        // 添加到主面板
        mainPanel.add(configPanel, BorderLayout.NORTH);
        mainPanel.add(logScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 启动按钮
        startButton.addActionListener(e -> startServer());
        
        // 取消按钮
        cancelButton.addActionListener(e -> System.exit(0));
        
        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
    
    /**
     * 加载当前配置
     */
    private void loadCurrentConfig() {
        // 设置当前绑定地址
        String currentBindAddress = serverConfig.getBindAddress();
        for (int i = 0; i < bindAddressComboBox.getItemCount(); i++) {
            NetworkAddressInfo item = bindAddressComboBox.getItemAt(i);
            if (item.getAddress().equals(currentBindAddress)) {
                bindAddressComboBox.setSelectedIndex(i);
                break;
            }
        }
        
        // 设置当前端口
        portSpinner.setValue(serverConfig.getPort());
        
        // 设置自动选择端口
        autoSelectPortCheckBox.setSelected(serverConfig.isAutoSelectPort());
    }
    
    /**
     * 启动服务器
     */
    private void startServer() {
        int port = (Integer) portSpinner.getValue();
        String bindAddress = ((NetworkAddressInfo) bindAddressComboBox.getSelectedItem()).getAddress();
        
        // 保存配置
        serverConfig.setBindAddress(bindAddress);
        serverConfig.setPort(port);
        serverConfig.setAutoSelectPort(autoSelectPortCheckBox.isSelected());
        serverConfig.saveConfig();
        
        // 使用启动器启动服务器
        server = ChatServerLauncher.startServer(bindAddress, port);
        
        if (server != null) {
            updateUIForServerRunning();
        } else {
            JOptionPane.showMessageDialog(this,
                    "服务器启动失败，请检查端口是否被占用",
                    "启动错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 更新UI以反映服务器正在运行
     */
    private void updateUIForServerRunning() {
        // 禁用界面元素
        bindAddressComboBox.setEnabled(false);
        portSpinner.setEnabled(false);
        autoSelectPortCheckBox.setEnabled(false);
        startButton.setEnabled(false);
        
        log("服务器已启动，监听地址: " + serverConfig.getBindAddress() + ":" + serverConfig.getPort());
        
        // 显示IP地址提示
        if (serverConfig.getBindAddress().equals("0.0.0.0")) {
            try {
                String localIP = InetAddress.getLocalHost().getHostAddress();
                log("服务器对外IP地址: " + localIP + ":" + serverConfig.getPort());
                log("其他用户可以通过以上地址连接到服务器");
            } catch (Exception ex) {
                logger.error("获取本地IP地址失败", ex);
            }
        }
    }
    
    /**
     * 重置界面元素
     */
    private void resetUI() {
        SwingUtilities.invokeLater(() -> {
            bindAddressComboBox.setEnabled(true);
            portSpinner.setEnabled(true);
            autoSelectPortCheckBox.setEnabled(true);
            startButton.setEnabled(true);
        });
    }
    
    /**
     * 检查端口是否可用
     * 
     * @param bindAddress 绑定地址
     * @param port 端口
     * @return 端口是否可用
     */
    private boolean checkPortAvailability(String bindAddress, int port) {
        try {
            InetAddress bindAddr = bindAddress.equals("0.0.0.0") ? 
                    null : InetAddress.getByName(bindAddress);
            
            ServerSocket socket = new ServerSocket(port, 1, bindAddr);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 查找可用端口
     * 
     * @param bindAddress 绑定地址
     * @return 可用端口，如果没有可用端口则返回-1
     */
    private int findAvailablePort(String bindAddress) {
        try {
            InetAddress bindAddr = bindAddress.equals("0.0.0.0") ? 
                    null : InetAddress.getByName(bindAddress);
            
            // 尝试从基础端口开始找可用端口
            for (int port = serverConfig.getPort() + 1; port < 65535; port++) {
                try {
                    ServerSocket socket = new ServerSocket(port, 1, bindAddr);
                    socket.close();
                    return port;
                } catch (IOException ignored) {
                    // 继续尝试下一个端口
                }
            }
            
            return -1;
        } catch (IOException e) {
            return -1;
        }
    }
    
    /**
     * 添加日志
     * 
     * @param message 日志消息
     */
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
        logger.info(message);
    }
} 