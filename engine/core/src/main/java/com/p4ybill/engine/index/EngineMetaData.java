package com.p4ybill.engine.index;

import com.p4ybill.engine.store.SimpleFileSerializer;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

/**
 * Class contains meta data for the engine like number of indexed documents etc.
 */
public class EngineMetaData extends EngineMetaAbstract<Integer> implements Flushable, Closeable {

    private String filePathToSave;

    public EngineMetaData(){
        this.serializer = new SimpleFileSerializer<>();
        this.metaDataMap = new HashMap<>();
        this.metaDataMap.put(EngineMeta.TOTAL_DOCS_KEY, 0);
        this.metaDataMap.put(EngineMeta.TOTAL_TERMS_KEY, 0);
    }

    public synchronized void updateDocsNumber(){
        int prevVal = this.getNumberOfDocs() + 1;
        this.metaDataMap.replace(EngineMeta.TOTAL_DOCS_KEY, prevVal);
    }

    private int getNumberOfDocs(){
        return this.metaDataMap.get(EngineMeta.TOTAL_DOCS_KEY);
    }

    public synchronized void updateTermsNumber(){
        int prevVal = this.getNumberOfTerms();
        this.metaDataMap.replace(EngineMeta.TOTAL_TERMS_KEY, prevVal++);
    }

    public int getNumberOfTerms(){
        return this.metaDataMap.get(EngineMeta.TOTAL_TERMS_KEY);
    }

    public void setDirToSave(String filePathToSave){
        this.filePathToSave = filePathToSave;
    }

    @Override
    public void close() {
        this.metaDataMap = Collections.emptyMap();
    }

    public void load() {
        if(this.filePathToSave == null){
            throw new IllegalStateException("You have to set the dir in order to flush the metadata");
        }
        this.metaDataMap = this.serializer.load(filePathToSave);
    }

    public Integer getValue(EngineMeta em){
        this.load();

        return metaDataMap.get(em);
    }

    @Override
    public void flush() throws IOException {
        if(this.filePathToSave == null){
            throw new IllegalStateException("You have to set the dir in order to flush the metadata");
        }
        this.serializer.write(filePathToSave, this.metaDataMap);
        this.close();
    }
}
