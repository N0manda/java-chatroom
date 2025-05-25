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
     * 视频消息
     */
    VIDEO,
    
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
    CONTROL,
    
    /**
     * 小组创建消息
     */
    SUBGROUP_CREATE,
    
    /**
     * 小组邀请消息
     */
    SUBGROUP_INVITE,
    
    /**
     * 小组加入消息
     */
    SUBGROUP_JOIN,
    
    /**
     * 小组离开消息
     */
    SUBGROUP_LEAVE,
    
    /**
     * 小组消息
     */
    SUBGROUP_MESSAGE
} 