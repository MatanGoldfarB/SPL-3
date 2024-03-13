package bgu.spl.net.impl.tftp;

import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private short dataSize = 0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        //
        pushByte(nextByte);
        if (len >= 2) {
            if(bytes[1] == 3){
                if(len==4){
                    dataSize = (short) ((bytes[2] << 8) | (bytes[3] & 0xFF));
                }
                else if(len == dataSize + 6){
                byte[] copy = Arrays.copyOfRange(bytes, 0, len);
                len = 0;
                dataSize = 0;
                return copy;
                }
            }
            else if(bytes[1] == 4){
                if(len==4){
                    len = 0;
                    return Arrays.copyOfRange(bytes, 0, 4);
                }
            }
            else if(bytes[1] == 6 || bytes[1] == 10){
                len = 0;
                return Arrays.copyOfRange(bytes, 0, 2);
            }
            else if(bytes[1] == 5){
                if(len>4 && nextByte==0){
                    byte[] copy = Arrays.copyOfRange(bytes, 0, len);
                    len = 0;
                    return copy;
                }
            }
            else if(nextByte == 0){
                byte[] copy = Arrays.copyOfRange(bytes, 0, len);
                len = 0;
                return copy;
            }
        }
        return null; //not a message yet
    }

    @Override
    public byte[] encode(byte[] message) {
        //TODO: implement this
        return message;
    }
    
    private void pushByte(byte nextByte) {
    if (len >= bytes.length) {
        bytes = Arrays.copyOf(bytes, len * 2);
    }
    bytes[len++] = nextByte;
    }
}
