package com.p4ybill.engine.search;

import com.p4ybill.engine.index.LexiconArray;
import com.p4ybill.engine.store.EngineIndexPB;
import com.p4ybill.engine.utils.VariableByteEncoding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @class for searching the term in the lexicon file with binary search
 * and then retrieving from postings file the posting list
 */
public class TermPostingSearchBS {

    private final int TERM_BUFFER_SIZE = 128;
    private final int BUFFER_SIZE_FIRST_VB = 128;

    private FileChannel postingFChannel;
    private FileChannel lexiconFChannel;
    private LexiconArray lexiconArray;
    private final Charset cs = StandardCharsets.UTF_8;

    public TermPostingSearchBS(LexiconArray la, FileChannel postingChannel, FileChannel lexiconChannel) {
        this.lexiconArray = la;
        this.postingFChannel = postingChannel;
        this.lexiconFChannel = lexiconChannel;
    }

    /**
     * Searches the term in the index and returns its posting list if the term exists.
     *
     * @param term The term to search in the index.
     * @return The posting list of the specified term. If the term does not exists, null is returned.
     */
    public EngineIndexPB.PostingList search(String term) {
        TermRecordResult trr = this.binarySearch(term);
        if (trr != null) {
            if (!trr.getTerm().equals("")) {
                return this.getPosting(trr);
            }
        }

        return null;
    }

    /**
     * Looks up for the posting list based on the record of the Lexicon array.
     *
     * @param trr TermRecordResult a record of the Lexicon array
     * @return the term's posting list.
     */
    private EngineIndexPB.PostingList getPosting(TermRecordResult trr) {
        int offsetPostingList = trr.getLexiconArrayRecord().getPostingOffset();

        EngineIndexPB.PostingList postingList = null;
        try {
            int messageSerializedSize = this.getLimitOfPostingList(offsetPostingList);
            ByteBuffer messageBuffer = ByteBuffer.allocateDirect(messageSerializedSize);
            // read posting list from postings file into buffer and parse protobuf message based on buffer.
            if (this.postingFChannel.read(messageBuffer) != -1) {
                byte[] messageBytes = new byte[messageSerializedSize];

                messageBuffer.flip();
                messageBuffer.get(messageBytes);
                postingList = EngineIndexPB.PostingList.parseFrom(messageBytes);
                messageBuffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return postingList;
    }

    /**
     * Sets the offset in the postings file in position where the posting list message start
     * discarding the beginning VB code that represent the size of the list.
     *
     * @param offsetPostingList starting byte offset of the posting list with its size prefixed.
     * @return int The size of the posting list. If there was and IO problem, -1 is return instead.
     */
    private int getLimitOfPostingList(int offsetPostingList) {
        try {
            this.postingFChannel.position(offsetPostingList);

            Map.Entry<Integer, Integer> messageSizesEntry = VariableByteEncoding.decodeFirstVB(postingFChannel, BUFFER_SIZE_FIRST_VB);

            // we reset the offset of the channel to the starting one of the whole list,
            // plus the byte length of the VB encoded size of the posting list.
            this.postingFChannel.position(offsetPostingList + messageSizesEntry.getValue());

            return messageSizesEntry.getKey();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Find the term with binary search on Lexicon Array.
     *
     * @param term the term to look for.
     * @return TermRecordResult
     */
    private TermRecordResult binarySearch(String term) {
        ByteBuffer termBuf = ByteBuffer.allocateDirect(TERM_BUFFER_SIZE);
        int l = 0, r = lexiconArray.getLexiconArraySize() - 1;
        while (l <= r) {
            int m = l + (r - l) / 2;

            // Check if term is present at mid
            TermRecordResult middleTrr = this.getTermFromFile(m, termBuf);

            if (middleTrr.getTerm().equals(term))
                return middleTrr;

            // If searched term is greater, ignore left half
            if (middleTrr.getTerm().compareTo(term) < 0)
                l = m + 1;

                // If term is smaller, ignore right half
            else
                r = m - 1;
        }

        // if we reach here, then element was
        // not present
        return null;
    }

    /**
     * Opens the lexicon file and looks for a term based on the termOffset.
     *
     * @param pos position term offset in a record of the Lexicon Array.
     * @return TermRecordResult
     */
    private TermRecordResult getTermFromFile(int pos, ByteBuffer buf) {
        EngineIndexPB.LexiconArrayItem termArrayRecord = this.lexiconArray.getLexiconTermRecord(pos);

        int offsetInFile = termArrayRecord.getTermOffset();
        StringBuilder term = new StringBuilder();

        try {
            // set the position in channel based on the offset of the term that we want to look for.
            this.lexiconFChannel.position(offsetInFile);
            boolean termFound = false;

            int rd;
            while ((rd = this.lexiconFChannel.read(buf)) != -1) {
                // flip the buffer so its ready for reading by decoder.
                buf.flip();

                CharBuffer chbuf = this.cs.decode(buf);

                for (int i = 0; i < chbuf.length(); i++) {
                    char readChar = chbuf.get(i);
                    if (readChar == '|') { // read until the end of the term
                        termFound = true;
                        break;
                    }

                    term.append(readChar);
                }

                // clear the buffer and prepare it for the next characters for the term
                // or for another term if this term was found
                buf.clear();

                if (termFound) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return new TermRecordResult(termArrayRecord, term.toString());
    }

    /**
     * Class the represents a matched term with a record in the Lexicon Array.
     */
    private class TermRecordResult {
        private EngineIndexPB.LexiconArrayItem lexiconArrayRecord;
        private String term;

        public TermRecordResult(EngineIndexPB.LexiconArrayItem lai, String term) {
            this.lexiconArrayRecord = lai;
            this.term = term;
        }

        public EngineIndexPB.LexiconArrayItem getLexiconArrayRecord() {
            return lexiconArrayRecord;
        }

        public String getTerm() {
            return term;
        }
    }
}
