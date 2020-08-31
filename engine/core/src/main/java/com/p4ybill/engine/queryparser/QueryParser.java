package com.p4ybill.engine.queryparser;

import com.p4ybill.engine.index.IndexManager;
import com.p4ybill.engine.search.ScoreSearcher;
import com.p4ybill.engine.search.SearchBooleanQuery;
import com.p4ybill.engine.store.EngineIndexPB;
import com.p4ybill.engine.utils.EngineUtils;

import java.io.IOException;
import java.util.*;


public class QueryParser {
    private final List<String> queryTokens;
    private IndexManager im;


    public QueryParser(IndexManager im) {
        queryTokens = Arrays.asList(EngineUtils.BOOL_OPERATORS);
        this.im = im;
    }

    /**
     * Checks if the input query(prior to tokenization etc.) provided by the user is a boolean query
     *
     * @param query string the input provided by the user
     * @return boolean True if its a boolean query otherwise false
     */
    private boolean isBoolQuery(String query) {
        List<String> tokens = Arrays.asList(query.split(" "));
        return tokens.stream().anyMatch(queryTokens::contains);
    }

    /**
     * Checked if its a boolean query and calls the right method
     *
     * @param query string submitted query input from the user.
     * @return List<ScoreDocument> A List containing the findings.
     */
    public List<ScoreDocument> query(String query) {
        List<ScoreDocument> scoreDocuments;

        if (isBoolQuery(query)) {
            scoreDocuments = processBool(query);
        } else {
            scoreDocuments = freeTextQuery(query);
        }

        return scoreDocuments;
    }

    /**
     * It is called when the query provided by the user is a boolean query
     *
     * @param query string submitted query input from the user.
     * @return List<ScoreDocument> A List containing the findings.
     */
    private List<ScoreDocument> processBool(String query){
        SearchBooleanQuery searchBooleanQuery = new SearchBooleanQuery();
        List<EngineIndexPB.PostingList.Posting> result = new ArrayList<>();
        String termsWithoutAnd = removeAnd(query);

        List<String> terms = this.im.getParserTokenizer().tokenize(termsWithoutAnd);

        List<EngineIndexPB.PostingList> termsPosting = im.getTermsPostingList(terms);

        result = searchBooleanQuery.intersectMany(termsPosting);
        List<ScoreDocument> scores = new ArrayList<>(result.size());

        for(int i = 0; i< result.size() ; i++){
            EngineIndexPB.PostingList.Posting posting = result.get(i);
            ScoreDocument doc = new ScoreDocument(posting.getDocId());
            scores.add(doc);
        }

        return scores;

    }

    /**
     * It is called when the query provided by the user is a free text query
     *
     * @param query string submitted query input from the user.
     * @return List<ScoreDocument> A List containing the findings.
     */
    private List<ScoreDocument> freeTextQuery(String query){
        ScoreSearcher searcher = new ScoreSearcher(im);
        List<ScoreDocument> scores = new ArrayList<>();

        try {
            scores = searcher.search(query);
            // sort the results by scores in descending order
            if(scores != null){
                Collections.sort(scores);
            }else{
                return new ArrayList<>();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return scores;
    }

    private String removeAnd(String query) {
        return query.replace("AND", "");
    }
}
