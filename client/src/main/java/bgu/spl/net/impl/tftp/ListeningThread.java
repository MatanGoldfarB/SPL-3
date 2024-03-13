package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;


public class ListeningThread implements Runnable {
    protected volatile boolean shouldTerminate = false;
    private final Socket sock;
    private BufferedInputStream in;
    private final TftpEncoderDecoder encdec;
    private final KeyboardThread keyThread;
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();


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
            //ACK
            short blockNumber = (short) ((msg[2] << 8) | (msg[3] & 0xFF));
            System.out.println("ACK " + blockNumber);
            synchronized(keyThread){
                keyThread.successful=true;
                keyThread.notify();
            }
        }
        else if(msg[1] == 5){
            //ERROR
            short errorNumber = (short) ((msg[2] << 8) | (msg[3] & 0xFF));
            byte[] errorMsg = new byte[msg.length-1 - 4];
            System.arraycopy(msg, 4, errorMsg, 0, msg.length-1 - 4);
            String errorString = new String(errorMsg, StandardCharsets.UTF_8);
            System.out.println("ERROR " + errorNumber + " " + errorString);
            synchronized(keyThread){
                keyThread.ended=true;
                keyThread.successful=false;
                keyThread.notify();
            }
        }
        else if(msg[1] == 3){
            //DATA
            short packetSize = (short) ((msg[2] << 8) | (msg[3] & 0xFF));
            if(keyThread.fileHandled != null){
                byte[] dataMsg = new byte[packetSize];
                System.arraycopy(msg, 6, dataMsg, 0, packetSize);
                try{
                    Files.write(keyThread.fileHandled, dataMsg, StandardOpenOption.APPEND);
                    if(packetSize < 512){
                        synchronized(keyThread){
                            keyThread.ended = true;
                            keyThread.fileHandled = null;
                            keyThread.successful=true;
                            keyThread.notify();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error appending to a file: " + e.getMessage());
                }
            }
            else{
                byte[] dataMsg = new byte[packetSize];
                System.arraycopy(msg, 6, dataMsg, 0, packetSize);
                try{
                    outputStream.write(dataMsg);
                    outputStream.flush();
                    if(packetSize < 512){
                        String content = outputStream.toString();
                        // Split the content based on the null byte delimiter
                        String[] lines = content.split("\\x00");
                        // Print each line
                        for (String line : lines) {
                            System.out.println(line);
                        }
                        synchronized(keyThread){
                            keyThread.ended = true;
                            keyThread.fileHandled = null;
                            keyThread.successful=true;
                            keyThread.notify();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error appending to a file: " + e.getMessage());
                }
            }
        }
        else if(msg[1]==9)
        {
            String filename= new String(msg, 3, msg.length-3, StandardCharsets.UTF_8);
            if(msg[2]==1) //add
               System.out.println("BCAST add " + filename);
            else
               System.out.println("BCAST del " + filename);
        }
    }
}