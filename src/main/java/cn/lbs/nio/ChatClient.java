package cn.lbs.nio;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class ChatClient {
    Selector selector;
    SocketChannel socketChannel;
    boolean running = true;

    MessageType messageType = MessageType.REG_CLIENT_ACK;
    String prompt = "";
    String userName= "";
    //NIO客户端模板
    public void runClient() throws IOException {
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress("127.0.0.1", ChatServer.SERVER_PORT));
            System.out.println("Client connecting to server.");

            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            while (running) {
                int eventCount = selector.select(100);
                if (eventCount == 0)
                    continue;
                Set<SelectionKey> set = selector.selectedKeys();
                Iterator<SelectionKey> keyIterable = set.iterator();
                while (keyIterable.hasNext()) {
                    SelectionKey key = keyIterable.next();
                    keyIterable.remove();
                    //核心函数
                    dealEvent(key);
                }
            }
        } finally {
            if (selector != null && selector.isOpen())
                selector.close();

            if (socketChannel != null && socketChannel.isConnected())
                socketChannel.close();
        }
    }

    private void dealEvent(SelectionKey key) throws IOException {
        if (key.isConnectable()) {
            SocketChannel channel = (SocketChannel) key.channel();
            if (channel.isConnectionPending()) {
                channel.finishConnect();
            }
            channel.register(selector, SelectionKey.OP_READ);

            //处理发送事务
            new Thread(() -> {
                try {
                    Thread.sleep(500);
//                    printMsgAndPrompt("Start to interconnect with server.");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    while (running) {
                        //获取用户输入
                        System.out.print(prompt);
                        String msg = reader.readLine();
                        if (msg == null || msg.length() == 0)
                            continue;
                        //messageType 初始值是 REG_CLIENT_ACK
                        if (messageType == MessageType.REG_CLIENT_ACK) {
                            ByteBuffer bufferMsg = Message.encodeRegClientAck(msg);
                            channel.write(bufferMsg);
                            userName=msg;
                        } else {
                            //可选择广播或者单独发送
                            String[] msgArr = msg.split("#", 2);
                            ByteBuffer bufferMsg = Message.encodeSendMsg(msg);
                            if (msgArr.length == 2) {
                                bufferMsg = Message.encodeSendMsg(msgArr[0], msgArr[1]);
                            }

                            channel.write(bufferMsg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {

                }
            }).start();
        } else if (key.isReadable()) {
            try {
                SocketChannel channel = (SocketChannel) key.channel();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                channel.read(byteBuffer);
                byteBuffer.flip();
                String msg = Message.CHARSET.decode(byteBuffer).toString();
                dealMsg(msg);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Server exit.");
                System.exit(0);
            }
        }
    }
    //处理收到的消息
    private void dealMsg(String msg) {
        Message message = Message.decode(msg);
        if (message == null)
            return;

        switch (message.getAction()) {
            //收到注册要求的消息
            case REG_SERVER_SYN:
                clearInputPrompt();
                printMsgAndPrompt(message.getMessage());
                break;
            case CHAT_MSG_RECEIVE:
                clearInputPrompt();
                printMsgAndPrompt(String.format("%s:%s", message.getOption(), message.getMessage()));
                break;
            case REG_SERVER_ACK:
                messageType = MessageType.CHAT_MSG_SEND;
                prompt = userName+":";
                printMsgAndPrompt(message.getMessage());
                break;
            case BROADCAST_USER_LIST:
                clearInputPrompt();
                printMsgAndPrompt(String.format("User list: %s", message.getMessage()));
                break;
            default:
        }
    }

    private void printMsgAndPrompt(String msg) {
        System.out.println(msg);
        System.out.print(prompt);
    }
    void clearInputPrompt(){
        for(int i=0;i<userName.length()+1;i++){
            System.out.print("\b");
        }
    }
    public static void main(String[] args) throws IOException {
        new ChatClient().runClient();
    }
}