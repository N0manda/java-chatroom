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
import com.chatroom.server.ui.ServerConfigFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
     * 默认历史消息加载条数
     */
    private static final int DEFAULT_HISTORY_MESSAGE_LIMIT = 50;
    
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
        
        // 创建默认的公共聊天室
        createPublicChatRoom();
    }
    
    /**
     * 创建公共聊天室
     */
    private void createPublicChatRoom() {
        // 创建系统用户作为群主
        User systemUser = User.createUser("系统");
        
        // 创建默认群组
        ChatGroup publicGroup = ChatGroup.createGroup("公共聊天室", systemUser);
        
        // 将群组ID设为固定值，便于后续识别
        publicGroup.setGroupId("public_chat_room");
        
        // 添加到群组映射
        chatGroups.put(publicGroup.getGroupId(), publicGroup);
        
        logger.info("已创建公共聊天室: {}", publicGroup.getGroupName());
    }
    
    /**
     * 获取公共聊天室
     * 
     * @return 公共聊天室对象
     */
    public ChatGroup getPublicChatRoom() {
        return chatGroups.get("public_chat_room");
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
        // 保存消息到历史记录
        if (message.getType() != MessageType.CONTROL) {
            messageStoreService.storeGroupMessage(groupId, message);
        }
        
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
        // 保存消息到历史记录
        if (message.getType() != MessageType.CONTROL && message.getSender() != null) {
            messageStoreService.storePrivateMessage(message.getSender().getUserId(), receiverId, message);
        }
        
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
        
        // 检查用户名是否已经存在
        for (ClientHandler existingHandler : onlineUsers.values()) {
            User existingUser = existingHandler.getUser();
            if (existingUser != null && existingUser.getUsername().equals(username)) {
                logger.warn("用户 {} 登录失败: 该用户名已在线", username);
                return ChatResponse.createErrorResponse(
                        ResponseType.LOGIN_RESULT,
                        "登录失败：该用户名已经在线，请使用其他用户名",
                        null
                );
            }
        }
        
        // 这里简单处理，实际应用中应该做更复杂的验证
        if (username != null && !username.trim().isEmpty()) {
            // 生成随机用户ID
            String userId = java.util.UUID.randomUUID().toString();
            
            // 设置用户属性
            user.setUserId(userId);
            
            // 将用户添加到在线用户列表
            onlineUsers.put(userId, handler);
            
            // 将用户添加到公共聊天室
            ChatGroup publicChatRoom = getPublicChatRoom();
            if (publicChatRoom != null) {
                publicChatRoom.addMember(userId);
                logger.info("用户 {} (ID: {}) 加入公共聊天室", username, userId);
            }
            
            logger.info("用户 {} (ID: {}) 登录成功", username, userId);
            
            // 广播用户登录消息
            broadcastUserStatusChange(username + " 已登录", true);
            
            // 发送欢迎消息
            Message welcomeMessage = Message.createSystemMessage(
                    "欢迎 " + username + " 加入聊天室！",
                    publicChatRoom.getGroupId(),
                    true
            );
            broadcastToGroup(welcomeMessage, publicChatRoom.getGroupId());
            
            // 发送成功响应
            return ChatResponse.createSuccessResponse(
                    null,
                    "登录成功",
                    new Object[]{user, getPublicChatRoom()}
            );
        } else {
            logger.warn("用户登录失败: 无效的用户名");
            return ChatResponse.createErrorResponse(
                    ResponseType.LOGIN_RESULT,
                    "登录失败：无效的用户名",
                    null
            );
        }
    }
    
    /**
     * 处理用户注销
     * 
     * @param userId 用户ID
     */
    public synchronized void handleLogout(String userId) {
        ClientHandler handler = onlineUsers.get(userId);
        if (handler != null) {
            User user = handler.getUser();
            if (user != null) {
                // 从所有群组中移除用户
                for (ChatGroup group : chatGroups.values()) {
                    group.removeMember(userId);
                }
                
                // 从在线用户列表中移除
                onlineUsers.remove(userId);
                
                logger.info("用户 {} (ID: {}) 已注销", user.getUsername(), userId);
                
                // 广播用户注销消息
                broadcastUserStatusChange(user.getUsername() + " 已离线", true);
            }
        }
    }
    
    /**
     * 获取在线用户列表
     * 
     * @return 用户列表响应
     */
    public ChatResponse getUserList() {
        User[] users = getUsersFromHandlers();
        return ChatResponse.createSuccessResponse(null, "获取用户列表成功", users);
    }
    
    /**
     * 从处理器中获取用户数组
     * 
     * @return 在线用户数组
     */
    private User[] getUsersFromHandlers() {
        return onlineUsers.values().stream()
                .map(ClientHandler::getUser)
                .filter(user -> user != null)
                .toArray(User[]::new);
    }
    
    /**
     * 获取在线用户映射
     * 
     * @return 在线用户映射
     */
    public Map<String, ClientHandler> getOnlineUsers() {
        return onlineUsers;
    }
    
    /**
     * 获取聊天群组映射
     * 
     * @return 聊天群组映射
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
     * 获取历史消息
     * 
     * @param request 请求对象
     * @return 响应对象
     */
    public ChatResponse getHistoryMessages(ChatRequest request) {
        if (request == null || request.getData() == null) {
            return ChatResponse.createErrorResponse(
                    ResponseType.MESSAGE_RESULT,
                    "获取历史消息失败：无效的请求参数",
                    null
            );
        }
        
        try {
            Map<String, Object> params = (Map<String, Object>) request.getData();
            String targetId = (String) params.get("targetId");
            boolean isGroup = (boolean) params.get("isGroup");
            int limit = params.containsKey("limit") ? (int) params.get("limit") : DEFAULT_HISTORY_MESSAGE_LIMIT;
            
            List<Message> historyMessages;
            if (isGroup) {
                // 获取群组历史消息
                historyMessages = messageStoreService.getGroupMessages(targetId, limit);
                logger.info("获取群组 {} 的历史消息, 共 {} 条", targetId, historyMessages.size());
            } else {
                // 获取私聊历史消息
                String userId = request.getSender() != null ? request.getSender().getUserId() : null;
                if (userId == null) {
                    return ChatResponse.createErrorResponse(
                            ResponseType.MESSAGE_RESULT,
                            "获取历史消息失败：无效的发送者",
                            null
                    );
                }
                historyMessages = messageStoreService.getPrivateMessages(userId, targetId, limit);
                logger.info("获取用户 {} 和 {} 之间的历史消息, 共 {} 条", userId, targetId, historyMessages.size());
            }
            
            return ChatResponse.createSuccessResponse(
                    request,
                    "获取历史消息成功",
                    historyMessages
            );
        } catch (Exception e) {
            logger.error("获取历史消息时出错: {}", e.getMessage());
            return ChatResponse.createErrorResponse(
                    ResponseType.MESSAGE_RESULT,
                    "获取历史消息失败：" + e.getMessage(),
                    null
            );
        }
    }
    
    /**
     * 主方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 显示服务器配置界面
        ServerConfigFrame.showServerConfigFrame();
    }
} 