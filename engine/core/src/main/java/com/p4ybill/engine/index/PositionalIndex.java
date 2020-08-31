package com.p4ybill.engine.index;

import com.p4ybill.engine.store.EngineIndexPB;
import com.p4ybill.engine.search.TermPostingSearchBS;
import com.p4ybill.engine.utils.EngineUtils;
import com.p4ybill.engine.utils.VariableByteEncoding;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PositionalIndex {
    private final String LEXICON_TERM_DELIMITER = "|";
    private final int LEX_TERM_DELIMITER_LENGTH = LEXICON_TERM_DELIMITER.getBytes().length;

    private Map<String, List<PostingPos>> h;

    private Map<Integer, Double> docNorms;
    private LexiconArray lexiconArray;

    public PositionalIndex() {
        this.h = new TreeMap<>();
        this.docNorms = new ConcurrentHashMap<>();
        this.lexiconArray = new LexiconArray();
    }

    /**
     * Thread safe method that indexes a term with its position in the document, to a doc with a specific identifier.
     *
     * @param sTerm string that represents the term to be indexed.
     * @param docId integer identifier of the doc.
     * @param pos integer term's position in the document
     */
    public synchronized void insert(String sTerm, int docId, int pos) {
        List<PostingPos> postingMap = h.get(sTerm);

        if (postingMap != null) {
            PostingPos doc = new PostingPos(docId);

            if (postingMap.contains(doc)) {
                doc = postingMap.get(postingMap.indexOf(doc));
                doc.addTermPosition(pos);
            } else {
                doc.addTermPosition(pos);
                postingMap.add(doc);
            }
        } else {
            postingMap = new ArrayList<>();
            PostingPos posDoc = new PostingPos(docId);
            posDoc.addTermPosition(pos);

            postingMap.add(posDoc);
            h.put(sTerm, postingMap);
        }
    }

    /**
     * Indexes a list of terms of a document and also computes the norm of the document.
     *
     * @param termList List containing the terms to be indexed.
     * @param docId    the terms from termList will be associated to the document with docId.
     */
    protected void insertTermList(List<String> termList, int docId) {
        Map<String, Integer> tfDocMapper = new HashMap<String, Integer>();
        for (int i = 0; i < termList.size(); i++) {
            String term = termList.get(i);
            if (tfDocMapper.containsKey(termList.get(i))) {
                Integer tftd = tfDocMapper.get(term);
                tfDocMapper.replace(term, ++tftd);
            } else {
                tfDocMapper.put(term, 1);
            }
            insert(term, docId, i);
        }

        docNorms.put(docId, this.computeNorm(tfDocMapper.values()));
    }

    /**
     * Looks up for the norm of a document in the map and returns it.
     *
     * @param docId integer identifier of the document
     * @return double value that represents the norm.
     */
    protected double getNorm(int docId) {
        Double norm = this.docNorms.get(docId);
        return norm == null ? 1 : norm;
    }

    /**
     * Computes the norm
     * @param tftds Collection of all term frequencies in a document.
     * @return double the norm of the doc.
     */
    private double computeNorm(Collection<Integer> tftds) {
        return Math.sqrt(tftds.stream().mapToDouble(x -> Math.pow(x, 2)).sum());
    }

    /**
     * Searches a term in the index.
     *
     * @param term string the term to look in the index
     * @param postingsFc FileChannel of the postings file
     * @param lexiconFc FileChannel of the lexicon file
     * @return the posting list of a term
     */
    public EngineIndexPB.PostingList getPostingList(String term, FileChannel postingsFc, FileChannel lexiconFc) {
        TermPostingSearchBS bs = new TermPostingSearchBS(this.lexiconArray, postingsFc, lexiconFc);

        return bs.search(term);
    }

    /**
     * Creates a file in the specified path.
     *
     * @param filePath string the filepath of the file.
     */
    private void createNewFile(String filePath) {
        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the index to the disk in 3 different files
     * One for the Lexicon's array, one for the lexicon itself and one for the postings.
     *
     * @param indexFilePath  The filepath to save the index to.
     * @param createNewFiles Boolean that specifies if the three files should be overwritten
     *                       This might come in handy in the future.
     * @throws IOException
     */
    protected void writeIndex(String indexFilePath, boolean createNewFiles) throws IOException {
        Set<String> sortedTerms = this.h.keySet();
        String lexiconFileName = indexFilePath + "\\" + EngineUtils.LEXICON_FILE_NAME;
        String lexiconArrayFileName = indexFilePath + "\\" + EngineUtils.LEXICON_ARRAY;
        String postingsFileName = indexFilePath + "\\" + EngineUtils.POSTINGS_FILE_NAME;

        if (createNewFiles) {
            this.createNewFile(lexiconFileName);
            this.createNewFile(lexiconArrayFileName);
            this.createNewFile(postingsFileName);
        }

        FileOutputStream postingsFileFOS = new FileOutputStream(postingsFileName, true);
        FileOutputStream laFOS = new FileOutputStream(lexiconArrayFileName, true);

        PostingComparator postingComparator = new PostingComparator();
        int offsetPostingList = 0;
        int offsetLexiconTerm = 0;
        EngineIndexPB.LexiconArray.Builder lexiconArrayBuilder = EngineIndexPB.LexiconArray.newBuilder();

        FileWriter fw = new FileWriter(lexiconFileName, false);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter lexiconPw = new PrintWriter(bw);

        // For each term, insert the term in the lexicon file, the term's postingList in the postings file
        // and add a record with the offsets in the Lexicon Array.
        for (String term : sortedTerms) {
            List<PostingPos> postingListIndex = this.h.get(term);
            EngineIndexPB.PostingList.Builder postingListB = EngineIndexPB.PostingList.newBuilder();
            List<EngineIndexPB.PostingList.Posting> postingList = new ArrayList<>();

            // iterate every posting, compute the weight based on the norm and add it to the protobuf postingList.
            for (PostingPos posting : postingListIndex) {
                double weight = (1 + Math.log10(posting.getTermFrequency()))
                        / this.getNorm(posting.getDocId());
                postingList.add(
                        EngineIndexPB.PostingList.Posting.newBuilder()
                        .addAllTermPositions(posting.getTermPositionList())
                        .setDocId(posting.getDocId())
                        .setWeight(weight).build()
                );
            }

            int df = postingList.size();

            // DESC sort based on weight
            postingList.sort(postingComparator);
            EngineIndexPB.PostingList postingListBuilded = postingListB.addAllPostings(postingList).build();

            // release resources
            postingList = Collections.emptyList();
            // get the size of the postingList
            int msgSize = postingListBuilded.getSerializedSize();

            // encode posting lists bytes length with vb
            byte[] vbSizeOfMessage = VariableByteEncoding.encode(msgSize);

            // add the length of the posting list to the postings and then the list itself.
            postingsFileFOS.write(vbSizeOfMessage);
            postingListBuilded.writeTo(postingsFileFOS);

            // construct a lexicon array record
            EngineIndexPB.LexiconArrayItem.Builder termArrRecord = EngineIndexPB.LexiconArrayItem.newBuilder();
            termArrRecord.setDocFrequency(df);
            termArrRecord.setPostingOffset(offsetPostingList);
            termArrRecord.setTermOffset(offsetLexiconTerm);
            // add the record to array
            lexiconArrayBuilder.addLexiconItem(termArrRecord.build());

            // add the string term with the delimiter in the lexicon's file
            lexiconPw.print(term + LEXICON_TERM_DELIMITER);

            // update the offsets
            offsetPostingList += (vbSizeOfMessage.length + msgSize);
            offsetLexiconTerm += (LEX_TERM_DELIMITER_LENGTH + term.length());
        }

        // create and save lexicon array, and keep it in memory.
        EngineIndexPB.LexiconArray lexiconArray = lexiconArrayBuilder.build();
        lexiconArray.writeTo(laFOS);
        this.lexiconArray.setLexiconArray(lexiconArray);

        // flush and release resources.
        laFOS.close();
        postingsFileFOS.close();
        lexiconPw.flush();
        lexiconPw.close();
        this.h = Collections.emptyMap();
    }


    protected void loadLexiconArray(String filePath) throws IOException {
        this.lexiconArray.load(filePath);
    }

    private class PostingComparator implements Comparator<EngineIndexPB.PostingList.Posting> {

        @Override
        public int compare(EngineIndexPB.PostingList.Posting o1, EngineIndexPB.PostingList.Posting o2) {
            if (o1.getWeight() > o2.getWeight()) {
                return -1;
            } else if (o1.getWeight() < o2.getWeight()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public static class PostingPos implements IPosting, Comparable<PostingPos> {
        private int docId;
        private List<Integer> termPositions;

        public PostingPos(int docId) {
            this.docId = docId;
            this.termPositions = new Vector<>();
        }

        public int getDocId() {
            return docId;
        }

        public int getTermFrequency() {
            return termPositions.size();
        }

        public void addTermPosition(int pos) {
            termPositions.add(pos);
        }

        public List<Integer> getTermPositionList() {
            return this.termPositions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PostingPos postingPos = (PostingPos) o;

            return docId == postingPos.getDocId();
        }

        @Override
        public int hashCode() {
            return Objects.hash(docId);
        }

        @Override
        public int compareTo(PostingPos o) {
            if (this.getDocId() > o.getDocId()) {
                return 1;
            } else if (this.getDocId() < o.getDocId()) {
                return -1;
            } else {
                return 0;
            }
        }

    }
}
