package com.chatroom.common.model;

import java.io.Serializable;

/**
 * 会话类型枚举
 */
public enum SessionType implements Serializable {
    /**
     * 私聊会话
     */
    PRIVATE,
    
    /**
     * 群聊会话
     */
    GROUP,
    
    /**
     * 系统会话
     */
    SYSTEM
} 