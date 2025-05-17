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
import com.chatroom.server.ui.ServerConfigFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
        
        // 检查用户名是否已经存在
        for (ClientHandler existingHandler : onlineUsers.values()) {
            User existingUser = existingHandler.getUser();
            if (existingUser != null && existingUser.getUsername().equals(username)) {
                // 用户已在线（此处逻辑保留，已在ClientHandler中实现更全面的检查）
                return ChatResponse.createErrorResponse(
                        ResponseType.LOGIN_RESULT, 
                        "用户名已存在", 
                        null);
            }
        }
        
        // 检查用户是否在凭据文件中，如果不在则可以自动注册（向下兼容）
        // 或者检查用户凭据是否正确
        boolean userExists = credentials.userExists(username);
        boolean credentialsValid = userExists && credentials.validateCredentials(username, password);
        
        // 如果用户不存在且系统允许自动注册，则添加用户
        if (!userExists) {
            // 添加新用户到凭据文件
            credentials.addUser(username, password);
            credentials.saveCredentials();
            logger.info("已添加新用户: {}", username);
            credentialsValid = true;
        }
        
        // 验证失败
        if (!credentialsValid) {
            return ChatResponse.createErrorResponse(
                    ResponseType.LOGIN_RESULT, 
                    "用户名或密码错误", 
                    null);
        }
        
        // 登录成功
        onlineUsers.put(user.getUserId(), handler);
        
        // 广播用户登录消息
        broadcastUserStatusChange(username + " 已登录", true);
        
        // 获取公共聊天室
        ChatGroup publicChatRoom = getPublicChatRoom();
        
        // 将用户添加到公共聊天室
        if (publicChatRoom != null) {
            publicChatRoom.addMember(user.getUserId());
            logger.info("已将用户 {} 添加到公共聊天室", username);
        }
        
        // 返回成功响应和公共聊天室信息
        return ChatResponse.createSuccessResponse(
                null,
                "登录成功",
                new Object[]{user, publicChatRoom}
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
                
                // 从所有群组中移除该用户
                for (ChatGroup group : chatGroups.values()) {
                    if (group.getMemberIds().contains(userId)) {
                        group.removeMember(userId);
                        logger.info("用户 {} 已从群组 {} 中移除", user.getUsername(), group.getGroupName());
                    }
                }
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