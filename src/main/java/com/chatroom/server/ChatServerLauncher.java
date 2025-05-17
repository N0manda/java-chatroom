package com.chatroom.server;

import com.chatroom.server.config.ServerConfig;
import com.chatroom.server.config.UserCredentials;
import com.chatroom.server.ui.ServerConfigFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 聊天服务器启动器
 * 作为程序主入口，提供CLI和GUI两种启动方式
 */
public class ChatServerLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ChatServerLauncher.class);
    
    /**
     * 主函数
     * 根据命令行参数决定启动方式
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 解析参数
        LaunchParams params = parseArguments(args);
        
        // 根据参数选择启动方式
        if (params.useGui) {
            launchWithGui();
        } else {
            launchWithCli(params);
        }
    }
    
    /**
     * 使用图形界面启动
     */
    private static void launchWithGui() {
        logger.info("使用图形界面启动服务器");
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception e) {
                logger.error("设置界面外观失败: {}", e.getMessage());
            }
            
            new ServerConfigFrame().setVisible(true);
        });
    }
    
    /**
     * 使用命令行方式启动
     * 
     * @param params 启动参数
     */
    private static void launchWithCli(LaunchParams params) {
        logger.info("使用命令行方式启动服务器，绑定地址: {}，端口: {}", params.bindAddress, params.port);
        ChatServer server = startServer(params.bindAddress, params.port);
        
        // 显示启动信息
        System.out.println("=================================================");
        System.out.println("  聊天服务器启动");
        System.out.println("  绑定地址: " + params.bindAddress);
        System.out.println("  监听端口: " + params.port);
        System.out.println("=================================================");
    }
    
    /**
     * 启动服务器
     * 
     * @param bindAddress 绑定地址
     * @param port 端口
     * @return 服务器实例
     */
    public static ChatServer startServer(String bindAddress, int port) {
        try {
            // 初始化用户凭据管理器
            UserCredentials.getInstance();
            logger.info("已初始化用户凭据管理器");
            
            ChatServer server = new ChatServer(bindAddress, port);
            
            // 异步启动服务器
            CompletableFuture.runAsync(() -> {
                server.start();
            });
            
            logger.info("服务器已启动: {}:{}", bindAddress, port);
            return server;
        } catch (Exception e) {
            logger.error("启动服务器失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析命令行参数
     * 
     * @param args 命令行参数
     * @return 解析后的参数对象
     */
    private static LaunchParams parseArguments(String[] args) {
        LaunchParams params = new LaunchParams();
        
        // 默认使用图形界面
        if (args.length == 0) {
            params.useGui = true;
            return params;
        }
        
        // 遍历参数
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--host":
                    if (i + 1 < args.length) {
                        params.bindAddress = args[++i];
                    }
                    break;
                case "-p":
                case "--port":
                    if (i + 1 < args.length) {
                        try {
                            params.port = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            logger.warn("无效的端口参数: {}，使用默认端口: {}", args[i], params.port);
                        }
                    }
                    break;
                case "-g":
                case "--gui":
                    params.useGui = true;
                    break;
                case "--help":
                    printHelp();
                    System.exit(0);
                    break;
                default:
                    logger.warn("未知参数: {}", args[i]);
                    break;
            }
        }
        
        return params;
    }
    
    /**
     * 打印帮助信息
     */
    private static void printHelp() {
        System.out.println("聊天服务器启动器");
        System.out.println("用法: java -jar chatserver.jar [选项]");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  -h, --host <地址>       指定绑定地址 (默认: 0.0.0.0)");
        System.out.println("  -p, --port <端口>       指定监听端口 (默认: 9999)");
        System.out.println("  -g, --gui               使用图形界面启动 (默认模式)");
        System.out.println("  --help                  显示此帮助信息");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -jar chatserver.jar                  # 使用图形界面启动");
        System.out.println("  java -jar chatserver.jar -p 8888          # 使用端口8888启动");
        System.out.println("  java -jar chatserver.jar -h 192.168.1.100 # 绑定到特定IP启动");
    }
    
    /**
     * 启动参数类
     */
    private static class LaunchParams {
        String bindAddress = "0.0.0.0";
        int port = 9999;
        boolean useGui = false;
    }
} 