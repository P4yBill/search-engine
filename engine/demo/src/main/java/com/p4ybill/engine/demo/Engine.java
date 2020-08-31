package com.p4ybill.engine.demo;

import com.p4ybill.engine.index.*;
import com.p4ybill.engine.queryparser.QueryParser;
import com.p4ybill.engine.queryparser.QueryResult;
import com.p4ybill.engine.queryparser.ScoreDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Engine {
    private File dir;
    private QueryParser queryParser;
    private IndexManager im;

    public Engine(String dirName) {
        File dir = new File(dirName);
        if(!dir.isDirectory() || !dir.exists()){
            throw new IllegalStateException("Directory : " + dirName + " does not exists or it is not a directory");
        }
        this.dir = dir;
        this.im = new IndexManager(dir);
        this.queryParser = new QueryParser(im);

    }

    public void indexDocs() throws IOException {
        this.indexFiles();
        this.im.save();
    }

    private void indexFiles(){
        this.walkingFiles(this.dir.listFiles());
    }

    private void walkingFiles(File[] files){
        Stream.of(files).parallel().forEach(f -> {
            if(f.isDirectory()){
                walkingFiles(f.listFiles());
            }else{
                this.processFile(f);
            }
        });

    }

    private void processFile(File file){
        if (this.isTextFile(file)) {
            im.addDocument(file);
        }
    }

    public QueryResult query(String queryString) throws IOException {
        List<ScoreDocument> res = queryParser.query(queryString);
        Map<Integer, String> fileNames = this.im.getFileNames(res);

        return new QueryResult(res, fileNames);
    }

    /**
     * Returns true if the file is a .txt file.
     *
     * @param file File
     * @return boolean
     */
    private boolean isTextFile(File file) {
        return file.getName().endsWith(".txt");
    }

    public boolean isAlreadyIndexed(){
        return this.im.hasBeenIndexed();
    }

    public void load(){
        this.im.load();
    }
}
