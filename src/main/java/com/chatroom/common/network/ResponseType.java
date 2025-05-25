package com.chatroom.common.network;

import java.io.Serializable;

/**
 * 响应类型枚举
 */
public enum ResponseType implements Serializable {
    /**
     * 登录响应
     */
    LOGIN,
    
    /**
     * 注销响应
     */
    LOGOUT,
    
    /**
     * 用户列表响应
     */
    USER_LIST,
    
    /**
     * 历史消息响应
     */
    HISTORY_MESSAGES,
    
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
    ERROR,
    
    /**
     * 小组结果
     */
    SUBGROUP_RESULT,
    
    /**
     * 小组列表
     */
    SUBGROUP_LIST,
    
    /**
     * 小组成员列表
     */
    SUBGROUP_MEMBERS
} 