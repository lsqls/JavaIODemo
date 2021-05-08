package cn.lbs.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server {
    public static void main(String[] args) {
        try
        {
            DatagramSocket ds=new DatagramSocket(8082);
            System.out.println("UDP服务器已启动。。。");
            DatagramPacket dpReceive;
            byte[] b=new byte[1024];
            while(ds.isClosed()==false)
            {
                dpReceive=new DatagramPacket(b, b.length);
                try
                {
                    ds.receive(dpReceive);
                    byte[] Data=dpReceive.getData();
                    int len=Data.length;
                    System.out.println("UDP客户端"+dpReceive.getSocketAddress()+"发送的内容是：" + new String(Data, 0, len));
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch(SocketException e1)
        {
            e1.printStackTrace();
        }
    }
}

