package com.chatroom.client;

import com.chatroom.client.ui.LoginFrame;
import com.chatroom.common.model.Message;
import com.chatroom.common.model.User;
import com.chatroom.common.network.ChatRequest;
import com.chatroom.common.network.ChatResponse;
import com.chatroom.common.network.RequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聊天客户端主类
 */
public class ChatClient {
    private static final Logger logger = LoggerFactory.getLogger(ChatClient.class);
    
    /**
     * 服务器地址
     */
    private final String serverHost;
    
    /**
     * 服务器端口
     */
    private final int serverPort;
    
    /**
     * 客户端Socket
     */
    private Socket socket;
    
    /**
     * 输入流
     */
    private ObjectInputStream inputStream;
    
    /**
     * 输出流
     */
    private ObjectOutputStream outputStream;
    
    /**
     * 当前用户
     */
    private User currentUser;
    
    /**
     * 消息处理器
     */
    private MessageHandler messageHandler;
    
    /**
     * 线程池
     */
    private final ExecutorService executorService;
    
    /**
     * 是否已连接
     */
    private boolean connected;
    
    /**
     * 构造方法
     * 
     * @param serverHost 服务器地址
     * @param serverPort 服务器端口
     */
    public ChatClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.executorService = Executors.newCachedThreadPool();
        this.connected = false;
    }
    
    /**
     * 连接到服务器
     * 
     * @return 是否连接成功
     */
    public boolean connect() {
        try {
            // 连接服务器
            socket = new Socket(serverHost, serverPort);
            
            // 初始化流
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            
            // 创建消息处理器
            messageHandler = new MessageHandler(this);
            executorService.execute(messageHandler);
            
            connected = true;
            logger.info("已连接到服务器: {}:{}", serverHost, serverPort);
            return true;
        } catch (IOException e) {
            logger.error("连接服务器失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        if (!connected) return;
        
        try {
            // 发送注销请求
            if (currentUser != null) {
                sendRequest(ChatRequest.createLogoutRequest(currentUser));
            }
            
            if (messageHandler != null) {
                messageHandler.stop();
            }
            
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
            
            connected = false;
            logger.info("已断开与服务器的连接");
        } catch (IOException e) {
            logger.error("断开连接时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 发送请求
     * 
     * @param request 请求对象
     * @return 是否发送成功
     */
    public boolean sendRequest(ChatRequest request) {
        if (!connected || outputStream == null) return false;
        
        try {
            outputStream.writeObject(request);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            logger.error("发送请求失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送消息
     * 
     * @param message 消息对象
     * @return 是否发送成功
     */
    public boolean sendMessage(Message message) {
        ChatRequest request = new ChatRequest(
                RequestType.SEND_MESSAGE,
                currentUser,
                message
        );
        boolean success = sendRequest(request);
        
        // 发送成功后直接在本地显示该消息（无需等待服务器回显）
        if (success && messageHandler != null) {
            for (MessageHandler.MessageListener listener : messageHandler.getMessageListeners()) {
                try {
                    listener.onMessageReceived(message);
                } catch (Exception e) {
                    logger.error("本地消息显示出错", e);
                }
            }
        }
        
        return success;
    }
    
    /**
     * 登录
     * 
     * @param username 用户名
     * @param password 密码
     * @return 是否发送登录请求成功
     */
    public boolean login(String username, String password) {
        // 创建登录请求对象，包含用户名和密码
        ChatRequest request = new ChatRequest(
                RequestType.LOGIN,
                null,
                new String[]{username, password}
        );
        return sendRequest(request);
    }
    
    /**
     * 登录（向下兼容无密码版本）
     * 
     * @param username 用户名
     * @return 是否发送登录请求成功
     */
    public boolean login(String username) {
        return login(username, "");
    }
    
    /**
     * 注销
     * 
     * @return 是否发送注销请求成功
     */
    public boolean logout() {
        if (currentUser == null) return false;
        
        ChatRequest request = ChatRequest.createLogoutRequest(currentUser);
        boolean result = sendRequest(request);
        if (result) {
            disconnect();
        }
        return result;
    }
    
    /**
     * 请求历史消息
     * 
     * @param targetId 目标ID（用户ID或群组ID）
     * @param isGroup 是否是群组
     * @param limit 消息数量限制，负数表示不限制
     * @return 是否发送请求成功
     */
    public boolean requestHistoryMessages(String targetId, boolean isGroup, int limit) {
        if (currentUser == null) return false;
        
        Map<String, Object> params = new HashMap<>();
        params.put("targetId", targetId);
        params.put("isGroup", isGroup);
        if (limit > 0) {
            params.put("limit", limit);
        }
        
        ChatRequest request = new ChatRequest(
                RequestType.GET_HISTORY_MESSAGES,
                currentUser,
                params
        );
        
        logger.info("请求{}历史消息: targetId={}, limit={}",
                isGroup ? "群组" : "私聊", targetId, limit > 0 ? limit : "不限制");
        
        return sendRequest(request);
    }
    
    /**
     * 请求历史消息（默认50条）
     * 
     * @param targetId 目标ID（用户ID或群组ID）
     * @param isGroup 是否是群组
     * @return 是否发送请求成功
     */
    public boolean requestHistoryMessages(String targetId, boolean isGroup) {
        return requestHistoryMessages(targetId, isGroup, 50);
    }
    
    /**
     * 获取输入流，供消息处理器使用
     * 
     * @return 输入流
     */
    public ObjectInputStream getInputStream() {
        return inputStream;
    }
    
    /**
     * 设置当前用户
     * 
     * @param user 用户对象
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    /**
     * 获取当前用户
     * 
     * @return 当前用户
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * 获取服务器地址
     * 
     * @return 服务器地址
     */
    public String getServerHost() {
        return serverHost;
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
     * 是否已连接
     * 
     * @return 是否已连接
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * 获取消息处理器
     * 
     * @return 消息处理器
     */
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }
    
    /**
     * 主方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
} 