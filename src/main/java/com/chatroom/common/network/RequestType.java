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
     * 邀请加入群组
     */
    INVITE_TO_GROUP,
    
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
     * 获取历史消息请求
     */
    GET_HISTORY_MESSAGES,
    
    /**
     * 创建小组
     */
    CREATE_SUBGROUP,
    
    /**
     * 邀请加入小组
     */
    INVITE_TO_SUBGROUP,
    
    /**
     * 加入小组
     */
    JOIN_SUBGROUP,
    
    /**
     * 离开小组
     */
    LEAVE_SUBGROUP,
    
    /**
     * 获取小组列表
     */
    GET_SUBGROUPS,
    
    /**
     * 获取小组成员列表
     */
    GET_SUBGROUP_MEMBERS
} 