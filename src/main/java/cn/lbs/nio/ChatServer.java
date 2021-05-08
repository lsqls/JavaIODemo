package cn.lbs.nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ChatServer {
    public static final int SERVER_PORT = 8080;

    Selector selector;
    ServerSocketChannel serverSocketChannel;
    boolean running = true;

    public void runServer() throws IOException {
        //NIO服务端模板
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();

            serverSocketChannel.bind(new InetSocketAddress(SERVER_PORT));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            log.info("Server started{}",serverSocketChannel);

            while (running) {
                int eventCount = selector.select(100);
                if (eventCount == 0)
                    continue;
                Set<SelectionKey> set = selector.selectedKeys();
                Iterator<SelectionKey> keyIterable = set.iterator();
                while (keyIterable.hasNext()) {
                    SelectionKey key = keyIterable.next();
                    keyIterable.remove();
                    //dealEvent 是关键
                    dealEvent(key);
                }
            }
        } finally {
            if (selector != null && selector.isOpen())
                selector.close();
            if (serverSocketChannel != null && serverSocketChannel.isOpen())
                serverSocketChannel.close();
        }
    }

    private void dealEvent(SelectionKey key) throws IOException {
        //处理新连接
        if (key.isAcceptable()) {
            SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            log.info("Accept client connection {}",socketChannel);
            //给客户端发送"注册要求"的消息
            socketChannel.write(Message.encodeRegSyn());
        }
        //处理读取的Message
        if (key.isReadable()) {
            SocketChannel socketChannel = null;
            try {
                socketChannel = (SocketChannel) key.channel();
                log.info("Receive message from client. {} ",socketChannel);
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                socketChannel.read(byteBuffer);
                byteBuffer.flip();
                //msg：客户端发送来的命令
                String msg = Message.CHARSET.decode(byteBuffer).toString();
                //服务端根据收到的命令进行相应处理
                dealMsg(msg, key);
            }
            //当Socket连接的一端关闭时，另一端会触发 OP_READ 事件，但此时 socketChannel.read(byteBuffer) 返回-1或抛IOException，需要捕获这个异常并关闭socketChannel。
            catch (IOException e) {
                socketChannel.close();
                String username = (String) key.attachment();
                log.info("User {} disconnected", username);
                //向其他用户广播用户更新后的用户列表
                broadcastUserList();
            }
        }
    }


    //核心函数
    private void dealMsg(String msg, SelectionKey key) throws IOException {
        log.info("Message info is: {}", msg);
        Message message = Message.decode(msg);
        if (message == null)
            return;

        SocketChannel currentChannel = (SocketChannel) key.channel();
        Set<SelectionKey> keySet = getConnectedChannel();
        switch (message.getAction()) {
            //处理用户注册
            case REG_CLIENT_ACK:
                String username = message.getMessage();
                //检查是否与其他人重名
                for (SelectionKey keyItem : keySet) {
                    String channelUser = (String) keyItem.attachment();
                    if (channelUser != null && channelUser.equals(username)) {
                        currentChannel.write(Message.encodeRegSyn(true));
                        return;
                    }
                }
                key.attach(username);
                currentChannel.write(Message.encodeRegServerAck(username));
                log.info("New user joined: {},", username);
                broadcastUserList();
                break;
            case CHAT_MSG_SEND:
                //获取用户向发送的用户，null表示广播
                String toUser = message.getOption();
                String msg2 = message.getMessage();
                String fromUser = (String) key.attachment();

                for (SelectionKey keyItem : keySet) {
                    if (keyItem == key) {
                        continue;
                    }
                    String channelUser = (String) keyItem.attachment();
                    SocketChannel channel = (SocketChannel) keyItem.channel();
                    if (toUser == null || toUser.equals(channelUser)) {
                        channel.write(Message.encodeReceiveMsg(fromUser, msg2));
                    }
                }
                break;
        }
    }

    public void broadcastUserList() throws IOException {
        Set<SelectionKey> keySet = getConnectedChannel();
        //通过Selector.keys可获取所有向Selector注册的客户端，获取客户端连接列表时，需要过滤掉ServerSocketChannel和关闭的Channel
        List<String> uList = keySet.stream().filter(item -> item.attachment() != null).map(SelectionKey::attachment)
                .map(Object::toString).collect(Collectors.toList());
        for (SelectionKey keyItem : keySet) {
            SocketChannel channel = (SocketChannel) keyItem.channel();
            channel.write(Message.encodePublishUserList(uList));
        }
    }

    private Set<SelectionKey> getConnectedChannel() {
        return selector.keys().stream()
                .filter(item -> item.channel() instanceof SocketChannel && item.channel().isOpen())
                .collect(Collectors.toSet());
    }

    public static void main(String[] args) throws IOException {
        new ChatServer().runServer();
    }
}