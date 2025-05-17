package com.chatroom.common.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * 消息模型类
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 发送者
     */
    private User sender;
    
    /**
     * 接收者（可以是用户或群组ID）
     */
    private String receiverId;
    
    /**
     * 是否是群消息
     */
    private boolean isGroupMessage;
    
    /**
     * 发送时间
     */
    private Date timestamp;
    
    /**
     * 默认构造函数
     */
    public Message() {
    }
    
    /**
     * 全参数构造函数
     */
    public Message(String messageId, MessageType type, String content, User sender, 
                  String receiverId, boolean isGroupMessage, Date timestamp) {
        this.messageId = messageId;
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.receiverId = receiverId;
        this.isGroupMessage = isGroupMessage;
        this.timestamp = timestamp;
    }
    
    /**
     * 创建一个文本消息
     * 
     * @param sender 发送者
     * @param receiverId 接收者ID
     * @param content 消息内容
     * @param isGroupMessage 是否是群消息
     * @return 新消息对象
     */
    public static Message createTextMessage(User sender, String receiverId, String content, boolean isGroupMessage) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setType(MessageType.TEXT);
        message.setContent(content);
        message.setSender(sender);
        message.setReceiverId(receiverId);
        message.setGroupMessage(isGroupMessage);
        message.setTimestamp(new Date());
        return message;
    }
    
    /**
     * 创建一个系统消息
     * 
     * @param content 消息内容
     * @param receiverId 接收者ID
     * @param isGroupMessage 是否是群消息
     * @return 系统消息对象
     */
    public static Message createSystemMessage(String content, String receiverId, boolean isGroupMessage) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setType(MessageType.SYSTEM);
        message.setContent(content);
        message.setReceiverId(receiverId);
        message.setGroupMessage(isGroupMessage);
        message.setTimestamp(new Date());
        return message;
    }
    
    /**
     * 获取消息ID
     */
    public String getMessageId() {
        return messageId;
    }
    
    /**
     * 设置消息ID
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    /**
     * 获取消息类型
     */
    public MessageType getType() {
        return type;
    }
    
    /**
     * 设置消息类型
     */
    public void setType(MessageType type) {
        this.type = type;
    }
    
    /**
     * 获取消息内容
     */
    public String getContent() {
        return content;
    }
    
    /**
     * 设置消息内容
     */
    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * 获取发送者
     */
    public User getSender() {
        return sender;
    }
    
    /**
     * 设置发送者
     */
    public void setSender(User sender) {
        this.sender = sender;
    }
    
    /**
     * 获取接收者ID
     */
    public String getReceiverId() {
        return receiverId;
    }
    
    /**
     * 设置接收者ID
     */
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    
    /**
     * 是否是群消息
     */
    public boolean isGroupMessage() {
        return isGroupMessage;
    }
    
    /**
     * 设置是否是群消息
     */
    public void setGroupMessage(boolean groupMessage) {
        isGroupMessage = groupMessage;
    }
    
    /**
     * 获取时间戳
     */
    public Date getTimestamp() {
        return timestamp;
    }
    
    /**
     * 设置时间戳
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
} 