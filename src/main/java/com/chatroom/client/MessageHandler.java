package com.chatroom.client;

import com.chatroom.common.model.Message;
import com.chatroom.common.model.MessageType;
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
        // 检查是否是异地登录的系统消息
        if (message.getType() == MessageType.SYSTEM && 
            message.getContent() != null && 
            message.getContent().contains("您的账号在其他地方登录")) {
            // 通知所有消息监听器
            for (MessageListener listener : messageListeners) {
                try {
                    listener.onMessageReceived(message);
                } catch (Exception e) {
                    logger.error("消息监听器处理消息出错: {}", e.getMessage());
                }
            }
            // 断开连接
            client.disconnect();
            return;
        }

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
        logger.debug("收到响应: type={}, success={}, message={}", 
            response.getType(), 
            response.isSuccess(), 
            response.getMessage());
        
        // 通知所有响应监听器
        for (ResponseListener listener : responseListeners) {
            try {
                listener.onResponseReceived(response);
            } catch (Exception e) {
                logger.error("响应监听器处理响应出错: {}", e.getMessage());
            }
        }
        
        // 如果是历史消息响应，且成功，则通知消息监听器
        if (response.getType() == ResponseType.HISTORY_MESSAGES && response.isSuccess()) {
            Object data = response.getData();
            if (data instanceof List<?>) {
                List<Message> messages = (List<Message>) data;
                logger.info("收到历史消息: {} 条", messages.size());
                
                // 按时间顺序显示消息
                for (Message message : messages) {
                    for (MessageListener listener : messageListeners) {
                        try {
                            listener.onMessageReceived(message);
                        } catch (Exception e) {
                            logger.error("消息监听器处理历史消息出错: {}", e.getMessage());
                        }
                    }
                }
            }
        }
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