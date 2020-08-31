package com.p4ybill.engine.parser;

public interface Tokenizer<T> {
    T tokenize(String sLine);
}
