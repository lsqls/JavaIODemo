package cn.lbs.socket.concurrency;


import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.RandomStringUtils;

@Slf4j
public class MultiClient {
    // 设置连接的服务端IP地址和端口
    static final String HOST = "127.0.0.1";
    static final int PORT = 8081;
    static final int CLIENTSNUM=100;
    public static void main(String args[]) throws Exception {
        ExecutorService threadPool= Executors.newCachedThreadPool();
        for(int i=0;i<CLIENTSNUM;i++){
            threadPool.submit(new ClientTask(HOST,PORT));
        }
    }

}

@Slf4j
class ClientTask implements  Runnable{
    //注意指定编码格式，发送方和接收方一定要统一，建议使用UTF-8
    final String CHARSET="UTF-8";
    final int MESSAGENUM=100;
    Socket socket ;

    OutputStream outputStream;
    InputStream inputStream;
    SocketAddress   localAddress;
    SocketAddress remoteAddress;

    ClientTask(String host,int port) throws Exception{
        // 与服务端建立连接
        // 建立连接后获得输出、输出流、连接信息
        socket=new Socket(host,port);
        outputStream = socket.getOutputStream();
        inputStream=socket.getInputStream();
        localAddress=socket.getLocalSocketAddress();
        remoteAddress=socket.getRemoteSocketAddress();
        log.info("The client {} successfully connects to the server {}",localAddress,remoteAddress);
    }

    @Override
    public void run()  {
        try {
            String[] messages = {"Hello World","你好，世界","YOLO"};
            byte[] receiveBytes,sendBytes;

            for(int i=0;i<MESSAGENUM;i++) {
                String message=RandomStringUtils.random(10, true, true);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                log.info("The client {} sends a message '{}' to the server ", localAddress, message);
                //首先需要计算得知消息的长度
                sendBytes = message.getBytes(CHARSET);
                //然后将消息的长度优先发送出去
                outputStream.write(sendBytes.length >> 8);
                outputStream.write(sendBytes.length);
                //然后将消息再次发送出去
                outputStream.write(sendBytes);
                outputStream.flush();


                //获取响应
                log.info("Client {} is  waiting for Response", localAddress);

                // 首先读取两个字节表示的长度,read()方法会阻塞进程
                int firstByte = inputStream.read();
                //如果读取的值为-1 说明到了流的末尾，Socket已经被关闭了，此时将不能再去读取
                if (firstByte == -1) {
                    break;
                }
                int secondByte = inputStream.read();
                int messageLen = (firstByte << 8) + secondByte;
                // 然后构造一个指定长的byte数组
                receiveBytes = new byte[messageLen];
                // 然后读取指定长度的消息即可
                inputStream.read(receiveBytes);
                log.info("Get response message '{}' from Server", new String(receiveBytes, CHARSET));
            }
            log.info("关闭连接{}",socket);
            socket.shutdownOutput();
            socket.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}