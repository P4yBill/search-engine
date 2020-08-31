package com.p4ybill.engine.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;

/**
 * @class That contains utility function for the Variable Byte Encoding.
 */
public class VariableByteEncoding {
    public static byte[] encode(int n) {
        if (n == 0) {
            return new byte[]{0};
        }
        int i = (int) (Math.log(n) / Math.log(128)) + 1;
        byte[] rv = new byte[i];
        int j = i - 1;
        do {
            rv[j--] = (byte) (n % 128);
            n /= 128;
        } while (j >= 0);
        rv[i - 1] += 128;
        return rv;
    }

    /**
     * Decodes only the first integer which will be the size of the posting list.
     *
     * @param ch the channel to read the bytes from.
     * @param sizeToAllocate the size that will be allocated for the buffer
     * @return Map.Entry<Integer, Integer> The Key is the decoded integer
     *                                      and the Value is the length in bytes of the encoded integer in VB.
     */
    public static Map.Entry<Integer, Integer> decodeFirstVB(FileChannel ch, int sizeToAllocate) throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(sizeToAllocate);
        int decodedInt = -1;
        int n = 0;
        int counter = 0;
        int rd;
        while ( ((rd = ch.read(buf)) != -1)) {
            // flip the buffer because it was filled by the channel.
            buf.flip();
            for (int i = 0; i < buf.capacity(); i++) {
                counter++;
                byte b = buf.get(i);
                if ((b & 0xff) < 128) {
                    n = 128 * n + b;
                } else {
                    decodedInt = (128 * n + ((b - 128) & 0xff));
                    break;
                }
            }
            // clear the buffer and prepare it for the next read from channel.
            buf.clear();

            if(decodedInt != -1){
                break;
            }

        }
        return Map.entry(decodedInt, counter);
    }
}
