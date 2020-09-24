import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class MyUDPClient implements Runnable {
    InetAddress address;
    DatagramSocket socket;
    int datagramNumber;

    MyUDPClient() {
        try {
            datagramNumber = 0;
            //numer datagramu z portem
            address = InetAddress.getLocalHost();
            socket = new DatagramSocket();
            new Thread(this).start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private volatile boolean running = true;

    void receiveInfo() {
        try {
            byte[] buff = new byte[UDP.MAX_DATAGRAM_SIZE];
            DatagramPacket packet = new DatagramPacket(buff, buff.length);
            socket.receive(packet);
            String str = new String(packet.getData(), 0, packet.getLength()).trim();
            //Przyjmujemy packet z portem dla ladowania pliku
            System.out.println(str);
            socket.receive(packet);
            String fileName;
            System.out.println("File's name is: " + (fileName = new String(packet.getData(), 0, packet.getLength())));
            //Przyjmujemy nazwe pliku
            socket.receive(packet);
            long fileSize = Long.valueOf(new String(packet.getData(), 0, packet.getLength()).trim());
            System.out.println("File's size is: " + fileSize);
            FileOutputStream fileWriter = new FileOutputStream("UDPClientFolder\\"+fileName);
            //Przyjmujemy rozmiar pliku
            int packetNumber = 0;
            //Poczatek otrzymywania pliku (nie znalazlem konkretnego pliku do wyslania i zrobilem zamiane)
            while (fileSize>0){
                socket.receive(packet);
                socket.send(new DatagramPacket(String.valueOf(packetNumber).getBytes(),String.valueOf(packetNumber).getBytes().length, packet.getAddress(), packet.getPort()));
                System.out.println("Packet #" + packetNumber++ + " received!");
                fileWriter.write(packet.getData());
                fileSize-=packet.getLength();
            }
            //Koniec otrzymywania pliku (mam nadzieje ze ta zamiana panu sie spodoba :D)
            running = false;
            socket.close();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MyUDPClient().receiveInfo();
    }


    //Dla wygodnego odprawiania pakietow na wybrane porty
    @Override
    public void run() {
        try {
            Scanner scanner = new Scanner(System.in);
            String ports;
            while (running) {
                System.out.println("Enter the port you want to knock to");
                System.out.println("If you want to exit, type EXIT");
                ports = scanner.nextLine();
                if (ports.contains("EXIT")) {
                    System.out.println("CLOSING...");
                    running = false;
                    System.exit(0);
                }
                if (ports.matches("\\d+")) {
                    int port = Integer.valueOf(ports);
                    if(port<=65535) {
                        byte[] queryBuff = String.valueOf(datagramNumber).getBytes();
                        DatagramPacket query = new DatagramPacket(queryBuff, queryBuff.length, address, port);
                        socket.send(query);
                        System.out.println("I've sent port №" + datagramNumber++);
                    }
                    else
                        System.out.println("ERROR:This port is out of bounds");
                }
            }
            /*
            Port is opened: 18917
            Port is opened: 20402
            Port is opened: 40204
            Port is opened: 65000
             */
            socket.close();
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
