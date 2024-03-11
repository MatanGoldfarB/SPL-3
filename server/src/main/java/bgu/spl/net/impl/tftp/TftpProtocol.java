package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private ConnectionsImpl<byte[]> connections;
    private boolean shouldTerminate = false;
    private String writeFile;
    private String readFile;
    
    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this
        this.connectionId = connectionId;
        this.connections = (ConnectionsImpl<byte[]>)connections;
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        String msgData;
        if(message.length>=2){
            if(message[1]==7){
                //LOGRQ
                msgData = new String(message, 2, message.length-2, StandardCharsets.UTF_8);
                logrq(msgData);
            }
            else if(!connections.getHandler(this.connectionId).getUserName().equals("")){
                if(message[1]==8){
                    //DELRQ
                    msgData = new String(message, 2, message.length-2, StandardCharsets.UTF_8);
                    String sanitizedMsgData = msgData.replaceAll("\0", "");
                    delrq(sanitizedMsgData);
                }
                else if(message[1]==1){
                    //RRQ
                    msgData = new String(message, 2, message.length-2, StandardCharsets.UTF_8);
                    String sanitizedMsgData = msgData.replaceAll("\0", "");
                    rrq(sanitizedMsgData);
                }
                else if(message[1]==2){
                    //WRQ
                    msgData = new String(message, 2, message.length-2, StandardCharsets.UTF_8);
                    String sanitizedMsgData = msgData.replaceAll("\0", "");
                    wrq(sanitizedMsgData);
                }
                else if(message[1]==6){
                    //DIRQ
                    dirq();
                }
                else if(message[1]==3){
                    //DATA
                    handleDataPacket(message);
                }
                else if(message[1]==4){
                    //ACK
                    handleACKPacket(message);
                }
                else if(message[1]==10){
                    //DISC
                    disc();
                }
            }
            else{
                byte[] response =  error((byte)6, "log in first");
                connections.send(connectionId, response);
            }
        }
        else{
            //Undeifined packet
            byte[] response = error((byte)0, "Undefined Request");
            connections.send(connectionId, response);
        }
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        return shouldTerminate;
    }
    
    private void logrq(String userName){
        byte[] response;
        // Checks if wasn't logged before
        if(!connections.userNameExist(userName)){
            connections.getHandler(this.connectionId).setUserName(userName);
            byte[] blockNumber = {0,0};
            response =  ack(blockNumber);
        }
        else{
            response = error((byte)7, "user already logged in");
        }
        connections.send(connectionId, response);
    }

    private void rrq(String fileName){
        byte[] response;
        String directoryFilePathString = "server" + File.separator + "Flies";
        Path filePath = Paths.get(directoryFilePathString, fileName);
        if(Files.exists(filePath)){
            try {
                byte[] fileBytes = Files.readAllBytes(filePath);
                int min =Math.min(512, fileBytes.length);
                readFile = fileName;
                sendDataPacket((short)1, Arrays.copyOfRange(fileBytes, 0, min));
            } catch (IOException e) {
                response = error((byte)2, "file can't be read");
                connections.send(connectionId, response);
            }
        }
        else{
            response = error((byte)1, "file doesn't exist");
            connections.send(connectionId, response);
        }
    }
    private void wrq(String fileName){
        byte[] response;
        String directoryFilePathString = "server" + File.separator + "Flies";
        Path filePath = Paths.get(directoryFilePathString, fileName);
        if(!Files.exists(filePath)){
            response = error((byte)5, "file exists");
            connections.send(connectionId, response);
        }
        else{
            byte[] blockNumber = {0,0};
            writeFile = fileName;
            try {
                // Create the file
                Files.createFile(filePath);
            } catch (IOException e) {
                System.err.println("Failed to create file: " + e.getMessage());
            }
            connections.send(connectionId, ack(blockNumber));
        }
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
        connections.send(connectionId, response);
    } 

    private void handleDataPacket(byte[] msg){
        try {
            System.out.println(msg.length);
            String directoryFilePathString = "server" + File.separator + "Flies";
            Path filePath = Paths.get(directoryFilePathString, writeFile);
            byte[] fileData = Arrays.copyOfRange(msg, 6, msg.length);;
            Files.write(filePath, fileData, StandardOpenOption.APPEND);
            byte[] blockNumber = {msg[4], msg[5]};
            connections.send(connectionId, ack(blockNumber));
        } catch (Exception e) {
            System.err.println("Error appending to a file: " + e.getMessage());
        }
        if(msg.length<518){
            bcast((byte)1, writeFile);
            writeFile=null;
        }
    }

    private int handleACKPacket(byte[] msg){
        byte[] response;
        short blockNumber = (short) ((msg[2] << 8) | (msg[3] & 0xFF));
        String directoryFilePathString = "server" + File.separator + "Flies";
        Path filePath = Paths.get(directoryFilePathString, readFile);
        if(Files.exists(filePath)){
            try {
                byte[] fileBytes = Files.readAllBytes(filePath);
                int min =Math.min(512*(blockNumber), fileBytes.length);
                if(512*(blockNumber-1)>fileBytes.length){
                    bcast((byte)1, readFile);
                    return 0;
                }
                sendDataPacket((short)(blockNumber+1), Arrays.copyOfRange(fileBytes, 512*(blockNumber-1), min));
            } catch (IOException e) {
                response = error((byte)2, "file can't be read");
                connections.send(connectionId, response);
            }
        }
        else{
            response = error((byte)1, "file doesn't exist");
            connections.send(connectionId, response);
        }
        return 0;
    }

    private byte[] ack(byte[] blockNumber){
        byte[] response = {0, 4, blockNumber[0],blockNumber[1]};
        return response;
    }

    private byte[] error(byte errorCode, String errorMsg){
        byte[] msgBytes = errorMsg.getBytes();
        byte[] response = new byte[msgBytes.length+5];
        response[0]=0;
        response[1]=5;
        response[2]=0;
        response[3]=errorCode;
        response[response.length-1]=0;
        System.arraycopy(msgBytes, 0, response, 4, msgBytes.length);
        return response;
    }
    
    private void dirq(){
        String directoryFilePathString = "server" + File.separator + "Flies";
        Path folderPath = Paths.get(directoryFilePathString);
        ArrayList<byte[]> filesNames=new ArrayList<byte[]>();
        try {
            // Iterate over the files in the directory
            Files.list(folderPath).forEach(filePath -> {
                // Get the file name from the file path
                String fileName = filePath.getFileName().toString();
                filesNames.add(fileName.getBytes());
                filesNames.add(new byte[]{0});
            });
        // Convert ArrayList<byte[]> to byte[]
        int totalLength = filesNames.stream().mapToInt(arr -> arr.length).sum();
        byte[] dataBytes = new byte[totalLength];
        int index = 0;
        for (byte[] byteArray : filesNames) {
            System.arraycopy(byteArray, 0, dataBytes, index, byteArray.length);
            index += byteArray.length;
        }
        short blockNumber = 1;
        while(blockNumber*512<dataBytes.length){
            sendDataPacket(blockNumber, Arrays.copyOfRange(dataBytes, 512*(blockNumber-1), 512*blockNumber));
            blockNumber++;
        }
        sendDataPacket(blockNumber, Arrays.copyOfRange(dataBytes, 512*(blockNumber-1), dataBytes.length));
        } catch (IOException e) {
            e.printStackTrace(); // Handle any potential IO exceptions
        }
    }
    
    private void delrq(String fileName){
        byte[] response;
        String directoryFilePathString = "server" + File.separator + "Flies";
        Path filePath = Paths.get(directoryFilePathString, fileName);
        if(Files.exists(filePath)){
            try {
                Files.delete(filePath);
                byte[] blockNumber = {0,0};
                response = ack(blockNumber);
                connections.send(connectionId, response);
                bcast((byte)0,fileName);
            } catch (IOException e) {
                response =  error((byte)2, "file can't be deleted");
            }
        }
        else{
            response =  error((byte)1, "file doesn't exist");
            connections.send(connectionId, response);
        }
    }
    private void bcast(byte b, String fileName){
        byte[] response = new byte[4+fileName.length()];
        response[0] = 0;
        response[1] = 9;
        response[2] = b;
        response[response.length-1] = 0;
        byte[] fileBytes = fileName.getBytes();
        System.arraycopy(fileBytes, 0, response, 3, fileBytes.length);
        for(Integer id : connections.getKeySet()){
            BlockingConnectionHandler<byte[]> handler = connections.getHandler(id);
            if(!handler.getUserName().equals("")){
                connections.send(id, response);
            }
        }
    }


    private void disc(){
        byte[] blockNumber = {0,0};
        byte[] response =  ack(blockNumber);
        connections.getHandler(connectionId).setUserName("");
        connections.send(connectionId, response);
        connections.disconnect(connectionId);
        shouldTerminate = true;
    }
}
