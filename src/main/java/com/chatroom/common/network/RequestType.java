package com.chatroom.common.network;

import java.io.Serializable;

/**
 * 请求类型枚举
 */
public enum RequestType implements Serializable {
    /**
     * 登录请求
     */
    LOGIN,
    
    /**
     * 注销请求
     */
    LOGOUT,
    
    /**
     * 发送消息
     */
    SEND_MESSAGE,
    
    /**
     * 创建群组
     */
    CREATE_GROUP,
    
    /**
     * 加入群组
     */
    JOIN_GROUP,
    
    /**
     * 离开群组
     */
    LEAVE_GROUP,
    
    /**
     * 获取用户列表
     */
    GET_USERS,
    
    /**
     * 获取群组列表
     */
    GET_GROUPS,
    
    /**
     * 心跳检测
     */
    HEARTBEAT,
    
    /**
     * 传输文件
     */
    TRANSFER_FILE,
    
    /**
     * 语音通话
     */
    VOICE_CALL,
    
    /**
     * 获取历史消息
     */
    GET_HISTORY_MESSAGES
} 