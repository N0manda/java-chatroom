package com.chatroom.client;

import com.chatroom.common.model.Message;
import com.chatroom.common.network.ChatRequest;
import com.chatroom.common.network.ChatResponse;
import com.chatroom.common.network.RequestType;
import com.chatroom.common.network.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 消息处理器类，负责处理从服务器接收的消息
 */
public class MessageHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    
    /**
     * 客户端引用
     */
    private final ChatClient client;
    
    /**
     * 是否运行
     */
    private boolean running;
    
    /**
     * 消息监听器列表
     */
    private final List<MessageListener> messageListeners;
    
    /**
     * 响应监听器列表
     */
    private final List<ResponseListener> responseListeners;
    
    /**
     * 构造方法
     * 
     * @param client 客户端引用
     */
    public MessageHandler(ChatClient client) {
        this.client = client;
        this.running = true;
        this.messageListeners = new CopyOnWriteArrayList<>();
        this.responseListeners = new CopyOnWriteArrayList<>();
    }
    
    @Override
    public void run() {
        try {
            while (running && client.isConnected()) {
                try {
                    // 读取服务器发送的对象
                    Object obj = client.getInputStream().readObject();
                    
                    if (obj instanceof Message) {
                        // 处理消息
                        handleMessage((Message) obj);
                    } else if (obj instanceof ChatResponse) {
                        // 处理响应
                        handleResponse((ChatResponse) obj);
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("读取对象时出错: {}", e.getMessage());
                } catch (EOFException e) {
                    logger.warn("服务器已关闭连接");
                    break;
                } catch (IOException e) {
                    if (running) {
                        logger.error("读取数据时出错: {}", e.getMessage());
                    }
                    break;
                }
            }
        } finally {
            logger.info("消息处理器已停止");
        }
    }
    
    /**
     * 处理消息
     * 
     * @param message 消息对象
     */
    private void handleMessage(Message message) {
        // 通知所有消息监听器
        for (MessageListener listener : messageListeners) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception e) {
                logger.error("消息监听器处理消息出错: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 处理响应
     * 
     * @param response 响应对象
     */
    private void handleResponse(ChatResponse response) {
        logger.info("收到服务器响应: 类型={}, 成功={}, 消息={}", response.getType(), response.isSuccess(), response.getMessage());
        
        // 处理登录响应 - 同时接受LOGIN_RESULT和GENERIC_RESULT类型
        if (response.getType() == ResponseType.LOGIN_RESULT || 
            (response.getType() == ResponseType.GENERIC_RESULT && response.isSuccess() && "登录成功".equals(response.getMessage()))) {
            logger.info("处理登录响应: 成功={}, 消息={}", response.isSuccess(), response.getMessage());
            
            if (response.isSuccess()) {
                // 提取用户对象
                Object[] users = (Object[]) response.getData();
                if (users != null && users.length > 0) {
                    logger.info("登录响应包含 {} 个用户对象", users.length);
                    
                    for (Object user : users) {
                        if (user instanceof com.chatroom.common.model.User) {
                            com.chatroom.common.model.User currentUser = (com.chatroom.common.model.User) user;
                            logger.info("设置当前用户: {}", currentUser.getUsername());
                            client.setCurrentUser(currentUser);
                            
                            // 登录成功后，自动获取用户列表
                            logger.info("发送获取用户列表请求");
                            client.sendRequest(new ChatRequest(RequestType.GET_USERS, currentUser, null));
                            
                            break;
                        } else {
                            logger.warn("用户对象类型错误: {}", user.getClass().getName());
                        }
                    }
                } else {
                    logger.warn("登录响应不包含用户对象，尝试从请求中获取用户名");
                    // 如果服务器没有返回用户对象，我们可以从登录请求中获取用户名
                    String username = response.getMessage().replace("登录成功", "").trim();
                    if (username.isEmpty()) {
                        username = "用户" + System.currentTimeMillis(); // 生成临时用户名
                    }
                    // 创建临时用户对象
                    com.chatroom.common.model.User tempUser = com.chatroom.common.model.User.createUser(username);
                    logger.info("创建临时用户: {}", tempUser.getUsername());
                    client.setCurrentUser(tempUser);
                }
            }
        }
        
        // 通知所有响应监听器
        logger.info("通知 {} 个响应监听器", responseListeners.size());
        int notifiedCount = 0;
        
        for (ResponseListener listener : responseListeners) {
            try {
                logger.debug("通知响应监听器: {}", listener.getClass().getName());
                listener.onResponseReceived(response);
                notifiedCount++;
            } catch (Exception e) {
                logger.error("响应监听器处理响应出错: {}", e.getMessage(), e);
            }
        }
        
        logger.info("成功通知了 {} 个响应监听器", notifiedCount);
    }
    
    /**
     * 停止处理器
     */
    public void stop() {
        running = false;
    }
    
    /**
     * 添加消息监听器
     * 
     * @param listener 消息监听器
     */
    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }
    
    /**
     * 移除消息监听器
     * 
     * @param listener 消息监听器
     */
    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }
    
    /**
     * 添加响应监听器
     * 
     * @param listener 响应监听器
     */
    public void addResponseListener(ResponseListener listener) {
        responseListeners.add(listener);
    }
    
    /**
     * 移除响应监听器
     * 
     * @param listener 响应监听器
     */
    public void removeResponseListener(ResponseListener listener) {
        responseListeners.remove(listener);
    }
    
    /**
     * 获取消息监听器列表
     * 
     * @return 消息监听器列表
     */
    public List<MessageListener> getMessageListeners() {
        return messageListeners;
    }
    
    /**
     * 获取响应监听器列表
     * 
     * @return 响应监听器列表
     */
    public List<ResponseListener> getResponseListeners() {
        return responseListeners;
    }
    
    /**
     * 消息监听器接口
     */
    public interface MessageListener {
        /**
         * 收到消息时调用
         * 
         * @param message 消息对象
         */
        void onMessageReceived(Message message);
    }
    
    /**
     * 响应监听器接口
     */
    public interface ResponseListener {
        /**
         * 收到响应时调用
         * 
         * @param response 响应对象
         */
        void onResponseReceived(ChatResponse response);
    }
} 