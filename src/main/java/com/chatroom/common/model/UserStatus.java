package com.chatroom.common.model;

import java.io.Serializable;

/**
 * 用户状态枚举
 */
public enum UserStatus implements Serializable {
    /**
     * 在线状态
     */
    ONLINE,
    
    /**
     * 离线状态
     */
    OFFLINE,
    
    /**
     * 忙碌状态
     */
    BUSY,
    
    /**
     * 离开状态
     */
    AWAY
} 