package cn.lbs.socket.base;


import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

@Slf4j
public class SocketClient {
    public static void main(String args[]) throws Exception {
        // 设置连接的服务端IP地址和端口
        final String HOST = "127.0.0.1";
        final int PORT = 8081;
        //注意指定编码格式，发送方和接收方一定要统一，建议使用UTF-8
        final String CHARSET="UTF-8";

        // 与服务端建立连接
        Socket socket = new Socket(HOST, PORT);
        // 建立连接后获得输出、输出流、连接信息
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream=socket.getInputStream();
        SocketAddress   localAddress=socket.getLocalSocketAddress();
        SocketAddress remoteAddress=socket.getRemoteSocketAddress();


        log.info("The client {} successfully connects to the server {}",localAddress,remoteAddress);

        String message="Hello World";
        log.info("The client {} sends a message '{}' to the server ",localAddress,message);
        outputStream.write(message.getBytes(CHARSET));
        socket.shutdownOutput();

        //建立缓冲区用于读取或写入
        byte[] bytes = new byte[1024];
        int len;
        StringBuilder sb = new StringBuilder();
        //获取响应
        log.info("Client {} is  waiting for Response",localAddress);
        //read()会阻塞进程，直到有输入，返回-1代表流结束
        while ((len = inputStream.read(bytes)) != -1) {
            //注意指定编码格式，发送方和接收方一定要统一，建议使用UTF-8
            sb.append(new String(bytes, 0, len,CHARSET));
        }
        log.info("Get response message '{}' from Server" , sb);
        //关闭连接
        outputStream.close();
        socket.close();
    }
}