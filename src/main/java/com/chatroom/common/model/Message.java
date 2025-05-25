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
     * 是否是小组消息
     */
    private boolean isSubGroupMessage;
    
    /**
     * 小组ID（如果是小组消息）
     */
    private String subGroupId;
    
    /**
     * 发送时间
     */
    private Date timestamp;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小
     */
    private long fileSize;
    
    /**
     * 文件数据
     */
    private byte[] fileData;
    
    /**
     * 文件传输进度(0-100)
     */
    private int transferProgress;
    
    /**
     * 附加数据
     */
    private Object data;
    
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
     * 创建一个文件消息
     * 
     * @param sender 发送者
     * @param receiverId 接收者ID
     * @param fileName 文件名
     * @param fileData 文件数据
     * @param isGroupMessage 是否是群消息
     * @return 文件消息对象
     */
    public static Message createFileMessage(User sender, String receiverId, String fileName, byte[] fileData, boolean isGroupMessage) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setType(MessageType.FILE);
        message.setContent("发送文件：" + fileName);
        message.setSender(sender);
        message.setReceiverId(receiverId);
        message.setGroupMessage(isGroupMessage);
        message.setTimestamp(new Date());
        message.setFileName(fileName);
        message.setFileSize(fileData.length);
        message.setFileData(fileData);
        message.setTransferProgress(0);
        return message;
    }
    
    /**
     * 创建一个图片消息
     * 
     * @param sender 发送者
     * @param receiverId 接收者ID
     * @param fileName 图片文件名
     * @param fileData 图片数据
     * @param isGroupMessage 是否是群消息
     * @return 图片消息对象
     */
    public static Message createImageMessage(User sender, String receiverId, String fileName, byte[] fileData, boolean isGroupMessage) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setType(MessageType.IMAGE);
        message.setContent("发送图片：" + fileName);
        message.setSender(sender);
        message.setReceiverId(receiverId);
        message.setGroupMessage(isGroupMessage);
        message.setTimestamp(new Date());
        message.setFileName(fileName);
        message.setFileSize(fileData.length);
        message.setFileData(fileData);
        message.setTransferProgress(0);
        return message;
    }
    
    /**
     * 创建一个视频消息
     * 
     * @param sender 发送者
     * @param receiverId 接收者ID
     * @param fileName 视频文件名
     * @param fileData 视频数据
     * @param isGroupMessage 是否是群消息
     * @return 视频消息对象
     */
    public static Message createVideoMessage(User sender, String receiverId, String fileName, byte[] fileData, boolean isGroupMessage) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setType(MessageType.VIDEO);
        message.setContent("发送视频：" + fileName);
        message.setSender(sender);
        message.setReceiverId(receiverId);
        message.setGroupMessage(isGroupMessage);
        message.setTimestamp(new Date());
        message.setFileName(fileName);
        message.setFileSize(fileData.length);
        message.setFileData(fileData);
        message.setTransferProgress(0);
        return message;
    }
    
    /**
     * 创建一个小组文本消息
     * 
     * @param sender 发送者
     * @param subGroupId 小组ID
     * @param content 消息内容
     * @return 新消息对象
     */
    public static Message createSubGroupTextMessage(User sender, String subGroupId, String content) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setType(MessageType.SUBGROUP_MESSAGE);
        message.setContent(content);
        message.setSender(sender);
        message.setSubGroupId(subGroupId);
        message.setSubGroupMessage(true);
        message.setTimestamp(new Date());
        return message;
    }
    
    /**
     * 创建一个小组系统消息
     * 
     * @param content 消息内容
     * @param subGroupId 小组ID
     * @return 系统消息对象
     */
    public static Message createSubGroupSystemMessage(String content, String subGroupId) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setType(MessageType.SYSTEM);
        message.setContent(content);
        message.setSubGroupId(subGroupId);
        message.setSubGroupMessage(true);
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
    
    /**
     * 获取文件名
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * 设置文件名
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    /**
     * 获取文件大小
     */
    public long getFileSize() {
        return fileSize;
    }
    
    /**
     * 设置文件大小
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    /**
     * 获取文件数据
     */
    public byte[] getFileData() {
        return fileData;
    }
    
    /**
     * 设置文件数据
     */
    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }
    
    /**
     * 获取传输进度
     */
    public int getTransferProgress() {
        return transferProgress;
    }
    
    /**
     * 设置传输进度
     */
    public void setTransferProgress(int transferProgress) {
        this.transferProgress = transferProgress;
    }
    
    /**
     * 是否是小组消息
     */
    public boolean isSubGroupMessage() {
        return isSubGroupMessage;
    }
    
    /**
     * 设置是否是小组消息
     */
    public void setSubGroupMessage(boolean subGroupMessage) {
        isSubGroupMessage = subGroupMessage;
    }
    
    /**
     * 获取小组ID
     */
    public String getSubGroupId() {
        return subGroupId;
    }
    
    /**
     * 设置小组ID
     */
    public void setSubGroupId(String subGroupId) {
        this.subGroupId = subGroupId;
    }
    
    /**
     * 获取附加数据
     */
    public Object getData() {
        return data;
    }
    
    /**
     * 设置附加数据
     */
    public void setData(Object data) {
        this.data = data;
    }
} 