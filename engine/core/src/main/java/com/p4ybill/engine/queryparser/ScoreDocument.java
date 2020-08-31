package com.p4ybill.engine.queryparser;

import java.util.Objects;

public class ScoreDocument implements Comparable<ScoreDocument>{
    private int docId;
    private double score;

    public ScoreDocument(int docId){
        this.docId = docId;
        this.score = 0;
    }

    public int getDocId() {
        return docId;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public int compareTo(ScoreDocument o) {
        if(this.score > o.score){
            return -1;
        }else if(this.score < o.score){
            return 1;
        }else{
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoreDocument that = (ScoreDocument) o;
        return docId == that.docId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(docId);
    }
}
