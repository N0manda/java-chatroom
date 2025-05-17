package com.chatroom.server.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务器配置类
 * 管理服务器的网络配置和端口设置
 */
public class ServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);
    
    // 配置文件路径
    private static final String CONFIG_PATH = "server_config.properties";
    
    // 默认配置
    private static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";
    private static final int DEFAULT_PORT = 9999;
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;
    
    // 配置属性
    private String bindAddress;
    private int port;
    private boolean autoSelectPort;
    
    // 单例实例
    private static ServerConfig instance;
    
    /**
     * 获取配置实例
     */
    public static synchronized ServerConfig getInstance() {
        if (instance == null) {
            instance = new ServerConfig();
        }
        return instance;
    }
    
    /**
     * 私有构造函数
     */
    private ServerConfig() {
        loadConfig();
    }
    
    /**
     * 加载配置
     */
    private void loadConfig() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_PATH);
        
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                
                bindAddress = props.getProperty("server.bind_address", DEFAULT_BIND_ADDRESS);
                port = Integer.parseInt(props.getProperty("server.port", String.valueOf(DEFAULT_PORT)));
                autoSelectPort = Boolean.parseBoolean(props.getProperty("server.auto_select_port", "false"));
                
                logger.info("从配置文件加载服务器配置: bindAddress={}, port={}, autoSelectPort={}", 
                        bindAddress, port, autoSelectPort);
            } catch (IOException | NumberFormatException e) {
                logger.error("加载配置文件失败: {}", e.getMessage());
                setDefaultConfig();
            }
        } else {
            logger.info("配置文件不存在，使用默认配置");
            setDefaultConfig();
            saveConfig();
        }
    }
    
    /**
     * 设置默认配置
     */
    private void setDefaultConfig() {
        bindAddress = DEFAULT_BIND_ADDRESS;
        port = DEFAULT_PORT;
        autoSelectPort = false;
    }
    
    /**
     * 保存配置到文件
     */
    public void saveConfig() {
        Properties props = new Properties();
        props.setProperty("server.bind_address", bindAddress);
        props.setProperty("server.port", String.valueOf(port));
        props.setProperty("server.auto_select_port", String.valueOf(autoSelectPort));
        
        try (FileOutputStream fos = new FileOutputStream(CONFIG_PATH)) {
            props.store(fos, "Chat Server Configuration");
            logger.info("服务器配置已保存");
        } catch (IOException e) {
            logger.error("保存配置文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取系统中所有可用的网络地址
     * 
     * @return 可用的网络地址列表
     */
    public List<NetworkAddressInfo> getAvailableNetworkAddresses() {
        List<NetworkAddressInfo> addressList = new ArrayList<>();
        
        // 添加通用地址
        addressList.add(new NetworkAddressInfo("0.0.0.0", "所有网络接口"));
        addressList.add(new NetworkAddressInfo("127.0.0.1", "本地环回地址"));
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // 跳过回环接口、虚拟接口和未运行的接口
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    // 只获取IPv4地址
                    if (address.getHostAddress().contains(":")) {
                        continue; // 跳过IPv6地址
                    }
                    
                    String displayName = networkInterface.getDisplayName();
                    String hostAddress = address.getHostAddress();
                    addressList.add(new NetworkAddressInfo(hostAddress, displayName));
                }
            }
        } catch (SocketException e) {
            logger.error("获取网络接口失败: {}", e.getMessage());
        }
        
        return addressList;
    }
    
    /**
     * 获取绑定地址
     */
    public String getBindAddress() {
        return bindAddress;
    }
    
    /**
     * 设置绑定地址
     */
    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }
    
    /**
     * 获取端口
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 设置端口
     */
    public void setPort(int port) {
        if (port >= MIN_PORT && port <= MAX_PORT) {
            this.port = port;
        } else {
            logger.warn("端口超出范围 ({}-{}), 使用默认端口: {}", MIN_PORT, MAX_PORT, DEFAULT_PORT);
            this.port = DEFAULT_PORT;
        }
    }
    
    /**
     * 是否自动选择端口
     */
    public boolean isAutoSelectPort() {
        return autoSelectPort;
    }
    
    /**
     * 设置是否自动选择端口
     */
    public void setAutoSelectPort(boolean autoSelectPort) {
        this.autoSelectPort = autoSelectPort;
    }
    
    /**
     * 网络地址信息类
     */
    public static class NetworkAddressInfo {
        private final String address;
        private final String description;
        
        public NetworkAddressInfo(String address, String description) {
            this.address = address;
            this.description = description;
        }
        
        public String getAddress() {
            return address;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return address + " (" + description + ")";
        }
    }
} 