package com.chatroom.common.network;

import com.chatroom.common.model.User;

import java.io.Serializable;
import java.util.UUID;

/**
 * 客户端请求对象
 */
public class ChatRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 命令类型
     */
    private RequestType type;
    
    /**
     * 请求发送者
     */
    private User sender;
    
    /**
     * 请求数据
     */
    private Object data;
    
    /**
     * 请求时间戳
     */
    private long timestamp;
    
    /**
     * 构造方法
     * 
     * @param type 请求类型
     * @param sender 发送者
     * @param data 数据
     */
    public ChatRequest(RequestType type, User sender, Object data) {
        this.requestId = UUID.randomUUID().toString();
        this.type = type;
        this.sender = sender;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 构造登录请求
     * 
     * @param username 用户名
     * @return 登录请求对象
     */
    public static ChatRequest createLoginRequest(String username) {
        return new ChatRequest(RequestType.LOGIN, null, username);
    }
    
    /**
     * 构造注销请求
     * 
     * @param user 用户
     * @return 注销请求对象
     */
    public static ChatRequest createLogoutRequest(User user) {
        return new ChatRequest(RequestType.LOGOUT, user, null);
    }
    
    /**
     * 获取请求ID
     * 
     * @return 请求ID
     */
    public String getRequestId() {
        return requestId;
    }
    
    /**
     * 获取请求类型
     * 
     * @return 请求类型
     */
    public RequestType getType() {
        return type;
    }
    
    /**
     * 获取发送者
     * 
     * @return 发送者
     */
    public User getSender() {
        return sender;
    }
    
    /**
     * 获取数据
     * 
     * @return 数据
     */
    public Object getData() {
        return data;
    }
    
    /**
     * 获取时间戳
     * 
     * @return 时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "ChatRequest{" +
                "requestId='" + requestId + '\'' +
                ", type=" + type +
                ", sender=" + (sender != null ? sender.getUsername() : "null") +
                ", timestamp=" + timestamp +
                '}';
    }
} 