package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class KeyboardThread implements Runnable {
    private volatile boolean shouldTerminate = false;
    private final Socket sock;
    private BufferedOutputStream out;
    private final MessageEncoderDecoder<byte[]> encdec;
    protected boolean successful = false;
    protected boolean ended = false;
    protected Path fileHandled = null;
    private boolean connected = false;

    public KeyboardThread(Socket sock, MessageEncoderDecoder<byte[]> writer){
        this.sock = sock;
        this.encdec = writer;
    }
    @Override
    public void run() {
        try {
            Scanner scanner = new Scanner(System.in);
            out = new BufferedOutputStream(sock.getOutputStream());
            while(!shouldTerminate){
                String userInput = scanner.nextLine();
                String[] words = userInput.split("\\s+",2);
                if(words[0].equals("LOGRQ")){
                    sendLogrq(words[1]);
                    if(successful){
                        connected = true;
                    }
                }
                else if(words[0].equals("DELRQ")){
                    sendDelrq(words[1]);
                }
                else if(words[0].equals("RRQ")){
                    String directoryFilePathString = "client";
                    Path filePath = Paths.get(directoryFilePathString, words[1]);
                    try {
                        // Create the empty file
                        Files.createFile(filePath);
                        fileHandled = filePath;
                        sendRrq(words[1]);
                        short blockNumber = 1;
                        while(!ended){
                            sendAck(blockNumber);
                            blockNumber++;
                        }
                        if(!successful){
                            Files.delete(filePath);
                        }
                        else{
                            System.out.println("RRQ " + words[1] + " complete");
                        }
                        successful = false;
                    } catch (IOException e) {
                        System.out.println("File already exists");
                    }
                }
                else if(words[0].equals("WRQ")){
                    String directoryFilePathString = "client";
                    Path filePath = Paths.get(directoryFilePathString, words[1]);
                    if(Files.exists(filePath)){
                        sendWrq(words[1]);
                    }
                    else{
                        System.out.println("File does not exists");
                    }

                }
                else if(words[0].equals("DIRQ")){
                    sendDirq();
                }
                else if(words[0].equals("DISC")){
                    sendDisc();
                }
                else{
                    System.out.println("invalid command");
                }
            }
            scanner.close();
        } catch (IOException ignored) {}
    }

    private void sendLogrq(String userName){
        byte[] nameBytes = userName.getBytes();
        byte[] msg = new byte[3+nameBytes.length];
        msg[0] = 0;
        msg[1] = 7;
        System.arraycopy(nameBytes, 0, msg, 2, nameBytes.length);
        msg[msg.length-1] = 0;
        send(msg);
        try {
            synchronized(this){
                System.out.println("waiting");
                this.wait();
                System.out.println("stopped");
            }
        } catch (InterruptedException ignored) {}
    }
    private void sendDelrq(String fileName){
        byte[] nameBytes = fileName.getBytes();
        byte[] msg = new byte[3+nameBytes.length];
        msg[0] = 0;
        msg[1] = 8;
        System.arraycopy(nameBytes, 0, msg, 2, nameBytes.length);
        msg[msg.length-1] = 0;
        send(msg);
        try {
            synchronized(this){
                System.out.println("waiting");
                this.wait();
                System.out.println("stopped");
            }
        } catch (InterruptedException ignored) {}
    }
    private void sendRrq(String fileName){
        byte[] nameBytes = fileName.getBytes();
        byte[] msg = new byte[3+nameBytes.length];
        msg[0] = 0;
        msg[1] = 1;
        System.arraycopy(nameBytes, 0, msg, 2, nameBytes.length);
        msg[msg.length-1] = 0; 
        send(msg);
        try {
            synchronized(this){
                System.out.println("waiting");
                this.wait();
                System.out.println("stopped");
            }
        } catch (InterruptedException ignored) {}
    }
    private void sendWrq(String fileName){
        byte[] nameBytes = fileName.getBytes();
        byte[] msg = new byte[3+nameBytes.length];
        msg[0] = 0;
        msg[1] = 2;
        System.arraycopy(nameBytes, 0, msg, 2, nameBytes.length);
        msg[msg.length-1] = 0;
        send(msg);
        try {
            synchronized(this){
                System.out.println("waiting");
                this.wait();
                System.out.println("stopped");
            }
        } catch (InterruptedException ignored) {}
        sendFile(fileName);
    }

    private void sendFile(String fileName){
        String directoryFilePathString = "client";
        Path filePath = Paths.get(directoryFilePathString, fileName);
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            short blockNumber = 1;
            while((blockNumber)*512 < fileBytes.length){
                sendDataPacket(blockNumber, Arrays.copyOfRange(fileBytes, 512*(blockNumber-1), 512*blockNumber));
                blockNumber++;
                try {
                    synchronized(this){
                        System.out.println("waiting");
                        this.wait();
                        System.out.println("stopped");
                    }
                } catch (InterruptedException ignored) {}
            }
            sendDataPacket(blockNumber, Arrays.copyOfRange(fileBytes, 512*(blockNumber-1), fileBytes.length));
            try {
                synchronized(this){
                    System.out.println("waiting");
                    this.wait();
                    System.out.println("stopped");
                }
            } catch (InterruptedException ignored) {}
            if(!successful){
                Files.delete(filePath);
            }
            else{
                System.out.println("WRQ " + fileName + " complete");
            }
            successful = false;
        } catch (IOException e) {}
    }

    private void sendDataPacket(short blockNumber, byte[] data){
        byte[] response = new byte[6+data.length];
        response[0] = 0;
        response[1] = 3;
        response[2] = (byte) ((data.length >> 8) & 0xFF);
        response[3] = (byte) (data.length & 0xFF);
        response[4] = (byte) ((blockNumber >> 8) & 0xFF);
        response[5] = (byte) (blockNumber & 0xFF);
        System.arraycopy(data, 0, response, 6, data.length);
        send(response);
    } 

    private void sendDirq(){
        byte[] msg = new byte[2];
        msg[0] = 0;
        msg[1] = 6;
        send(msg);
        try {
            synchronized(this){
                System.out.println("waiting");
                this.wait();
                System.out.println("stopped");
            }
        } catch (InterruptedException ignored) {}
    }

    private void sendDisc(){
        if(connected){
            byte[] disc_packet={0,10};
            send(disc_packet);
            try {
                synchronized(this){
                    System.out.println("waiting");
                    this.wait();
                    System.out.println("stopped");
                }
            } catch (InterruptedException ignored) {}
            connected = false;
        }
        shouldTerminate = true;
    }

    private void sendAck(short blockNumber){
        byte[] msg = new byte[4];
        msg[0] = 0;
        msg[1] = 4;
        msg[2] = (byte) ((blockNumber >> 8) & 0xFF);;
        msg[3] = (byte) (blockNumber & 0xFF);;
        send(msg);
        try {
            synchronized(this){
                System.out.println("waiting");
                this.wait();
                System.out.println("stopped");
            }
        } catch (InterruptedException ignored) {}
    }

    private void send(byte[] msg){
        try {
            out.write(encdec.encode(msg));
            out.flush();
        } catch (IOException ex) {}
    }
}
