

import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class UDPClient {
    private static  String SERVER_IP;
    private static  int SERVER_PORT;
    private static final int BUFFER_SIZE = 1024;
    private static final int TIMEOUT = 100; // 超时时间（毫秒）
    private static final int MAX_RETRIES = 2; // 最大重试次数

    public static void main(String[] args) throws SocketException, Exception {
        DatagramSocket socket = new DatagramSocket();
        Scanner sc = new Scanner(System.in);

        System.out.print("请输入服务器IP地址: ");
        SERVER_IP = sc.nextLine();

        System.out.print("请输入服务器的端口号: ");
        SERVER_PORT = sc.nextInt();
        if (SERVER_PORT < 1 || SERVER_PORT > 65535) {
            System.out.println("端口号必须在1到65535之间。");
            System.exit(1);
        }
        sc.close();
        InetAddress address = InetAddress.getByName(SERVER_IP);


        System.out.println("尝试与服务器端连接..." );
        // 第一次握手：客户端发送SYN包
        String syn = "SYN";
        byte[] synData = syn.getBytes();
        DatagramPacket synPacket = new DatagramPacket(synData, synData.length, address, SERVER_PORT);
        socket.send(synPacket);
        System.out.println("发送SYN");

        // 第二次握手：客户端接收服务器的SYN-ACK包
        byte[] synAckData = new byte[BUFFER_SIZE];
        DatagramPacket synAckPacket = new DatagramPacket(synAckData, synAckData.length);
        socket.receive(synAckPacket);
        String synAck = new String(synAckPacket.getData(), 0, synAckPacket.getLength());
        System.out.println("收到synAck");
        // 第三次握手：客户端发送ACK包
        if(synAck.equals("SYN-ACK")){
            String ack ="ACK";
            byte[] ackData = ack.getBytes();
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, SERVER_PORT);
            socket.send(ackPacket);
            System.out.println("发送ACK");
        }else{
            System.out.println("三次握手失败");
            System.exit(1);
        }





        List<Long> rttList = new ArrayList<Long>();
        List<String> responseTimes = new ArrayList<String>();
        for(int seqNo = 1;seqNo<=12;seqNo++) {
            String requestData = seqNo + "|2|";
            requestData +=generateRandomData(200);

            byte[] buffer = requestData.getBytes();
            DatagramPacket request = new DatagramPacket(buffer,buffer.length,address,SERVER_PORT);

            boolean  receivedResponse = false;
            int retries = 0;

            while (!receivedResponse && retries <=MAX_RETRIES) {
                long startTime = System.currentTimeMillis();
                socket.send(request);

                //设置套接字的超时时间，即等待服务器响应的最大时间
                socket.setSoTimeout(TIMEOUT);

                try {
                    byte[] responseBuffer = new byte[BUFFER_SIZE];
                    DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
                    socket.receive(response);

                    long endTime = System.currentTimeMillis();
                    long rtt = endTime - startTime;
                    rttList.add(rtt);

                    String responseData = new String(response.getData(), 0, response.getLength());

                    String serverTime = responseData.split("\\|")[2];
                    responseTimes.add(serverTime);

                    receivedResponse = true;
                    System.out.println("第"+seqNo+"个 Packet "+"回复: " + responseData +" sequence no:"+responseData.split("\\|")[0] +
                            " serverIP:"+ response.getPort() +", RTT: " + rtt + "ms"+"\n");
                } catch (SocketTimeoutException e) {
                    retries++;
                    System.out.println("第"+seqNo+"个包"+"Timeout. Retrying...");
                }
            }
            if (!receivedResponse) {
                System.out.println("第"+seqNo+"个包"+"未收到回复"+"\n");
            }
        }

//        socket.close();

        // 发送关闭连接请求
        String fin ="FIN";
        byte[] finData = fin.getBytes();
        DatagramPacket finPacket = new DatagramPacket(finData, finData.length, address, SERVER_PORT);
        socket.send(finPacket);
        System.out.println("发送FIN");
        // 接收ACK
        byte[] responseBuffer = new byte[BUFFER_SIZE];
        DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
        socket.receive(response);
        String responseData = new String(response.getData(), 0, response.getLength());
        System.out.println("Server response: " + responseData);
        //接收FIN-ACK
        socket.receive(response);
        responseData = new String(response.getData(), 0, response.getLength());
        System.out.println("Server response: " + responseData);
        String ack = "ACK";
        byte[] ackData = ack.getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, SERVER_PORT);
        socket.send(ackPacket);
        System.out.println("发送ACK");
        // 关闭连接
//        socket.setSoTimeout(120000);
        socket.setSoTimeout(12);
        System.out.println("等待2MSL");
        try {
            socket.receive(response);
        } catch (SocketTimeoutException e) {
            socket.close();
        }
        System.out.println("连接关闭\n");


        System.out.println("总结:");
        System.out.println("收到了" + rttList.size()+"个 UDP packets");
        System.out.println("收到了" +  (12 - rttList.size())+"个 UDP packets" );
        System.out.println("丢失率: " + ((12 - rttList.size()) / 12.0) * 100 + "%");

        if (!rttList.isEmpty()) {
            long maxRTT = Long.MIN_VALUE;
            long minRTT = Long.MAX_VALUE;
            long sumRTT = 0;

            for (long rtt : rttList) {
                if (rtt > maxRTT) {
                    maxRTT = rtt;
                }
                if (rtt < minRTT) {
                    minRTT = rtt;
                }
                sumRTT += rtt;
            }

            double avgRTT = (double) sumRTT / rttList.size();

            double stdDevRTT = calculateStandardDeviation(rttList);

            System.out.println("Max RTT: " + maxRTT + "ms, Min RTT: " + minRTT + "ms, Avg RTT: " + avgRTT + "ms, RTT Standard Deviation(标准差): " + stdDevRTT + "ms");
        }

        if (!responseTimes.isEmpty()) {
            String firstResponseTime = responseTimes.get(0);
            String lastResponseTime = responseTimes.get(responseTimes.size() - 1);//得到首和尾
            long serverResponseTime = calculateResponseTimeDifference(firstResponseTime, lastResponseTime);
            //打印server整体的响应时间
            System.out.println("server整体的响应时间: " + serverResponseTime + " s");
        }
    }

    //计算server相应时间
    private static long calculateResponseTimeDifference(String firstResponseTime, String lastResponseTime) throws Exception {
        SimpleDateFormat format  = new SimpleDateFormat("HH-mm-ss");
        long firstTime = format.parse(firstResponseTime).getTime();
        long lastTime = format.parse(lastResponseTime).getTime();
        //System.out.println(firstTime+","+lastTime);
        return (lastTime-firstTime)/1000;//输出秒
    }

    //计算RTT标准差
    private static double calculateStandardDeviation(List<Long> rttList) {
        long sum =0;
        int n = rttList.size();

        for (long rtt: rttList) {
            sum+=rtt;
        }
        double ave = (double)sum/n;
        double Dev=0;
        for(long rtt: rttList) {
            Dev+=(rtt-ave)*(rtt-ave);
        }
        double stdDev = (double)Math.sqrt(Dev/n);
        return stdDev;
    }

    //用来生成随机数据
    private static String generateRandomData(int len) {
        Random r  = new Random();
        StringBuilder str = new StringBuilder();
        for (int i =0;i<len;i++){
            str.append((char)('a'+r.nextInt(26)));
        }
        return str.toString();
    }
}
