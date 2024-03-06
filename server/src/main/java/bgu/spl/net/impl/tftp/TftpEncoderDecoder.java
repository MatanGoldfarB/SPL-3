package bgu.spl.net.impl.tftp;

import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private int dataSize = 0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        //
        pushByte(nextByte);
        if (len >= 2) {
            if(bytes[1] == 3){
                if(len==4){
                    dataSize = nextByte;
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
            else if(nextByte == 0){
                byte[] copy = Arrays.copyOfRange(bytes, 0, len);
                len = 0;
                return copy;
            }
        }
        return null; //not a line yet
    }

    @Override
    public byte[] encode(byte[] message) {
        //TODO: implement this
        return null;
    }
        private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }

    public static void main(String[] args) {
        // Create a new ByteArrayOutputStream
        TftpEncoderDecoder dec = new TftpEncoderDecoder();
        byte[] bytes = null;
        bytes = dec.decodeNextByte((byte)0x000);
        System.out.println(bytes);
        bytes = dec.decodeNextByte((byte)0x003);
        System.out.println(bytes);
        bytes = dec.decodeNextByte((byte)0x000);
        System.out.println(bytes);
        bytes = dec.decodeNextByte((byte)0x003);
        System.out.println(bytes);
        bytes = dec.decodeNextByte((byte)0x000);
        System.out.println(bytes);
        bytes = dec.decodeNextByte((byte)0x001);
        System.out.println(bytes);
        bytes = dec.decodeNextByte((byte)0x0D7);
        System.out.println(bytes);
        bytes = dec.decodeNextByte((byte)0x0A6);
        System.out.println(bytes);
        bytes = dec.decodeNextByte((byte)0x095);
        System.out.println(bytes);
        for(byte b : bytes){
            System.out.println(b);
        }
        bytes = dec.decodeNextByte((byte)0x000);
        System.out.println(bytes);

    }
}
