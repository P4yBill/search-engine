package com.p4ybill.engine.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
/**
 *
 */
public class EnhancedTokenizer implements Tokenizer<List<String>>{
    private final String DELIMITERS = " .!?-',\t\n;()[]{}:\"-/";
    private List<String> stopList;
    private PorterStemmer stemmer;

    public EnhancedTokenizer(){
        stopList = null;
        this.stemmer = new PorterStemmer();
    }

    /**
     * @param stopList List<String> If null means that no stop list will be used
     */
    public EnhancedTokenizer(List<String> stopList){
        this.stopList = stopList;
        this.stemmer = new PorterStemmer();
    }

    public List<String> tokenize(String sLine) {
        List<String> fileTermList = new ArrayList<>();

        StringTokenizer st = new StringTokenizer(sLine, this.DELIMITERS);

        String sToken = "";

        while (st.hasMoreTokens()) {
            sToken = st.nextToken();
            sToken = sToken.toLowerCase();
            // if its not a number, stem the term
            if(!sToken.matches(".*\\d.*")){
                sToken = this.getStemmedToken(sToken);
            }
            fileTermList.add(sToken);
        }

        // remove terms that are in the stop list.
        if(stopList != null){
            fileTermList.removeAll(stopList);
        }

        return fileTermList;
    }

    /**
     * Stems the specified token. Thread safe.
     * @param sToken
     * @return
     */
    private synchronized String getStemmedToken(String sToken){
        char[] charArr = sToken.toCharArray();
        this.stemmer.add(charArr, charArr.length);
        this.stemmer.stem();
        return this.stemmer.toString();
    }

    /**
     * Setter for current stopList that will be used in tokenization.
     *
     * @param stopList If null means that no stoplist will be used
     */
    public void setStopList(List<String> stopList) {
        this.stopList = stopList;
    }

}
