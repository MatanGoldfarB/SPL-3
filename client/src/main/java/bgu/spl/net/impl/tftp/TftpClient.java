package bgu.spl.net.impl.tftp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class TftpClient {
    public static void main(String[] args) throws IOException{
        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, port");
            System.exit(1);
        }
        try (Socket sock = new Socket(args[0], Integer.parseInt(args[1]));
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {
            ListeningThread<byte[]> listenT = new ListeningThread<byte[]>();
            KeyboardThread<byte[]> keyboardT = new KeyboardThread<byte[]>();
            Thread lT = new Thread(listenT);
            Thread kT = new Thread(keyboardT);
            lT.start();
            kT.start();
            try {
                kT.join();
                lT.join(); 
            } catch (InterruptedException ignored) {}
            sock.close();
        }
    }
}
