package com.chatroom.common.network;

import java.io.Serializable;

/**
 * 服务器响应对象
 */
public class ChatResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 响应ID
     */
    private String responseId;
    
    /**
     * 关联的请求ID
     */
    private String requestId;
    
    /**
     * 响应类型
     */
    private ResponseType type;
    
    /**
     * 响应状态
     */
    private boolean success;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private Object data;
    
    /**
     * 响应时间戳
     */
    private long timestamp;
    
    /**
     * 构造方法
     * 
     * @param requestId 请求ID
     * @param type 响应类型
     * @param success 是否成功
     * @param message 消息
     * @param data 数据
     */
    public ChatResponse(String requestId, ResponseType type, boolean success, String message, Object data) {
        this.responseId = java.util.UUID.randomUUID().toString();
        this.requestId = requestId;
        this.type = type;
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 创建成功响应
     * 
     * @param request 请求对象
     * @param message 消息
     * @param data 数据
     * @return 响应对象
     */
    public static ChatResponse createSuccessResponse(ChatRequest request, String message, Object data) {
        return new ChatResponse(
                request != null ? request.getRequestId() : null,
                request != null ? mapRequestToResponseType(request.getType()) : ResponseType.GENERIC_RESULT,
                true,
                message,
                data
        );
    }
    
    /**
     * 创建错误响应
     * 
     * @param request 请求对象
     * @param message 错误消息
     * @return 响应对象
     */
    public static ChatResponse createErrorResponse(ChatRequest request, String message) {
        return new ChatResponse(
                request != null ? request.getRequestId() : null,
                request != null ? mapRequestToResponseType(request.getType()) : ResponseType.ERROR,
                false,
                message,
                null
        );
    }
    
    /**
     * 创建错误响应(允许指定数据)
     * 
     * @param request 请求对象
     * @param message 错误消息
     * @param data 错误相关数据
     * @return 响应对象
     */
    public static ChatResponse createErrorResponse(ChatRequest request, String message, Object data) {
        return new ChatResponse(
                request != null ? request.getRequestId() : null,
                request != null ? mapRequestToResponseType(request.getType()) : ResponseType.ERROR,
                false,
                message,
                data
        );
    }
    
    /**
     * 创建错误响应(允许指定响应类型)
     * 
     * @param responseType 响应类型
     * @param message 错误消息
     * @param data 错误相关数据
     * @return 响应对象
     */
    public static ChatResponse createErrorResponse(ResponseType responseType, String message, Object data) {
        return new ChatResponse(
                null,
                responseType != null ? responseType : ResponseType.ERROR,
                false,
                message,
                data
        );
    }
    
    /**
     * 映射请求类型到响应类型
     * 
     * @param requestType 请求类型
     * @return 对应的响应类型
     */
    private static ResponseType mapRequestToResponseType(RequestType requestType) {
        if (requestType == null) {
            return ResponseType.GENERIC_RESULT;
        }
        
        switch (requestType) {
            case LOGIN:
                return ResponseType.LOGIN_RESULT;
            case LOGOUT:
                return ResponseType.LOGOUT_RESULT;
            case SEND_MESSAGE:
                return ResponseType.MESSAGE_RESULT;
            case CREATE_GROUP:
            case JOIN_GROUP:
            case LEAVE_GROUP:
                return ResponseType.GROUP_RESULT;
            case GET_USERS:
                return ResponseType.USER_LIST;
            case GET_GROUPS:
                return ResponseType.GROUP_LIST;
            case TRANSFER_FILE:
                return ResponseType.FILE_RESULT;
            case VOICE_CALL:
                return ResponseType.VOICE_RESULT;
            default:
                return ResponseType.GENERIC_RESULT;
        }
    }
    
    /**
     * 获取响应ID
     * 
     * @return 响应ID
     */
    public String getResponseId() {
        return responseId;
    }
    
    /**
     * 获取请求ID
     * 
     * @return 请求ID
     */
    public String getRequestId() {
        return requestId;
    }
    
    /**
     * 获取响应类型
     * 
     * @return 响应类型
     */
    public ResponseType getType() {
        return type;
    }
    
    /**
     * 是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 获取消息
     * 
     * @return 消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取数据
     * 
     * @return 数据
     */
    public Object getData() {
        return data;
    }
    
    /**
     * 获取时间戳
     * 
     * @return 时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "ChatResponse{" +
                "responseId='" + responseId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", type=" + type +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 