package com.chatroom.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 聊天会话模型类
 */
public class ChatSession implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 会话类型
     */
    private SessionType type;
    
    /**
     * 会话名称
     */
    private String sessionName;
    
    /**
     * 目标ID（用户ID或群组ID）
     */
    private String targetId;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 最后活跃时间
     */
    private Date lastActiveTime;
    
    /**
     * 未读消息数量
     */
    private int unreadCount;
    
    /**
     * 会话消息列表
     */
    private List<Message> messages;
    
    /**
     * 默认构造函数
     */
    public ChatSession() {
        this.messages = new ArrayList<>();
    }
    
    /**
     * 全参数构造函数
     */
    public ChatSession(String sessionId, SessionType type, String sessionName, String targetId, 
                     Date createTime, Date lastActiveTime, int unreadCount, List<Message> messages) {
        this.sessionId = sessionId;
        this.type = type;
        this.sessionName = sessionName;
        this.targetId = targetId;
        this.createTime = createTime;
        this.lastActiveTime = lastActiveTime;
        this.unreadCount = unreadCount;
        this.messages = messages != null ? messages : new ArrayList<>();
    }
    
    /**
     * 创建私聊会话
     * 
     * @param targetUser 目标用户
     * @return 新的会话对象
     */
    public static ChatSession createPrivateSession(User targetUser) {
        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setType(SessionType.PRIVATE);
        session.setSessionName(targetUser.getUsername());
        session.setTargetId(targetUser.getUserId());
        session.setCreateTime(new Date());
        session.setLastActiveTime(new Date());
        session.setUnreadCount(0);
        return session;
    }
    
    /**
     * 创建群聊会话
     * 
     * @param group 群组
     * @return 新的会话对象
     */
    public static ChatSession createGroupSession(ChatGroup group) {
        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setType(SessionType.GROUP);
        session.setSessionName(group.getGroupName());
        session.setTargetId(group.getGroupId());
        session.setCreateTime(new Date());
        session.setLastActiveTime(new Date());
        session.setUnreadCount(0);
        return session;
    }
    
    /**
     * 添加消息
     * 
     * @param message 消息对象
     */
    public void addMessage(Message message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
        this.lastActiveTime = new Date();
    }
    
    /**
     * 增加未读消息计数
     */
    public void increaseUnreadCount() {
        this.unreadCount++;
    }
    
    /**
     * 清除未读消息计数
     */
    public void clearUnreadCount() {
        this.unreadCount = 0;
    }
    
    /**
     * 获取会话ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * 设置会话ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * 获取会话类型
     */
    public SessionType getType() {
        return type;
    }
    
    /**
     * 设置会话类型
     */
    public void setType(SessionType type) {
        this.type = type;
    }
    
    /**
     * 获取会话名称
     */
    public String getSessionName() {
        return sessionName;
    }
    
    /**
     * 设置会话名称
     */
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }
    
    /**
     * 获取目标ID
     */
    public String getTargetId() {
        return targetId;
    }
    
    /**
     * 设置目标ID
     */
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
    
    /**
     * 获取创建时间
     */
    public Date getCreateTime() {
        return createTime;
    }
    
    /**
     * 设置创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    /**
     * 获取最后活跃时间
     */
    public Date getLastActiveTime() {
        return lastActiveTime;
    }
    
    /**
     * 设置最后活跃时间
     */
    public void setLastActiveTime(Date lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
    
    /**
     * 获取未读消息数量
     */
    public int getUnreadCount() {
        return unreadCount;
    }
    
    /**
     * 设置未读消息数量
     */
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    /**
     * 获取会话消息列表
     */
    public List<Message> getMessages() {
        return messages;
    }
    
    /**
     * 设置会话消息列表
     */
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
} 