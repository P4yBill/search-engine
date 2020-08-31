package com.p4ybill.engine.search;

import com.p4ybill.engine.index.IndexManager;
import com.p4ybill.engine.queryparser.ScoreDocument;
import com.p4ybill.engine.store.EngineIndexPB;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.*;

public class ScoreSearcher {
    private IndexManager im;

    public ScoreSearcher(IndexManager im){
        this.im = im;
    }

    public List<ScoreDocument> search(String query) throws IOException {

        FileInputStream inPostings = new FileInputStream(Objects.requireNonNull(this.im.getPostingsFilePath()));
        FileInputStream inLexicon = new FileInputStream(Objects.requireNonNull(this.im.getLexiconFilePath()));
        FileChannel postingsChannel = inPostings.getChannel();
        FileChannel lexiconChannel = inLexicon.getChannel();

        // Get tokenized terms of the query using the same tokenizer
        // as the one that is used in Parser.
        List<String> terms = im.getParserTokenizer().tokenize(query);

        List<ScoreDocument> scores = new ArrayList<>();

        long numberOfDocuments = this.im.getNumberOfDocuments();

        // Iterate each term and compute the score of the docs.
        for(String term : terms){
            // Reset the position of the channel for each term
            lexiconChannel.position(0);
            postingsChannel.position(0);

            // TODO: Parallelize the computation of scores after finding the termPostingList
            EngineIndexPB.PostingList termPostingList = this.im.getPostingList(term, postingsChannel, lexiconChannel);

            // Term was not found
            if(termPostingList == null){
                continue;
            }

            double docFreq = termPostingList.getPostingsCount();
            // idf = Log(N/df)
            double idf = Math.log10(((double) numberOfDocuments) / docFreq);

            List<EngineIndexPB.PostingList.Posting> postingList = termPostingList.getPostingsList();
            for (EngineIndexPB.PostingList.Posting posting : postingList) {

                double wfTd = posting.getWeight();
                ScoreDocument docCompare = new ScoreDocument(posting.getDocId());

                if (scores.contains(docCompare)) {
                    docCompare = scores.get(scores.indexOf(docCompare));
                    docCompare.setScore(docCompare.getScore() + (idf * wfTd));
                } else {
                    docCompare.setScore((idf * wfTd));

                    scores.add(docCompare);
                }
            }
        }

        inPostings.close();
        inLexicon.close();
        postingsChannel.close();
        lexiconChannel.close();

        return scores;
    }

}
