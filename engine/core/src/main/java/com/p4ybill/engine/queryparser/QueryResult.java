package com.p4ybill.engine.queryparser;

import java.util.List;
import java.util.Map;

public class QueryResult {
    private List<ScoreDocument> scoreDocuments;
    private Map<Integer, String> fileNames;

    public QueryResult(List<ScoreDocument> docs, Map<Integer, String> fileNames){
        this.scoreDocuments = docs;
        this.fileNames = fileNames;
    }

    public List<ScoreDocument> getScoreDocuments() {
        return scoreDocuments;
    }

    public Map<Integer, String> getFileNames() {
        return fileNames;
    }
}
