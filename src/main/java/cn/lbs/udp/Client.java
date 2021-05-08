package cn.lbs.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {
    public static void main(String[] args)  {


        try
        {
            DatagramSocket udpScoket=new DatagramSocket();
            InetAddress serverAddress=InetAddress.getByName("127.0.0.1");
            int serverPort=8082;

            DatagramPacket udPacket=null;
            String[] messages={"YOLO","HELLO","JUST DO IT"};
            for (String message:messages)
            {
                byte[] data=message.getBytes();
                udPacket=new DatagramPacket(data,data.length,serverAddress,serverPort);
                udpScoket.send(udPacket);
                Thread.sleep(1000);
            }
            udpScoket.close();
        }
        catch(IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
