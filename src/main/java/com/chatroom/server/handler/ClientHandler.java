package com.chatroom.server.handler;

import com.chatroom.common.model.Message;
import com.chatroom.common.model.User;
import com.chatroom.common.network.ChatRequest;
import com.chatroom.common.network.ChatResponse;
import com.chatroom.common.network.RequestType;
import com.chatroom.common.network.ResponseType;
import com.chatroom.server.ChatServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

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
                case CREATE_GROUP:
                    handleCreateGroup(request);
                    break;
                case JOIN_GROUP:
                    handleJoinGroup(request);
                    break;
                case LEAVE_GROUP:
                    handleLeaveGroup(request);
                    break;
                case GET_USERS:
                    handleGetUsers(request);
                    break;
                case GET_GROUPS:
                    handleGetGroups(request);
                    break;
                case HEARTBEAT:
                    handleHeartbeat(request);
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
        logger.info("处理登录请求: {}", request);
        
        try {
            String username = (String) request.getData();
            logger.info("用户尝试登录: {}", username);
            
            // 创建新用户
            user = User.createUser(username);
            logger.info("已创建用户对象: {}", user);
            
            // 处理登录逻辑
            logger.info("调用服务器处理登录逻辑");
            ChatResponse response = server.handleLogin(user, this);
            logger.info("登录响应: success={}, message={}", response.isSuccess(), response.getMessage());
            
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
            messageSent = true;
        } else {
            // 私聊消息
            // 查找接收者处理器
            ClientHandler receiverHandler = server.getOnlineUsers().get(message.getReceiverId());
            
            if (receiverHandler != null) {
                logger.info("向接收者发送消息: {}", message.getReceiverId());
                receiverHandler.sendMessage(message);
                messageSent = true;
            } else {
                logger.warn("接收者不在线: {}", message.getReceiverId());
            }
            
            // 如果发送者和接收者不是同一人，则回显给发送者
            if (!message.getSender().getUserId().equals(message.getReceiverId())) {
                logger.info("回显消息给发送者");
                sendMessage(message);
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
     * 处理创建群组请求
     * 
     * @param request 请求对象
     */
    private void handleCreateGroup(ChatRequest request) {
        // 暂不实现
    }
    
    /**
     * 处理加入群组请求
     * 
     * @param request 请求对象
     */
    private void handleJoinGroup(ChatRequest request) {
        // 暂不实现
    }
    
    /**
     * 处理离开群组请求
     * 
     * @param request 请求对象
     */
    private void handleLeaveGroup(ChatRequest request) {
        // 暂不实现
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
        // 获取所有群组
        sendResponse(ChatResponse.createSuccessResponse(
                request,
                "获取群组列表成功",
                server.getChatGroups().values().toArray()
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
} 