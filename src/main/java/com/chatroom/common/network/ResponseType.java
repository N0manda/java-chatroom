package com.chatroom.common.network;

import java.io.Serializable;

/**
 * 响应类型枚举
 */
public enum ResponseType implements Serializable {
    /**
     * 登录结果
     */
    LOGIN_RESULT,
    
    /**
     * 注销结果
     */
    LOGOUT_RESULT,
    
    /**
     * 消息结果
     */
    MESSAGE_RESULT,
    
    /**
     * 群组结果
     */
    GROUP_RESULT,
    
    /**
     * 用户列表
     */
    USER_LIST,
    
    /**
     * 群组列表
     */
    GROUP_LIST,
    
    /**
     * 文件传输结果
     */
    FILE_RESULT,
    
    /**
     * 语音通话结果
     */
    VOICE_RESULT,
    
    /**
     * 通用结果
     */
    GENERIC_RESULT,
    
    /**
     * 错误
     */
    ERROR
} 