package cn.lbs.socket.communication;

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
        byte[] receiveBytes,sendBytes;

        
        // 因为可以复用Socket且能判断长度，所以可以一个Socket用到底
        while (true) {
            log.info("Waiting for message from client {} ",clientAddress);
            // 首先读取两个字节表示的长度,read()方法会阻塞进程
            int firstByte = inputStream.read();
            //如果读取的值为-1 说明到了流的末尾，Socket已经被关闭了，此时将不能再去读取
            if(firstByte==-1){
                break;
            }
            int secondByte= inputStream.read();
            int messageLen= (firstByte<<8)+secondByte;
            // 然后构造一个指定长的byte数组
            receiveBytes = new byte[messageLen];
            // 然后读取指定长度的消息即可
            inputStream.read(receiveBytes);
            log.info("Get  message '{}' from client {}" , new String(receiveBytes, CHARSET),clientAddress);




            String respStr= "Hello,I get the message";
            log.info("Response message '{}' to Client {} ",respStr,clientAddress);
            //首先需要计算得知消息的长度
            sendBytes = respStr.getBytes(CHARSET);
            //然后将消息的长度优先发送出去
            outputStream.write(sendBytes.length >>8);
            outputStream.write(sendBytes.length);
            //然后将消息再次发送出去
            outputStream.write(sendBytes);
            outputStream.flush();
        }
        //关闭连接和服务器
        socket.close();
        server.close();
    }
}
