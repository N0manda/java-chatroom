package com.chatroom.server.ui;

import com.chatroom.server.ChatServer;
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
    
    private final ServerConfig config;
    private JComboBox<NetworkAddressInfo> addressComboBox;
    private JSpinner portSpinner;
    private JCheckBox autoSelectPortCheckBox;
    private JButton startButton;
    private JButton cancelButton;
    private JTextArea logArea;
    
    /**
     * 构造方法
     */
    public ServerConfigFrame() {
        this.config = ServerConfig.getInstance();
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
        
        List<NetworkAddressInfo> addresses = config.getAvailableNetworkAddresses();
        addressComboBox = new JComboBox<>(addresses.toArray(new NetworkAddressInfo[0]));
        
        SpinnerNumberModel portModel = new SpinnerNumberModel(config.getPort(), 1024, 65535, 1);
        portSpinner = new JSpinner(portModel);
        
        autoSelectPortCheckBox = new JCheckBox("端口被占用时自动选择其他端口");
        autoSelectPortCheckBox.setSelected(config.isAutoSelectPort());
        
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
        configPanel.add(addressComboBox, gbc);
        
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
        String currentBindAddress = config.getBindAddress();
        for (int i = 0; i < addressComboBox.getItemCount(); i++) {
            NetworkAddressInfo item = addressComboBox.getItemAt(i);
            if (item.getAddress().equals(currentBindAddress)) {
                addressComboBox.setSelectedIndex(i);
                break;
            }
        }
        
        // 设置当前端口
        portSpinner.setValue(config.getPort());
        
        // 设置自动选择端口
        autoSelectPortCheckBox.setSelected(config.isAutoSelectPort());
    }
    
    /**
     * 保存配置
     */
    private void saveConfig() {
        NetworkAddressInfo selectedAddress = (NetworkAddressInfo) addressComboBox.getSelectedItem();
        if (selectedAddress != null) {
            config.setBindAddress(selectedAddress.getAddress());
        }
        
        config.setPort((Integer) portSpinner.getValue());
        config.setAutoSelectPort(autoSelectPortCheckBox.isSelected());
        config.saveConfig();
        
        log("配置已保存: 绑定地址=" + config.getBindAddress() + ", 端口=" + config.getPort() + 
            ", 自动选择端口=" + config.isAutoSelectPort());
    }
    
    /**
     * 启动服务器
     */
    private void startServer() {
        saveConfig();
        
        // 禁用界面元素
        addressComboBox.setEnabled(false);
        portSpinner.setEnabled(false);
        autoSelectPortCheckBox.setEnabled(false);
        startButton.setEnabled(false);
        
        log("正在启动服务器...");
        
        // 检查端口可用性
        boolean portAvailable = checkPortAvailability(config.getBindAddress(), config.getPort());
        int actualPort = config.getPort();
        
        if (!portAvailable && config.isAutoSelectPort()) {
            // 自动查找可用端口
            actualPort = findAvailablePort(config.getBindAddress());
            if (actualPort != -1) {
                log("端口 " + config.getPort() + " 已被占用，自动切换到端口 " + actualPort);
            } else {
                log("错误: 无法找到可用端口，请手动配置端口");
                resetUI();
                return;
            }
        } else if (!portAvailable) {
            log("错误: 端口 " + config.getPort() + " 已被占用，请选择其他端口或启用自动选择端口功能");
            resetUI();
            return;
        }
        
        // 启动服务器
        final int finalPort = actualPort;
        new Thread(() -> {
            try {
                ChatServer server = new ChatServer(config.getBindAddress(), finalPort);
                log("服务器已启动，监听地址: " + config.getBindAddress() + ":" + finalPort);
                
                // 显示IP地址提示
                if (config.getBindAddress().equals("0.0.0.0")) {
                    try {
                        String localIP = InetAddress.getLocalHost().getHostAddress();
                        log("服务器对外IP地址: " + localIP + ":" + finalPort);
                        log("其他用户可以通过以上地址连接到服务器");
                    } catch (Exception ex) {
                        logger.error("获取本地IP地址失败", ex);
                    }
                }
                
                server.start();
            } catch (Exception ex) {
                log("启动服务器失败: " + ex.getMessage());
                logger.error("启动服务器失败", ex);
                resetUI();
            }
        }).start();
    }
    
    /**
     * 重置界面元素
     */
    private void resetUI() {
        SwingUtilities.invokeLater(() -> {
            addressComboBox.setEnabled(true);
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
            for (int port = config.getPort() + 1; port < 65535; port++) {
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
    
    /**
     * 主函数
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.error("设置界面外观失败", e);
        }
        
        SwingUtilities.invokeLater(() -> {
            new ServerConfigFrame().setVisible(true);
        });
    }
} 