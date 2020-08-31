package com.p4ybill.engine.store;

public interface FileSerializer<T> {
    /**
     * Serializes specified object to a given file.
     *
     * @param sFile is the path of the file
     * @param obj is the object to serialize
     * @return boolean true if file was written successfully
     */
    boolean write(String sFile, T obj);

    /**
     * Loads object from a given file.
     *
     * @param sFile the path of the file
     * @return loaded object from the specified file
     */
    T load(String sFile);
}
