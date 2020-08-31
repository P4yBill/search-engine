package com.p4ybill.engine.utils;

public class EngineUtils {
    // only AND operator is supported
    public static final String[] BOOL_OPERATORS = {"AND"};

    public static final String[] STOP_LIST = {"a", "an", "and", "are", "as", "at", "be", "by", "for"
            , "from", "has", "he", "in", "is", "it", "its", "of", "on", "that", "the", "to", "was"
            , "were", "will", "with"};

    public static final String INDEX_DIRECTORY_NAME = "_00index";
    public static final String MAPPER_FILE_NAME = "mapperIdFiles.ser";
    public static final String LEXICON_FILE_NAME = "lexicon";
    public static final String LEXICON_ARRAY = "lexiconArray.ser";
    public static final String POSTINGS_FILE_NAME = "postings.ser";
    // we might also want to provide this data with a xml file, for readability etc.
    public static final String META_DATA_FILE = "metadata.ser";
}
