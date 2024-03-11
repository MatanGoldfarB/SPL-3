package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Scanner;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.net.Socket;


public class KeyboardThread implements Runnable {
    private volatile boolean shouldTerminate = false;
    private final Socket sock;
    private BufferedOutputStream out;
    private final MessageEncoderDecoder<byte[]> encdec;

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
                String[] words = userInput.split("\\s+");
                if(words[0].equals("LOGRQ") && words.length==2){
                    sendLogrq(words[1]);
                }
                else if(words[0].equals("DELRQ") && words.length==2){
                    sendDelrq(words[1]);
                }
                else if(words[0].equals("RRQ") && words.length==2){
                    sendRrq(words[1]);
                }
                else if(words[0].equals("WRQ") && words.length==2){
                    sendWrq(words[1]);
                }
                else if(words[0].equals("DIRQ") && words.length==1){
                    sendDirq();
                }
                else if(words[0].equals("DISC") && words.length==1){
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
                this.wait();
            }
        } catch (InterruptedException ignored) {}
    }
    private void sendDelrq(String fileName){
        System.out.println(fileName);
    }
    private void sendRrq(String fileName){
        System.out.println(fileName);
    }
    private void sendWrq(String fileName){
        System.out.println(fileName);
    }
    private void sendDirq(){
    }
    private void sendDisc(){
    }

    private void send(byte[] msg){
        try {
            out.write(encdec.encode(msg));
            out.flush();
        } catch (IOException ex) {}
    }
}
