package com.p4ybill.engine.index;

import com.p4ybill.engine.store.EngineIndexPB;

import java.io.FileInputStream;
import java.io.IOException;

public class LexiconArray {
    EngineIndexPB.LexiconArray lexiconArray;

    protected void setLexiconArray(EngineIndexPB.LexiconArray lexiconArray) {
        this.lexiconArray = lexiconArray;
    }

    public EngineIndexPB.LexiconArrayItem getLexiconTermRecord(int pos) {
        if (pos > this.getLexiconArraySize() - 1) {
            return null;
        }

        return this.lexiconArray.getLexiconItem(pos);
    }

    public int getLexiconArraySize() {
        return this.lexiconArray.getLexiconItemCount();
    }

    public void load(String filePath) throws IOException {
        if (filePath != null) {
            FileInputStream fis = new FileInputStream(filePath);
            this.lexiconArray = EngineIndexPB.LexiconArray.parseFrom(fis);
            fis.close();
        }
    }
}
