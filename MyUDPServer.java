import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class MyUDPServer {

//4-5 wyklad
//npk2.0
//np2.1
//npk3.0
//3-20 ZDARZENIE U ODBIORCY TCP
//3-49 PODSUMOWANIE: KONTROLA PRZECIAZENIA TCP

    InetAddress ClientAdress;
    int ClientPort;
    static boolean isTimeout = false;
    private DatagramSocket server;
    private LinkedList<DatagramSocket> knockPorts;
    static byte[] buff = new byte[UDP.MAX_DATAGRAM_SIZE];
    final DatagramPacket datagram = new DatagramPacket(buff, buff.length);

    public MyUDPServer(String[] args) throws IOException {
        ///////////////WLASNE ERRORY, SPRAWDZAJACE PARAMETRY///////////////////
        if (args.length < 1) {
            System.out.println("ERROR: Enter the ports for knocking, please");
            return;
        }
        for (String arg : args) {
            if (!arg.matches("\\d+")) {
                System.out.println("ERROR: Only numbers are allowed as ports");
                return;
            }
        }
        ///////////////////////////////////////////////////////////////////////
        knockPorts = new LinkedList<>();
        for (int i = 0; i < args.length; i++) {
            knockPorts.add(new DatagramSocket(Integer.valueOf(args[i].trim())));
            System.out.println("Port is opened: " + knockPorts.get(i).getLocalPort());
        }
        //Serwer wylacza sie po 60 sekundach, jesli nie zapukac w prawidlowe porty
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future Timeout = executor.submit(() -> {
            try {
                System.out.print("Waiting for the right port combination.");
                Thread.sleep(1000);
                System.out.print(".");
                Thread.sleep(1000);
                System.out.println(".");
                Thread.sleep(1000);
                System.out.println("Server will shutdown in 60 seconds...");
                Thread.sleep(15000);
                System.out.println("Server will shutdown in 45 seconds...");
                Thread.sleep(15000);
                System.out.println("Server will shutdown in 30 seconds...");
                Thread.sleep(15000);
                System.out.println("Server will shutdown in 15 seconds...");
                Thread.sleep(10000);
                System.out.println("Server will shutdown in 5 seconds...");
                Thread.sleep(5000);
                System.out.println("Shutting down...");
                Thread.sleep(1000);
                isTimeout = true;
                for (DatagramSocket knockPort : knockPorts) {
                    knockPort.close();
                }
            } catch (InterruptedException e) {
                System.out.println("Right port combination has been entered");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                }
                System.out.println("Server shutdown has been interrupted");
                return;
            }
        });
        int counter = 0;
        //Proces pukania
        while (counter < knockPorts.size()) {
            for (int i = 0; i < knockPorts.size(); i++) {
                System.out.println(knockPorts.get(counter).getLocalPort() + " is waiting to be knocked");
                knockPorts.get(i).receive(datagram);
                if(counter>0 && (datagram.getAddress()!=ClientAdress && datagram.getPort()!=ClientPort)){
                    isTimeout = true;
                    for (DatagramSocket knockPort : knockPorts) {
                        knockPort.close();
                    }
                }
                if (Integer.valueOf(new String(datagram.getData(), 0, datagram.getLength())) == counter) {
                    System.out.println(knockPorts.get(counter).getLocalPort() + " has been knocked!");
                    if (counter == 0) {
                        ClientAdress = datagram.getAddress();
                        ClientPort = datagram.getPort();
                    }
                    ++counter;
                } else {
                    isTimeout = true;
                    for (DatagramSocket knockPort : knockPorts) {
                        knockPort.close();
                    }
                }
            }
        }
        //Koniec procesu pukania i zabijanie timeoutu
        Timeout.cancel(true);
        executor.shutdownNow();

        initializeServer();
    }

    private void initializeServer() {
        //Losujemy port i mowimy o tym klientu
        int randPort = (int) (Math.random() * (knockPorts.size() - 1));
        System.out.println("Random download port is: " + knockPorts.get(randPort).getLocalPort());
        String connectionInfo = "Access granted! \nDownload port is: " + knockPorts.get(randPort).getLocalPort();
        byte[] connectionBuf = connectionInfo.getBytes();
        DatagramPacket connectionPacket = new DatagramPacket(connectionBuf, connectionBuf.length, ClientAdress, ClientPort);
        server = knockPorts.get(randPort);
        try {
            server.send(connectionPacket);
            //Zaczynamy wysylanie pliku
            service();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void service() throws IOException {
        server.receive(datagram);
        //Czekamy na ostatni puk w download port

        new Thread(() -> {
            //Plik mozna latwo zmienic na inny
            File file = new File("UDPServerFolder\\Hej_Sokoly.mp3");
            //Testowalem to nawet na grze w .zip (okolo 1 GB), wszystko dzialalo poprawnie

            byte[] FileName = (file.getName()).getBytes();
            byte[] FileLength = String.valueOf(file.length()).getBytes();
            byte[] filePieceBuff = new byte[UDP.MAX_DATAGRAM_SIZE];

//            DatagramPacket resp = new DatagramPacket(respBuff, respBuff.length, clientAddress, clientPort);
            FileInputStream fileReader;
            try {
                server.send(new DatagramPacket(FileName, FileName.length, ClientAdress, ClientPort));
                //Wysylamy nazwe
                server.send(new DatagramPacket(FileLength, FileLength.length, ClientAdress, ClientPort));
                //I rozmiar pliku
                fileReader = new FileInputStream(file);
                int packetNumber = 0;
                while (fileReader.read(filePieceBuff, 0, filePieceBuff.length) > 0) {
                    //Przy wysylaniu plikow, wypisujemy ich numer
                    //Jesli ostatni numer pliku == ostatni numer pliku u klienta, to plik zostal wyslany na 100% poprawnie
                    System.out.println("Processing... " + packetNumber);
                    DatagramPacket dp = new DatagramPacket(filePieceBuff, filePieceBuff.length, ClientAdress, ClientPort);
                    server.send(dp);
                    server.receive(datagram);
                    if((datagram.getAddress().equals(ClientAdress) && datagram.getPort()==ClientPort) && (Integer.valueOf(new String(datagram.getData(), 0, datagram.getLength()))!=packetNumber++)){
                        server.send(dp);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
//        args = new String[4];
//        args[0] = "18917";
//        args[1] = "20402";
//        args[2] = "40204";
//        args[3] = "65000";
        try {
            System.out.println("Hello!");
            System.out.println("Please, enter the ports for knocking");
            System.out.println("Divide the ports using spacebars, please");
            new MyUDPServer(args);
        } catch (SocketException e) {
            if (!isTimeout)
                System.out.println("Could not set up the server");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
