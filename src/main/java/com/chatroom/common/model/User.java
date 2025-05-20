package com.chatroom.common.model;

import java.io.Serializable;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

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
     * 密码哈希
     */
    private String passwordHash;
    
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
    public User(String userId, String username, String passwordHash, UserStatus status, long lastActiveTime) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
        this.lastActiveTime = lastActiveTime;
    }
    
    /**
     * 创建一个带有随机ID的新用户
     * 
     * @param username 用户名
     * @param password 密码（明文）
     * @return 新用户对象
     */
    public static User createUser(String username, String password) {
        User user = new User();
        user.setUserId(generateUserIdFromUsername(username));
        user.setUsername(username);
        user.setPasswordHash(hashPassword(password));
        user.setStatus(UserStatus.ONLINE);
        user.setLastActiveTime(System.currentTimeMillis());
        return user;
    }
    
    /**
     * 创建一个带有随机ID的新用户（用于向下兼容）
     * 
     * @param username 用户名
     * @return 新用户对象
     */
    public static User createUser(String username) {
        return createUser(username, "");
    }
    
    /**
     * 验证密码
     * 
     * @param inputPassword 输入的密码（明文）
     * @return 是否匹配
     */
    public boolean verifyPassword(String inputPassword) {
        if (passwordHash == null || passwordHash.isEmpty()) {
            // 如果没有密码设置，任何密码都可以通过
            return true;
        }
        String inputHash = hashPassword(inputPassword);
        return passwordHash.equals(inputHash);
    }
    
    /**
     * 对密码进行哈希处理
     * 
     * @param password 明文密码
     * @return 哈希后的密码
     */
    private static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 如果SHA-256不可用，直接返回密码（不安全，但至少可以运行）
            return password;
        }
    }
    
    /**
     * 从用户名生成固定的用户ID
     * 
     * @param username 用户名
     * @return 用户ID
     */
    private static String generateUserIdFromUsername(String username) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(username.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 如果SHA-256不可用，使用简单的字符串处理
            return username.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        }
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
     * 获取密码哈希
     */
    public String getPasswordHash() {
        return passwordHash;
    }
    
    /**
     * 设置密码哈希
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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