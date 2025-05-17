package com.chatroom.common.model;

import java.io.Serializable;
import java.util.*;

/**
 * 聊天群组模型类
 */
public class ChatGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 群组ID
     */
    private String groupId;
    
    /**
     * 群组名称
     */
    private String groupName;
    
    /**
     * 群主ID
     */
    private String creatorId;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 成员ID列表
     */
    private Set<String> memberIds;
    
    /**
     * 默认构造函数
     */
    public ChatGroup() {
        this.memberIds = new HashSet<>();
    }
    
    /**
     * 全参数构造函数
     */
    public ChatGroup(String groupId, String groupName, String creatorId, Date createTime, Set<String> memberIds) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.creatorId = creatorId;
        this.createTime = createTime;
        this.memberIds = memberIds != null ? memberIds : new HashSet<>();
    }
    
    /**
     * 创建一个新的群组
     * 
     * @param groupName 群组名称
     * @param creator 创建者
     * @return 新群组对象
     */
    public static ChatGroup createGroup(String groupName, User creator) {
        String groupId = UUID.randomUUID().toString();
        Set<String> members = new HashSet<>();
        members.add(creator.getUserId());
        
        ChatGroup group = new ChatGroup();
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setCreatorId(creator.getUserId());
        group.setCreateTime(new Date());
        group.setMemberIds(members);
        return group;
    }
    
    /**
     * 添加成员
     * 
     * @param userId 用户ID
     * @return 是否添加成功
     */
    public boolean addMember(String userId) {
        return memberIds.add(userId);
    }
    
    /**
     * 移除成员
     * 
     * @param userId 用户ID
     * @return 是否移除成功
     */
    public boolean removeMember(String userId) {
        // 群主不能被移除
        if (userId.equals(creatorId)) {
            return false;
        }
        return memberIds.remove(userId);
    }
    
    /**
     * 检查用户是否是群成员
     * 
     * @param userId 用户ID
     * @return 是否是群成员
     */
    public boolean isMember(String userId) {
        return memberIds.contains(userId);
    }
    
    /**
     * 获取群组ID
     */
    public String getGroupId() {
        return groupId;
    }
    
    /**
     * 设置群组ID
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    /**
     * 获取群组名称
     */
    public String getGroupName() {
        return groupName;
    }
    
    /**
     * 设置群组名称
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    /**
     * 获取创建者ID
     */
    public String getCreatorId() {
        return creatorId;
    }
    
    /**
     * 设置创建者ID
     */
    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
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
     * 获取成员ID列表
     */
    public Set<String> getMemberIds() {
        return memberIds;
    }
    
    /**
     * 设置成员ID列表
     */
    public void setMemberIds(Set<String> memberIds) {
        this.memberIds = memberIds;
    }
} 