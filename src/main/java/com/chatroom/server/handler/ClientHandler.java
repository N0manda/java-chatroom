package com.chatroom.server.handler;

import com.chatroom.common.model.ChatGroup;
import com.chatroom.common.model.Message;
import com.chatroom.common.model.MessageType;
import com.chatroom.common.model.User;
import com.chatroom.common.model.RequestType;
import com.chatroom.common.network.ChatRequest;
import com.chatroom.common.network.ChatResponse;
import com.chatroom.common.network.ResponseType;
import com.chatroom.server.ChatServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户端处理器，处理单个客户端的请求
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    
    /**
     * 客户端Socket
     */
    private final Socket clientSocket;
    
    /**
     * 服务器引用
     */
    private final ChatServer server;
    
    /**
     * 输入流
     */
    private ObjectInputStream inputStream;
    
    /**
     * 输出流
     */
    private ObjectOutputStream outputStream;
    
    /**
     * 连接的用户
     */
    private User user;
    
    /**
     * 是否运行
     */
    private boolean running;
    
    /**
     * 构造方法
     * 
     * @param clientSocket 客户端Socket
     * @param server 服务器引用
     */
    public ClientHandler(Socket clientSocket, ChatServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.running = true;
    }
    
    @Override
    public void run() {
        try {
            // 初始化流
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            inputStream = new ObjectInputStream(clientSocket.getInputStream());
            
            // 处理客户端请求
            while (running) {
                try {
                    Object obj = inputStream.readObject();
                    if (obj instanceof ChatRequest) {
                        handleRequest((ChatRequest) obj);
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("读取对象时出错: {}", e.getMessage());
                } catch (IOException e) {
                    // 客户端断开连接
                    logger.info("客户端断开连接: {}", clientSocket.getRemoteSocketAddress());
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("处理客户端时出错: {}", e.getMessage());
        } finally {
            // 处理客户端断开连接
            if (user != null) {
                server.handleLogout(user.getUserId());
            }
            disconnect(null);
        }
    }
    
    /**
     * 处理客户端请求
     * 
     * @param request 请求对象
     */
    private void handleRequest(ChatRequest request) {
        if (request == null) return;
        
        try {
            switch (request.getType()) {
                case LOGIN:
                    handleLogin(request);
                    break;
                case LOGOUT:
                    handleLogout(request);
                    break;
                case SEND_MESSAGE:
                    handleSendMessage(request);
                    break;
                case JOIN_GROUP:
                    handleJoinGroup(request);
                    break;
                case LEAVE_GROUP:
                    handleLeaveGroup(request);
                    break;
                case CREATE_GROUP:
                    handleCreateGroup(request);
                    break;
                case GET_USERS:
                    handleGetUsers(request);
                    break;
                case GET_GROUPS:
                    handleGetGroups(request);
                    break;
                case GET_HISTORY_MESSAGES:
                    server.handleRequest(request, this);
                    break;
                case HEARTBEAT:
                    handleHeartbeat(request);
                    break;
                case INVITE_TO_GROUP:
                    handleInviteToGroup(request);
                    break;
                case DISMISS_GROUP:
                    handleDismissGroup(request);
                    break;
                default:
                    logger.warn("未知请求类型: {}", request.getType());
                    break;
            }
        } catch (Exception e) {
            logger.error("处理请求时出错: {}", e.getMessage());
            sendResponse(ChatResponse.createErrorResponse(request, "处理请求时出错: " + e.getMessage()));
        }
    }
    
    /**
     * 处理登录请求
     * 
     * @param request 请求对象
     */
    private void handleLogin(ChatRequest request) {
        try {
            Object requestData = request.getData();
            if (!(requestData instanceof String[]) || ((String[])requestData).length < 1) {
                sendResponse(ChatResponse.createErrorResponse(
                        ResponseType.LOGIN_RESULT, 
                        "无效的登录数据格式", 
                        null));
                return;
            }
            
            String[] loginData = (String[])requestData;
            String username = loginData[0];
            String password = loginData.length > 1 ? loginData[1] : "";
            
            logger.info("用户尝试登录: {}，使用密码认证", username);
            
            // 创建新用户对象，但此时还未验证
            User loginUser = User.createUser(username, password);
            logger.info("已创建用户对象: {}", loginUser);
            
            // 检查该用户名是否已经存在
            for (ClientHandler existingHandler : server.getOnlineUsers().values()) {
                User existingUser = existingHandler.getUser();
                if (existingUser != null && existingUser.getUsername().equals(username)) {
                    // 用户已在线，踢掉旧连接
                    logger.info("用户 {} 已在其他位置登录，踢掉旧连接", username);
                    existingHandler.disconnect("您的账号在其他地方登录，此连接已断开");
                    server.getOnlineUsers().remove(existingUser.getUserId());
                    break;
                }
            }
            
            // 验证用户
            ChatResponse response = server.handleLogin(loginUser, this);
            
            if (!response.isSuccess()) {
                user = null;
                logger.warn("用户登录失败: {}", response.getMessage());
            } else {
                user = loginUser;
                logger.info("用户登录成功，开始获取用户加入的群组列表");
                
                // 登录成功后，获取用户加入的群组列表
                List<ChatGroup> userGroups = new ArrayList<>();
                Map<String, ChatGroup> allGroups = server.getChatGroups();
                logger.info("服务器共有 {} 个群组", allGroups.size());
                
                for (ChatGroup group : allGroups.values()) {
                    logger.info("检查群组: {} (ID: {})", group.getGroupName(), group.getGroupId());
                    logger.info("  群组成员列表: {}", group.getMemberIds());
                    logger.info("  当前用户ID: {}", user.getUserId());
                    logger.info("  用户是否在群组中: {}", group.isMember(user.getUserId()));
                    
                    if (group.isMember(user.getUserId())) {
                        logger.info("用户 {} 是群组 {} 的成员", user.getUsername(), group.getGroupName());
                        userGroups.add(group);
                    }
                }
                
                logger.info("用户 {} 加入了 {} 个群组", user.getUsername(), userGroups.size());
                if (userGroups.isEmpty()) {
                    logger.warn("用户 {} 没有加入任何群组", user.getUsername());
                } else {
                    logger.info("用户加入的群组列表: {}", userGroups.stream()
                        .map(g -> g.getGroupName() + "(" + g.getGroupId() + ")")
                        .collect(Collectors.joining(", ")));
                }
                
                // 发送群组列表
                ChatResponse groupListResponse = new ChatResponse(
                    null,
                    ResponseType.GROUP_LIST,
                    true,
                    "获取群组列表成功",
                    userGroups.toArray()
                );
                sendResponse(groupListResponse);
                
                // 通知所有在线用户刷新群组列表
                server.broadcastMessage(Message.createSystemMessage("[REFRESH_GROUPS]", null, false));
            }
            
            // 发送响应
            sendResponse(response);
            logger.info("已发送登录响应");
        } catch (Exception e) {
            logger.error("处理登录请求时出错", e);
            sendResponse(ChatResponse.createErrorResponse(
                    ResponseType.LOGIN_RESULT, 
                    "登录处理失败: " + e.getMessage(), 
                    null));
        }
    }
    
    /**
     * 处理注销请求
     * 
     * @param request 请求对象
     */
    private void handleLogout(ChatRequest request) {
        if (user != null) {
            // 通知所有在线用户刷新群组列表
            server.broadcastMessage(Message.createSystemMessage("[REFRESH_GROUPS]", null, false));
            
            server.handleLogout(user.getUserId());
            sendResponse(ChatResponse.createSuccessResponse(request, "注销成功", null));
            disconnect(null);
        }
    }
    
    /**
     * 处理发送消息请求
     * 
     * @param request 请求对象
     */
    private void handleSendMessage(ChatRequest request) {
        Message message = (Message) request.getData();
        logger.info("处理发送消息请求: 发送者={}, 接收者={}, 内容={}",
                message.getSender() != null ? message.getSender().getUsername() : "未知",
                message.getReceiverId(),
                message.getContent());
        
        boolean messageSent = false;
        
        if (message.isGroupMessage()) {
            // 群聊消息
            logger.info("发送群组消息: groupId={}", message.getReceiverId());
            server.broadcastToGroup(message, message.getReceiverId());
            // 存储群组消息
            server.getMessageStoreService().storeGroupMessage(message.getReceiverId(), message);
            messageSent = true;
        } else {
            // 私聊消息
            // 查找接收者处理器
            ClientHandler receiverHandler = server.getOnlineUsers().get(message.getReceiverId());
            
            if (receiverHandler != null) {
                logger.info("向接收者发送消息: {}", message.getReceiverId());
                receiverHandler.sendMessage(message);
                // 存储私聊消息
                server.getMessageStoreService().storePrivateMessage(
                    message.getSender().getUserId(),
                    message.getReceiverId(),
                    message
                );
                messageSent = true;
            } else {
                logger.warn("接收者不在线: {}", message.getReceiverId());
            }
        }
        
        // 发送响应
        if (messageSent) {
            sendResponse(ChatResponse.createSuccessResponse(request, "消息发送成功", null));
        } else {
            sendResponse(ChatResponse.createErrorResponse(request, "消息发送失败，接收者可能不在线", null));
        }
    }
    
    /**
     * 处理加入群组请求
     * 
     * @param request 请求对象
     */
    private void handleJoinGroup(ChatRequest request) {
        String groupId = (String) request.getData();
        ChatGroup group = server.getChatGroups().get(groupId);
        
        if (group != null && user != null) {
            logger.info("用户 {} (ID: {}) 尝试加入群组 {} (ID: {})", 
                user.getUsername(), 
                user.getUserId(), 
                group.getGroupName(), 
                groupId);
                
            logger.info("加入前群组成员列表: {}", group.getMemberIds());
            
            boolean added = group.addMember(user.getUserId());
            if (!added) {
                logger.error("用户 {} 加入群组 {} 失败", user.getUsername(), group.getGroupName());
                sendResponse(ChatResponse.createErrorResponse(request, "加入群组失败", null));
                return;
            }
            
            logger.info("加入后群组成员列表: {}", group.getMemberIds());
            
            // 保存群组数据
            server.getGroupStoreService().saveGroup(group);
            
            // 通知群组内其他成员
            String notificationContent = user.getUsername() + " 加入了群组 " + group.getGroupName();
            Message notification = Message.createSystemMessage(notificationContent, groupId, true);
            server.broadcastToGroup(notification, groupId);
            
            // 发送控制消息通知用户状态变更（非重要变更）
            sendUserStatusControlMessage(user.getUsername() + " 加入群组: " + group.getGroupName(), false);
            
            // 通知所有在线用户刷新群组列表
            server.broadcastMessage(Message.createSystemMessage("[REFRESH_GROUPS]", null, false));
            
            sendResponse(ChatResponse.createSuccessResponse(request, "加入群组成功", group));
        } else {
            logger.error("加入群组失败: 群组或用户不存在");
            sendResponse(ChatResponse.createErrorResponse(request, "群组不存在或用户未登录", null));
        }
    }
    
    /**
     * 处理离开群组请求
     * 
     * @param request 请求对象
     */
    private void handleLeaveGroup(ChatRequest request) {
        String groupId = (String) request.getData();
        ChatGroup group = server.getChatGroups().get(groupId);
        
        if (group != null && user != null) {
            group.removeMember(user.getUserId());
            
            // 通知群组内其他成员
            String notificationContent = user.getUsername() + " 离开了群组 " + group.getGroupName();
            Message notification = Message.createSystemMessage(notificationContent, groupId, true);
            server.broadcastToGroup(notification, groupId);
            
            // 发送控制消息通知用户状态变更（非重要变更）
            sendUserStatusControlMessage(user.getUsername() + " 离开群组: " + group.getGroupName(), false);
            
            // 通知所有在线用户刷新群组列表
            server.broadcastMessage(Message.createSystemMessage("[REFRESH_GROUPS]", null, false));
            
            sendResponse(ChatResponse.createSuccessResponse(request, "离开群组成功", null));
        } else {
            sendResponse(ChatResponse.createErrorResponse(request, "群组不存在或用户未登录", null));
        }
    }
    
    /**
     * 处理创建群组请求
     * 
     * @param request 请求对象
     */
    private void handleCreateGroup(ChatRequest request) {
        String groupName = (String) request.getData();
        
        if (user != null) {
            ChatGroup group = ChatGroup.createGroup(groupName, user);
            group.addMember(user.getUserId());
            
            server.getChatGroups().put(group.getGroupId(), group);
            server.getGroupStoreService().saveGroup(group);
            
            // 发送控制消息通知用户状态变更（非重要变更）
            sendUserStatusControlMessage("新群组创建: " + groupName + " (创建者: " + user.getUsername() + ")", false);
            
            // 通知所有在线用户刷新群组列表
            server.broadcastMessage(Message.createSystemMessage("[REFRESH_GROUPS]", null, false));
            
            sendResponse(ChatResponse.createSuccessResponse(request, "创建群组成功", group));
        } else {
            sendResponse(ChatResponse.createErrorResponse(request, "用户未登录", null));
        }
    }
    
    /**
     * 处理获取用户列表请求
     * 
     * @param request 请求对象
     */
    private void handleGetUsers(ChatRequest request) {
        // 获取所有在线用户
        User[] users = server.getOnlineUsers().values().stream()
                .map(ClientHandler::getUser)
                .toArray(User[]::new);
        
        sendResponse(ChatResponse.createSuccessResponse(request, "获取用户列表成功", users));
    }
    
    /**
     * 处理获取群组列表请求
     * 
     * @param request 请求对象
     */
    private void handleGetGroups(ChatRequest request) {
        if (user == null) {
            logger.warn("获取群组列表失败：用户未登录");
            sendResponse(ChatResponse.createErrorResponse(request, "用户未登录", null));
            return;
        }

        logger.info("开始获取用户 {} (ID: {}) 的群组列表", user.getUsername(), user.getUserId());
        
        // 获取用户加入的群组列表
        List<ChatGroup> userGroups = new ArrayList<>();
        Map<String, ChatGroup> allGroups = server.getChatGroups();
        logger.info("服务器共有 {} 个群组", allGroups.size());
        
        for (ChatGroup group : allGroups.values()) {
            logger.info("检查群组: {} (ID: {})", group.getGroupName(), group.getGroupId());
            logger.info("群组成员列表: {}", group.getMemberIds());
            logger.info("当前用户ID: {}", user.getUserId());
            logger.info("用户是否在群组中: {}", group.isMember(user.getUserId()));
            
            if (group.isMember(user.getUserId())) {
                logger.info("用户 {} 是群组 {} 的成员", user.getUsername(), group.getGroupName());
                userGroups.add(group);
            }
        }
        
        logger.info("用户 {} 加入了 {} 个群组", user.getUsername(), userGroups.size());
        if (userGroups.isEmpty()) {
            logger.warn("用户 {} 没有加入任何群组", user.getUsername());
        } else {
            logger.info("用户加入的群组列表: {}", userGroups.stream()
                .map(g -> g.getGroupName() + "(" + g.getGroupId() + ")")
                .collect(Collectors.joining(", ")));
        }
        
        sendResponse(ChatResponse.createSuccessResponse(
                request,
                "获取群组列表成功",
                userGroups.toArray()
        ));
    }
    
    /**
     * 处理心跳请求
     * 
     * @param request 请求对象
     */
    private void handleHeartbeat(ChatRequest request) {
        if (user != null) {
            user.updateActiveTime();
        }
        sendResponse(ChatResponse.createSuccessResponse(request, "心跳成功", null));
    }
    
    /**
     * 处理邀请加入群组请求
     * 
     * @param request 请求对象
     */
    private void handleInviteToGroup(ChatRequest request) {
        if (user == null) {
            sendResponse(ChatResponse.createErrorResponse(request, "用户未登录", null));
            return;
        }

        Object[] data = (Object[]) request.getData();
        if (data == null || data.length != 2) {
            sendResponse(ChatResponse.createErrorResponse(request, "无效的邀请数据", null));
            return;
        }

        String groupId = (String) data[0];
        String inviteeId = (String) data[1];

        ChatGroup group = server.getChatGroups().get(groupId);
        if (group == null) {
            sendResponse(ChatResponse.createErrorResponse(request, "群组不存在", null));
            return;
        }

        // 检查邀请者是否是群组成员
        if (!group.isMember(user.getUserId())) {
            sendResponse(ChatResponse.createErrorResponse(request, "您不是该群组成员", null));
            return;
        }

        // 检查被邀请者是否已经是群组成员
        if (group.isMember(inviteeId)) {
            sendResponse(ChatResponse.createErrorResponse(request, "该用户已经是群组成员", null));
            return;
        }

        // 获取被邀请者的ClientHandler
        ClientHandler inviteeHandler = server.getClientHandler(inviteeId);
        if (inviteeHandler == null) {
            sendResponse(ChatResponse.createErrorResponse(request, "被邀请的用户不在线", null));
            return;
        }

        // 发送邀请消息给被邀请者
        String inviteContent = "[GROUP_INVITE]" + groupId + "|" + group.getGroupName();
        Message inviteMessage = Message.createSystemMessage(inviteContent, null, false);
        inviteMessage.setSender(user);

        inviteeHandler.sendMessage(inviteMessage);
        sendResponse(ChatResponse.createSuccessResponse(request, "邀请已发送", null));
    }
    
    /**
     * 处理解散群组请求
     * 
     * @param request 请求对象
     */
    private void handleDismissGroup(ChatRequest request) {
        String groupId = (String) request.getData();
        ChatGroup group = server.getChatGroups().get(groupId);
        
        if (group != null && user != null) {
            // 检查是否是群主
            if (group.getCreatorId().equals(user.getUserId())) {
                // 通知群组内所有成员
                String notificationContent = "群组 " + group.getGroupName() + " 已被群主解散";
                Message notification = Message.createSystemMessage(notificationContent, groupId, true);
                server.broadcastToGroup(notification, groupId);
                
                // 从服务器移除群组
                server.getChatGroups().remove(groupId);
                
                // 从持久化存储中删除群组
                server.getGroupStoreService().deleteGroup(groupId);
                
                // 发送控制消息通知用户状态变更（重要变更）
                sendUserStatusControlMessage("[STATUS_CHANGE]群组 " + group.getGroupName() + " 已被解散", true);
                
                // 通知所有在线用户刷新群组列表
                server.broadcastMessage(Message.createSystemMessage("[REFRESH_GROUPS]", null, false));
                
                sendResponse(ChatResponse.createSuccessResponse(request, "解散群组成功", null));
            } else {
                sendResponse(ChatResponse.createErrorResponse(request, "只有群主才能解散群组", null));
            }
        } else {
            sendResponse(ChatResponse.createErrorResponse(request, "群组不存在或用户未登录", null));
        }
    }
    
    /**
     * 向客户端发送响应
     * 
     * @param response 响应对象
     */
    public void sendResponse(ChatResponse response) {
        try {
            logger.debug("发送响应: 类型={}, 成功={}, 消息={}", 
                    response.getType(), response.isSuccess(), response.getMessage());
            
            outputStream.writeObject(response);
            outputStream.flush();
            
            logger.debug("响应已发送");
        } catch (IOException e) {
            logger.error("发送响应时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 向客户端发送消息
     * 
     * @param message 消息对象
     */
    public void sendMessage(Message message) {
        if (message == null) return;
        
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            logger.error("发送消息时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 断开连接
     * 
     * @param reason 断开原因
     */
    public void disconnect(String reason) {
        running = false;
        
        if (reason != null) {
            try {
                // 发送断开连接原因
                Message systemMessage = Message.createSystemMessage(reason, null, false);
                sendMessage(systemMessage);
            } catch (Exception e) {
                logger.error("发送断开原因时出错: {}", e.getMessage());
            }
        }
        
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            
            logger.info("客户端连接已关闭: {}", clientSocket.getRemoteSocketAddress());
        } catch (IOException e) {
            logger.error("关闭客户端连接时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 获取用户
     * 
     * @return 用户对象
     */
    public User getUser() {
        return user;
    }
    
    /**
     * 发送用户状态控制消息
     * 
     * @param content 消息内容
     * @param isImportant 是否是重要状态变更
     */
    private void sendUserStatusControlMessage(String content, boolean isImportant) {
        Message statusMessage = new Message();
        statusMessage.setMessageId(java.util.UUID.randomUUID().toString());
        statusMessage.setType(MessageType.CONTROL);
        
        // 为重要的状态变更添加特殊标记
        if (isImportant) {
            content = "[STATUS_CHANGE]" + content;
        }
        
        statusMessage.setContent(content);
        statusMessage.setTimestamp(new java.util.Date());
        
        logger.info("发送用户状态控制消息: {}", content);
        server.broadcastMessage(statusMessage);
    }
} 