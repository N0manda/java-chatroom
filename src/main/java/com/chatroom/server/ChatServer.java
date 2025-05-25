package com.chatroom.server;

import com.chatroom.common.model.ChatGroup;
import com.chatroom.common.model.Message;
import com.chatroom.common.model.MessageType;
import com.chatroom.common.model.User;
import com.chatroom.common.network.ChatRequest;
import com.chatroom.common.network.ChatResponse;
import com.chatroom.common.network.RequestType;
import com.chatroom.common.network.ResponseType;
import com.chatroom.server.config.UserCredentials;
import com.chatroom.server.handler.ClientHandler;
import com.chatroom.server.service.MessageStoreService;
import com.chatroom.server.service.GroupStoreService;
import com.chatroom.server.ui.ServerConfigFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聊天服务器主类
 */
public class ChatServer {
    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);
    
    // 服务器名称
    private static final String SERVER_NAME = "聊天室服务器";
    
    /**
     * 服务器绑定地址
     */
    private final String bindAddress;
    
    /**
     * 服务器端口
     */
    private final int port;
    
    /**
     * 服务器Socket
     */
    private ServerSocket serverSocket;
    
    /**
     * 线程池
     */
    private final ExecutorService executorService;
    
    /**
     * 在线用户映射 (userId -> ClientHandler)
     */
    private final Map<String, ClientHandler> onlineUsers;
    
    /**
     * 聊天群组映射 (groupId -> ChatGroup)
     */
    private final Map<String, ChatGroup> chatGroups;
    
    /**
     * 消息存储服务
     */
    private final MessageStoreService messageStoreService;
    
    /**
     * 群组存储服务
     */
    private final GroupStoreService groupStoreService;
    
    /**
     * 构造方法 - 只指定端口
     * 
     * @param port 服务器端口
     */
    public ChatServer(int port) {
        this("0.0.0.0", port);
    }
    
    /**
     * 构造方法 - 指定绑定地址和端口
     * 
     * @param bindAddress 绑定地址
     * @param port 服务器端口
     */
    public ChatServer(String bindAddress, int port) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.executorService = Executors.newCachedThreadPool();
        this.onlineUsers = new ConcurrentHashMap<>();
        this.chatGroups = new ConcurrentHashMap<>();
        this.messageStoreService = MessageStoreService.getInstance();
        this.groupStoreService = GroupStoreService.getInstance();
        
        logger.info("开始初始化服务器...");
        logger.info("从存储中加载群组数据...");
        
        // 从存储中加载群组
        List<ChatGroup> loadedGroups = groupStoreService.getAllGroups();
        logger.info("从存储中加载到 {} 个群组", loadedGroups.size());
        
        for (ChatGroup group : loadedGroups) {
            logger.info("正在加载群组: {}", group.getGroupName());
            logger.info("  群组ID: {}", group.getGroupId());
            logger.info("  创建者ID: {}", group.getCreatorId());
            logger.info("  成员数量: {}", group.getMemberIds().size());
            logger.info("  成员列表: {}", group.getMemberIds());
            
            chatGroups.put(group.getGroupId(), group);
            logger.info("群组 {} 加载完成", group.getGroupName());
        }
        
        // 创建公共聊天室（如果不存在）
        if (!chatGroups.containsKey("public")) {
            logger.info("创建公共聊天室...");
            ChatGroup publicChatRoom = new ChatGroup();
            publicChatRoom.setGroupId("public");
            publicChatRoom.setGroupName("公共聊天室");
            chatGroups.put(publicChatRoom.getGroupId(), publicChatRoom);
            groupStoreService.saveGroup(publicChatRoom);
            logger.info("公共聊天室创建完成");
        }
        
        logger.info("服务器初始化完成，当前共有 {} 个群组", chatGroups.size());
    }
    
    /**
     * 获取公共聊天室
     * 
     * @return 公共聊天室对象
     */
    public ChatGroup getPublicChatRoom() {
        return chatGroups.get("public");
    }
    
    /**
     * 启动服务器
     */
    public void start() {
        try {
            // 创建服务器Socket，绑定到指定地址和端口
            InetAddress bindAddr = bindAddress.equals("0.0.0.0") ? 
                    null : InetAddress.getByName(bindAddress);
            serverSocket = new ServerSocket(port, 50, bindAddr);
            
            logger.info("聊天服务器已启动，监听地址: {}:{}", bindAddress, port);
            
            // 开始接受客户端连接
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("新客户端连接: {}", clientSocket.getRemoteSocketAddress());
                    
                    // 为每个客户端创建一个处理器
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    executorService.execute(clientHandler);
                } catch (IOException e) {
                    logger.error("接受客户端连接时出错: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("服务器启动失败: {}", e.getMessage());
        } finally {
            shutdown();
        }
    }
    
    /**
     * 关闭服务器
     */
    public void shutdown() {
        logger.info("关闭聊天服务器...");
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("关闭服务器Socket时出错: {}", e.getMessage());
            }
        }
        
        executorService.shutdown();
        logger.info("聊天服务器已关闭");
    }
    
    /**
     * 广播消息给所有在线用户
     * 
     * @param message 消息对象
     */
    public void broadcastMessage(Message message) {
        for (ClientHandler handler : onlineUsers.values()) {
            handler.sendMessage(message);
        }
    }
    
    /**
     * 广播消息给指定群组的所有成员
     * 
     * @param message 消息对象
     * @param groupId 群组ID
     */
    public void broadcastToGroup(Message message, String groupId) {
        ChatGroup group = chatGroups.get(groupId);
        if (group != null) {
            for (String userId : group.getMemberIds()) {
                ClientHandler handler = onlineUsers.get(userId);
                if (handler != null) {
                    handler.sendMessage(message);
                }
            }
        }
    }
    
    /**
     * 发送私聊消息
     * 
     * @param message 消息对象
     * @param receiverId 接收者ID
     */
    public void sendPrivateMessage(Message message, String receiverId) {
        ClientHandler handler = onlineUsers.get(receiverId);
        if (handler != null) {
            handler.sendMessage(message);
        }
    }
    
    /**
     * 广播用户状态变更消息
     * 
     * @param content 消息内容
     * @param isImportant 是否是重要状态变更（登录/注销）
     */
    private void broadcastUserStatusChange(String content, boolean isImportant) {
        Message statusMessage = new Message();
        statusMessage.setMessageId(java.util.UUID.randomUUID().toString());
        statusMessage.setType(MessageType.CONTROL);
        
        // 为重要的状态变更（登录/注销）添加特殊标记
        if (isImportant) {
            content = "[STATUS_CHANGE]" + content;
        }
        
        statusMessage.setContent(content);
        statusMessage.setTimestamp(new java.util.Date());
        
        logger.info("广播用户状态变更: {}", content);
        broadcastMessage(statusMessage);
    }
    
    /**
     * 处理用户登录
     * 
     * @param user 用户对象
     * @param handler 客户端处理器
     * @return 登录结果响应
     */
    public synchronized ChatResponse handleLogin(User user, ClientHandler handler) {
        // 获取用户凭据管理器
        UserCredentials credentials = UserCredentials.getInstance();
        
        // 验证用户凭据
        String username = user.getUsername();
        String password = user.getPasswordHash().isEmpty() ? "" : user.getPasswordHash(); // 获取密码
        
        logger.info("用户 {} 尝试登录", username);
        
        // 检查用户名是否已经存在
        for (ClientHandler existingHandler : onlineUsers.values()) {
            User existingUser = existingHandler.getUser();
            if (existingUser != null && existingUser.getUsername().equals(username)) {
                // 用户已在线，踢掉旧连接
                logger.info("用户 {} 已在其他位置登录，踢掉旧连接", username);
                existingHandler.disconnect("您的账号在其他地方登录，此连接已断开");
                onlineUsers.remove(existingUser.getUserId());
                break;
            }
        }
        
        // 检查用户是否存在且凭据是否正确
        boolean userExists = credentials.userExists(username);
        boolean credentialsValid = userExists && credentials.validateCredentials(username, password);
        
        // 验证失败
        if (!credentialsValid) {
            logger.warn("用户 {} 登录失败：凭据无效", username);
            return ChatResponse.createErrorResponse(
                    ResponseType.LOGIN_RESULT, 
                    "用户名或密码错误", 
                    null);
        }
        
        // 登录成功
        onlineUsers.put(user.getUserId(), handler);
        logger.info("用户 {} 登录成功，用户ID: {}", username, user.getUserId());
        
        // 广播用户登录消息
        broadcastUserStatusChange(username + " 已登录", true);
        
        // 获取公共聊天室
        ChatGroup publicChatRoom = getPublicChatRoom();
        
        // 将用户添加到公共聊天室
        if (publicChatRoom != null) {
            publicChatRoom.addMember(user.getUserId());
            logger.info("已将用户 {} 添加到公共聊天室", username);
        }
        
        // 恢复用户在其他群组中的成员身份
        logger.info("开始恢复用户 {} 的群组成员身份", username);
        List<ChatGroup> userGroups = new ArrayList<>();
        for (ChatGroup group : chatGroups.values()) {
            if (!group.getGroupId().equals("public")) {
                logger.info("检查群组: {} (ID: {})", group.getGroupName(), group.getGroupId());
                logger.info("群组成员列表: {}", group.getMemberIds());
                logger.info("用户ID: {}", user.getUserId());
                
                if (group.isMember(user.getUserId())) {
                    logger.info("用户 {} 是群组 {} 的成员，正在恢复成员身份", username, group.getGroupName());
                    // 确保群组成员信息被保存
                    groupStoreService.saveGroup(group);
                    userGroups.add(group);
                    logger.info("群组 {} 的成员信息已保存", group.getGroupName());
                } else {
                    logger.info("用户 {} 不是群组 {} 的成员", username, group.getGroupName());
                }
            }
        }
        
        // 返回成功响应和公共聊天室信息
        logger.info("用户 {} 登录处理完成，准备返回响应", username);
        return new ChatResponse(
                null,
                ResponseType.LOGIN_RESULT,
                true,
                "登录成功",
                new Object[]{user, publicChatRoom, userGroups.toArray()}
        );
    }
    
    /**
     * 处理用户注销
     * 
     * @param userId 用户ID
     */
    public synchronized void handleLogout(String userId) {
        ClientHandler handler = onlineUsers.remove(userId);
        if (handler != null) {
            User user = handler.getUser();
            if (user != null) {
                // 通知其他用户此用户已离开
                String notificationContent = user.getUsername() + " 已离开聊天室";
                Message notification = Message.createSystemMessage(notificationContent, null, false);
                broadcastMessage(notification);
                
                // 发送用户状态变更控制消息（标记为重要状态变更）
                broadcastUserStatusChange("用户离线: " + user.getUsername(), true);
                
                // 通知所有在线用户刷新群组列表
                broadcastMessage(Message.createSystemMessage("[REFRESH_GROUPS]", null, false));
                
                logger.info("用户 {} 已离线，但保留群组成员身份", user.getUsername());
            }
        }
    }
    
    /**
     * 获取在线用户列表
     * 
     * @return 在线用户列表响应
     */
    public ChatResponse getUserList() {
        return ChatResponse.createSuccessResponse(null, "获取用户列表成功", getUsersFromHandlers());
    }
    
    /**
     * 从处理器中获取所有用户
     * 
     * @return 用户列表
     */
    private User[] getUsersFromHandlers() {
        return onlineUsers.values().stream()
                .map(ClientHandler::getUser)
                .toArray(User[]::new);
    }
    
    /**
     * 获取在线用户Map
     * 
     * @return 在线用户Map
     */
    public Map<String, ClientHandler> getOnlineUsers() {
        return onlineUsers;
    }
    
    /**
     * 获取聊天群组Map
     * 
     * @return 聊天群组Map
     */
    public Map<String, ChatGroup> getChatGroups() {
        return chatGroups;
    }
    
    /**
     * 获取绑定地址
     * 
     * @return 绑定地址
     */
    public String getBindAddress() {
        return bindAddress;
    }
    
    /**
     * 获取端口
     * 
     * @return 端口
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 处理获取历史消息请求
     */
    private void handleGetHistoryMessages(ChatRequest request, ClientHandler handler) {
        logger.info("处理获取历史消息请求: sender={}", request.getSender().getUsername());
        
        // 检查参数
        String targetId = (String) request.getData();
        if (targetId == null) {
            logger.error("获取历史消息失败：目标ID为空");
            handler.sendResponse(ChatResponse.createErrorResponse(request, "目标ID不能为空"));
            return;
        }
        
        try {
            List<Message> messages;
            // 判断是群聊还是私聊
            if (targetId.equals("public") || targetId.equals("public_chat_room")) {
                logger.info("获取群聊历史消息: groupId={}", targetId);
                messages = messageStoreService.getGroupMessages(targetId, 50); // 获取最近50条消息
            } else {
                logger.info("获取私聊历史消息: targetId={}", targetId);
                // 使用当前用户的ID和targetId获取消息
                messages = messageStoreService.getPrivateMessages(
                    handler.getUser().getUserId(),  // 使用当前用户的ID
                    targetId,                       // 使用目标用户的ID
                    50                             // 获取最近50条消息
                );
            }
            
            logger.info("获取到历史消息: {} 条", messages.size());
            
            // 发送响应
            ChatResponse response = new ChatResponse(
                request.getRequestId(),
                ResponseType.HISTORY_MESSAGES,  // 使用正确的响应类型
                true,
                "获取历史消息成功",
                messages
            );
            handler.sendResponse(response);
            logger.debug("历史消息响应已发送");
            
        } catch (Exception e) {
            logger.error("获取历史消息时发生错误", e);
            handler.sendResponse(ChatResponse.createErrorResponse(request, "获取历史消息失败：" + e.getMessage()));
        }
    }
    
    /**
     * 处理客户端请求
     */
    public void handleRequest(ChatRequest request, ClientHandler handler) {
        switch (request.getType()) {
            case LOGIN:
                handleLogin(request.getSender(), handler);
                break;
            case LOGOUT:
                handleLogout(request.getSender().getUserId());
                break;
            case GET_USERS:
                handler.sendResponse(ChatResponse.createSuccessResponse(request, "获取用户列表成功", getUsersFromHandlers()));
                break;
            case GET_HISTORY_MESSAGES:
                handleGetHistoryMessages(request, handler);
                break;
            default:
                logger.warn("未知的请求类型: {}", request.getType());
                break;
        }
    }
    
    /**
     * 获取消息存储服务
     * 
     * @return 消息存储服务实例
     */
    public MessageStoreService getMessageStoreService() {
        return messageStoreService;
    }
    
    /**
     * 获取群组存储服务
     */
    public GroupStoreService getGroupStoreService() {
        return groupStoreService;
    }
    
    /**
     * 获取用户的ClientHandler
     * 
     * @param userId 用户ID
     * @return ClientHandler对象，如果用户不在线则返回null
     */
    public ClientHandler getClientHandler(String userId) {
        return onlineUsers.get(userId);
    }
    
    /**
     * 程序入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 如果有命令行参数，则使用命令行启动模式
        if (args.length > 0) {
            String bindAddress = "0.0.0.0";
            int port = 9999;
            
            // 解析参数
            for (int i = 0; i < args.length; i++) {
                if ("-h".equals(args[i]) || "--host".equals(args[i])) {
                    if (i + 1 < args.length) {
                        bindAddress = args[++i];
                    }
                } else if ("-p".equals(args[i]) || "--port".equals(args[i])) {
                    if (i + 1 < args.length) {
                        try {
                            port = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            logger.warn("无效的端口参数: {}，使用默认端口: {}", args[i], port);
                        }
                    }
                }
            }
            
            // 创建并启动服务器
            ChatServer server = new ChatServer(bindAddress, port);
            server.start();
        } else {
            // 没有命令行参数，启动图形配置界面
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    logger.error("设置界面外观失败", e);
                }
                new ServerConfigFrame().setVisible(true);
            });
        }
    }
} 