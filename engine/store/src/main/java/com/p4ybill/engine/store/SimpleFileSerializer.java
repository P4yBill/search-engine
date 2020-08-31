package com.p4ybill.engine.store;

import java.io.*;

/**
 * @class SimpleFileSerializer
 * Writes and loads an object using Java Serialization.
 *
 * @param <T> the type of the object to be serialized.
 */
public class SimpleFileSerializer<T> implements FileSerializer<T> {

    public boolean write(String sFile, T obj) {
        try {
            FileOutputStream   fileOutputStream = new FileOutputStream(sFile, true);
            ObjectOutputStream oos              = new ObjectOutputStream(fileOutputStream);

            oos.writeObject(obj);
            oos.close();
            fileOutputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public T load(String sFile) {
        T obj = null;
        try {
            FileInputStream   fin = new FileInputStream(sFile);
            ObjectInputStream ois = new ObjectInputStream(fin);

            obj = (T) ois.readObject();

            ois.close();
            fin.close();
        }catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return obj;
    }
}
