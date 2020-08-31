package com.p4ybill.engine.store;

import com.google.protobuf.MessageLite;
import java.io.*;
import java.util.List;

/**
 * @Class ProtobufSerializerUtils provides utility functions regarding protobuf Messages
 */
public class ProtobufSerializerUtils {
    public static <T extends MessageLite> void writeListDelimited(List<T> messageList, FileOutputStream fos) throws IOException {
        for(T message : messageList){
            message.writeDelimitedTo(fos);
        }
        fos.close();
    }
}
