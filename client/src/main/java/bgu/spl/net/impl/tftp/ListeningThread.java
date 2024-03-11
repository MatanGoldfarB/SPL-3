package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class ListeningThread implements Runnable {
    private volatile boolean shouldTerminate = false;
    private final Socket sock;
    private BufferedInputStream in;
    private final TftpEncoderDecoder encdec;
    private final KeyboardThread keyThread;


    public ListeningThread(Socket sock, TftpEncoderDecoder reader, KeyboardThread keyThread){
        this.sock = sock;
        this.encdec = reader;
        this.keyThread = keyThread;
    }

    @Override
    public void run() {
        try {
            int read;
            in = new BufferedInputStream(sock.getInputStream());
            while(!shouldTerminate && (read = in.read()) >= 0){
                byte[] nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    process(nextMessage);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void process(byte[] msg){
        if(msg[1] == 4){
            short blockNumber = (short) ((msg[2] << 8) | (msg[3] & 0xFF));
            System.out.println("ACK " + blockNumber);
            synchronized(keyThread){
                keyThread.notify();
            }
        }
        else if(msg[1] == 5){
            short errorNumber = (short) ((msg[2] << 8) | (msg[3] & 0xFF));
            byte[] errorMsg = new byte[msg.length-1 - 4];
            System.arraycopy(msg, 4, errorMsg, 0, msg.length-1 - 4);
            String errorString = new String(errorMsg, StandardCharsets.UTF_8);
            System.out.println("ERROR " + errorNumber + " " + errorString);
            synchronized(keyThread){
                keyThread.notify();
            }
        }
    }
}