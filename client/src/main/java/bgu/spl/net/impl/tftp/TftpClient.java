package bgu.spl.net.impl.tftp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;


public class TftpClient {
    public static void main(String[] args) throws IOException{
        try (Socket sock = new Socket("127.0.0.1", 7777);
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {
            TftpEncoderDecoder encdec = new TftpEncoderDecoder();
            KeyboardThread keyboardT = new KeyboardThread(sock, encdec);
            ListeningThread listenT = new ListeningThread(sock, encdec, keyboardT);
            Thread lT = new Thread(listenT);
            Thread kT = new Thread(keyboardT);
            lT.start();
            kT.start();
            try {
                kT.join();
                listenT.shouldTerminate = true;
                lT.join(); 
            } catch (InterruptedException ignored) {}
            sock.close();
        }
    }
}
