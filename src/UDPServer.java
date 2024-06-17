

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.logging.SimpleFormatter;

public class UDPServer {
    private static final int PORT = 12345;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(PORT);
        byte[] synData = new byte[BUFFER_SIZE];
        DatagramPacket synPacket = new DatagramPacket(synData, synData.length);
        System.out.println("服务器已启动。侦听端口 " + PORT);
        socket.receive(synPacket);
        // 第一次握手：服务器接收客户端的SYN包
        String syn = new String(synPacket.getData(), 0, synPacket.getLength());
        if(syn.equals("SYN")){
            System.out.println("收到"+syn);
            // 第二次握手：服务器发送SYN-ACK包
            String synAck = "SYN-ACK";
            byte[] synAckData = synAck.getBytes();
            DatagramPacket synAckPacket = new DatagramPacket(synAckData, synAckData.length, synPacket.getAddress(), synPacket.getPort());
            socket.send(synAckPacket);
            System.out.println("发送SYN-ACK");
        }else{
            System.out.println("三次握手失败");
            System.exit(1);
        }
        // 第三次握手：服务器接收客户端的ACK包
        byte[] ackData = new byte[BUFFER_SIZE];
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
        socket.receive(ackPacket);
        String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
        System.out.println("收到ack"+"\n");


        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket request = new DatagramPacket(buffer, buffer.length);
        while(true){


            socket.receive(request);
            String seqNo= new String(request.getData(), 0, request.getLength()).split("\\|")[0];
            String requestString = new String(request.getData(), 0, request.getLength());
            // 在服务器端代码中，接收到"DISCONNECT"数据包后响应并关闭连接
            if(requestString.equals("FIN")) {
                System.out.println("收到"+"FIN");
                String responseDisconnectString = "ACK";
                byte[] responseByteDisconnect = responseDisconnectString.getBytes();
                DatagramPacket responseDisconnect = new DatagramPacket(responseByteDisconnect, 0, responseByteDisconnect.length, request.getAddress(), request.getPort());
                socket.send(responseDisconnect);
                System.out.println("发送ACK\n");

                responseDisconnectString = "FIN-ACK";
                responseByteDisconnect = responseDisconnectString.getBytes();
                responseDisconnect = new DatagramPacket(responseByteDisconnect, 0, responseByteDisconnect.length, request.getAddress(), request.getPort());
                socket.send(responseDisconnect);
                System.out.println("发送FIN-ACK");

                socket.receive(request);
                String AckData = new String(request.getData(), 0, request.getLength());
                System.out.println("收到Ack");
                // 关闭服务器端Socket连接
                socket.close();
                System.out.println("连接关闭");
                break; // 退出循环
            }
            //打印输出
            System.out.println(" seq为"+seqNo+"的Packet的数据: " + requestString);


            if(new Random().nextDouble()<0.4){
                //打印输出
                System.out.println(" 第"+seqNo+"个"+"Packet lost\n");
                continue;
            }
            String currentTime = new SimpleDateFormat("HH-mm-ss").format(new Date());
            String responseString = seqNo+"|"+ requestString.split("\\|")[1]+ "|" + currentTime;
            byte[] responseStringBuffer = responseString.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseStringBuffer,responseStringBuffer.length,request.getAddress(),request.getPort());
            socket.send(responsePacket);
            //打印输出
            System.out.println(" 第"+seqNo+"个 Packet "+"的回复: " + responseString+"\n");
        }
    }
}
