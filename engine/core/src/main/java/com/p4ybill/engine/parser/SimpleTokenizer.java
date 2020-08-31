package com.p4ybill.engine.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class SimpleTokenizer implements Tokenizer<List<String>>{
    private final String DELIMITERS = " .!?,\t\n;()[]{}:\"-/";

    public List<String> tokenize(String sLine) {
        List<String> fileTermList = new ArrayList<>();

        StringTokenizer st = new StringTokenizer(sLine, this.DELIMITERS);

        String sToken = "";
//        st.countTokens();
        while (st.hasMoreTokens()) {
            sToken = st.nextToken();
            sToken = sToken.toLowerCase();
            fileTermList.add(sToken);
        }

        return fileTermList;
    }
}
