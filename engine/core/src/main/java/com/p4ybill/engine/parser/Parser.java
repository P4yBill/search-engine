package com.p4ybill.engine.parser;

import com.p4ybill.engine.utils.EngineUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private final Tokenizer<List<String>> tokenizer;

    public Parser(){
        tokenizer = new EnhancedTokenizer(Arrays.asList(EngineUtils.STOP_LIST));
    }

    /**
     * Constructs a string with the terms of the specified file
     * and uses the tokenizer to get a List of analyzed terms.
     *
     * @param sFile string the path of the file
     * @return List containing the analyzed terms to be indexed.
     *
     * @throws IOException
     */
    public List<String> parseFile(String sFile) throws IOException {
        StringBuilder sb = new StringBuilder();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(sFile));
        String sLine = "";
        while ((sLine = bufferedReader.readLine()) != null) {
            sb.append(sLine).append(" ");
        }

        return tokenize(sb.toString());
    }

    public List<String> tokenize(String sLine) {
        return tokenizer.tokenize(sLine);
    }

    public Tokenizer<List<String>> getTokenizer(){
        return this.tokenizer;
    }
}
