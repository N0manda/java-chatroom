package com.chatroom.common.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * 用户模型类
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户状态
     */
    private UserStatus status;
    
    /**
     * 最后活跃时间
     */
    private long lastActiveTime;
    
    /**
     * 默认构造函数
     */
    public User() {
    }
    
    /**
     * 全参数构造函数
     */
    public User(String userId, String username, UserStatus status, long lastActiveTime) {
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.lastActiveTime = lastActiveTime;
    }
    
    /**
     * 创建一个带有随机ID的新用户
     * 
     * @param username 用户名
     * @return 新用户对象
     */
    public static User createUser(String username) {
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setStatus(UserStatus.ONLINE);
        user.setLastActiveTime(System.currentTimeMillis());
        return user;
    }
    
    /**
     * 更新用户活跃时间
     */
    public void updateActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 获取用户ID
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * 设置用户ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    /**
     * 获取用户名
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * 设置用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * 获取用户状态
     */
    public UserStatus getStatus() {
        return status;
    }
    
    /**
     * 设置用户状态
     */
    public void setStatus(UserStatus status) {
        this.status = status;
    }
    
    /**
     * 获取最后活跃时间
     */
    public long getLastActiveTime() {
        return lastActiveTime;
    }
    
    /**
     * 设置最后活跃时间
     */
    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
} 