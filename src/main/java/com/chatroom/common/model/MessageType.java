package com.chatroom.common.model;

import java.io.Serializable;

/**
 * 消息类型枚举
 */
public enum MessageType implements Serializable {
    /**
     * 文本消息
     */
    TEXT,
    
    /**
     * 图片消息
     */
    IMAGE,
    
    /**
     * 文件消息
     */
    FILE,
    
    /**
     * 语音消息
     */
    VOICE,
    
    /**
     * 系统消息
     */
    SYSTEM,
    
    /**
     * 控制消息 - 用于客户端和服务器之间的控制通信
     */
    CONTROL
} 