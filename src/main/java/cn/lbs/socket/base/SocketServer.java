package cn.lbs.socket.base;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

@Slf4j
public class SocketServer {
    public static void main(String[] args) throws Exception {
        // 创建具有指定端口的服务器套接字
        final int PORT = 8081;
        //注意指定编码格式，发送方和接收方一定要统一，建议使用UTF-8
        final String CHARSET="UTF-8";
        ServerSocket server = new ServerSocket(PORT);
        SocketAddress serverAddress=server.getLocalSocketAddress();


        // accept() 会阻塞进程，直到建立新连接，成功建立连接后会返回一个新的Socket。
        log.info("Waiting for new connection(Listening on : {})",serverAddress);
        Socket socket = server.accept();


        // 建立好连接后，从socket中获取输入、输出流、客户端信息
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream=socket.getOutputStream();
        SocketAddress clientAddress=socket.getRemoteSocketAddress();
        log.info("There is a new  connection {}",socket);

        //建立缓冲区用于读取或写入
        byte[] bytes = new byte[1024];
        int len;
        StringBuilder sb = new StringBuilder();
        log.info("Waiting for message from client {} ",clientAddress);

        //read()会阻塞进程，直到有输入，返回-1代表流结束
        while ((len = inputStream.read(bytes)) != -1) {
            //注意指定编码格式，发送方和接收方一定要统一，建议使用UTF-8
            sb.append(new String(bytes, 0, len,CHARSET));
        }
        log.info("Get a message '{}' from client {}" ,sb,socket.getRemoteSocketAddress());

        //关闭连接和服务器
        inputStream.close();
        socket.close();
        server.close();
    }
}
