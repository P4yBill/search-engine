package com.p4ybill.engine.index;

import com.p4ybill.engine.parser.Parser;
import com.p4ybill.engine.parser.Tokenizer;
import com.p4ybill.engine.queryparser.ScoreDocument;
import com.p4ybill.engine.store.EngineIndexPB;
import com.p4ybill.engine.utils.EngineUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IndexManager {
    private File dir;
    private Parser parser;
    private PositionalIndex posIndex;
    private MapDocid2Files mapperDocFiles;
    private int docId;
    private EngineMetaData engineMeta;

    public IndexManager(File dir) {
        this.dir = dir;
        this.posIndex = new PositionalIndex();
        this.mapperDocFiles = new MapDocid2Files();
        this.parser = new Parser();
        this.engineMeta = new EngineMetaData();
        this.docId = 0;
    }

    /**
     * Saves all the required files regarding indexing.
     *
     * @throws IOException
     */
    public void save() throws IOException {
        this.createIndexedFolder();
        this.flushMapper();
        this.flushMetaData();
        this.saveIndex();
    }

    /**
     * Saves the Positional Index to disk.
     *
     * @throws IOException
     */
    protected void saveIndex() throws IOException {
        this.posIndex.writeIndex(this.getIndexFolderPath(),
                true
        );
    }

    /**
     * Loads/sets the required files for indexing.
     */
    public void load(){
        try {
            this.engineMeta.setDirToSave(this.getMetadataFilePath());
            this.mapperDocFiles.setDirToSave(new File(this.getMapperIdFilePath()));
            this.posIndex.loadLexiconArray(this.getLexiconArrayFilePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return true if the specified file is already indexed.
     */
    public boolean hasBeenIndexed() {
        return this.hasIndexedFiles();
    }

    /**
     * Checks if the specified folder to be indexed, has already been indexed
     * @return true is the folder has the files required for indexing.
     */
    private boolean hasIndexedFiles() {
        File postingsFile = new File(this.getPostingsFilePath());
        File lexiconFile = new File(this.getLexiconFilePath());
        File lexiconArrFile = new File(this.getLexiconArrayFilePath());
        File metaDataFile = new File(this.getMetadataFilePath());
        File mapIdFile = new File(this.getMapperIdFilePath());

        return postingsFile.exists() && lexiconFile.exists() && lexiconArrFile.exists() && metaDataFile.exists()
                && mapIdFile.exists();
    }

    /**
     * Creates index folder in the given directory to index, if it does not exist.
     *
     * @throws IOException
     */
    public void createIndexedFolder() throws IOException {
        String indexDirStringPath = this.getIndexFolderPath();
        File indexDir = new File(indexDirStringPath);
        indexDir.mkdirs();
    }

    /**
     * @param term string the term to find.
     * @param postingsChannel FileChannel of the postings file
     * @param lexiconChannel FileChannel of the lexicon File
     * @return the posting list of the given term.
     */
    public EngineIndexPB.PostingList getPostingList(String term, FileChannel postingsChannel, FileChannel lexiconChannel) {
        return this.posIndex.getPostingList(term, postingsChannel, lexiconChannel);
    }


    /**
     * Iterate through a list of terms and uses the {@link #getPostingList(String, FileChannel, FileChannel)}
     * to retrieve the posting list of each term.
     *
     * @param terms A list of terms to look up. Uses
     * @return A list of posting lists of the given terms
     */
    public List<EngineIndexPB.PostingList> getTermsPostingList(List<String> terms) {
        List<EngineIndexPB.PostingList> termsPostingLists = new ArrayList<>();

        try {
            FileInputStream inPostings = new FileInputStream(Objects.requireNonNull(this.getPostingsFilePath()));
            FileInputStream inLexicon = new FileInputStream(Objects.requireNonNull(this.getLexiconFilePath()));
            FileChannel postingsChannel = inPostings.getChannel();
            FileChannel lexiconChannel = inLexicon.getChannel();

            for (String term : terms) {
                termsPostingLists.add(this.posIndex.getPostingList(term, postingsChannel, lexiconChannel));
            }

            postingsChannel.close();
            lexiconChannel.close();
            inPostings.close();
            inLexicon.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return termsPostingLists;
    }

    /**
     * Thread safe method that returns a unique identifier for a document.
     * @return integer representing the doc id.
     */
    private synchronized int getIncreasedDocId() {
        return ++this.docId;
    }

    /**
     * Adds the specified file to the index and returns the identifier for the file.
     *
     * @param fileToIndex file to index
     * @return The id of the document if successfully added.
     *          If any IO problem occurs, -1 is returned instead.
     */
    public int addDocument(File fileToIndex) {
        int docId = this.getIncreasedDocId();
        List<String> list = null;
        try {
            this.engineMeta.updateDocsNumber();
            String fileCanonicalPath = fileToIndex.getCanonicalPath();

            list = this.parser.parseFile(fileCanonicalPath);

            this.posIndex.insertTermList(list, docId);

            this.mapperDocFiles.add(docId, fileCanonicalPath);
            return docId;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Gets the file names for all the score documents based on document identifiers
     *
     * @param listDocId a list with score documents.
     * @return map containing document ids as keys and as values the file names of the documents
     */
    public Map<Integer, String> getFileNames(List<ScoreDocument> listDocId) {
        return this.mapperDocFiles.getList(listDocId);
    }


    private String getIndexFolderPath() throws IOException {
        return this.dir.getCanonicalFile() + "\\" + EngineUtils.INDEX_DIRECTORY_NAME;
    }

    public String getLexiconFilePath() {
        try {
            return this.getIndexFolderPath() + "\\" + EngineUtils.LEXICON_FILE_NAME;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getLexiconArrayFilePath() {
        try {
            return this.getIndexFolderPath() + "\\" + EngineUtils.LEXICON_ARRAY;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getPostingsFilePath() {
        try {
            return this.getIndexFolderPath() + "\\" + EngineUtils.POSTINGS_FILE_NAME;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getMapperIdFilePath() {
        try {
            return this.getIndexFolderPath() + "\\" + EngineUtils.MAPPER_FILE_NAME;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Saves the mapper to disk
     */
    private void flushMapper() {
        try {
            File mapperFile = new File(this.getMapperIdFilePath());

            // Delete file if exists and create new
            if (mapperFile.exists()) {
                mapperFile.delete();
            }
            mapperFile.createNewFile();

            this.mapperDocFiles.setDirToSave(mapperFile);
            this.mapperDocFiles.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String getMetadataFilePath() {
        try {
            return this.getIndexFolderPath() + "\\" + EngineUtils.META_DATA_FILE;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Saves the meta data to disk.
     */
    private void flushMetaData() {
        try {
            String metaFile = this.getMetadataFilePath();
            File mf = new File(metaFile);
            if (mf.exists()) {
                mf.delete();
            }
            mf.createNewFile();

            this.engineMeta.setDirToSave(metaFile);
            this.engineMeta.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the total number of documents.
     * @return an integer that represents total number of documents
     */
    public long getNumberOfDocuments() {
        int nDocs = this.engineMeta.getValue(EngineMetaAbstract.EngineMeta.TOTAL_DOCS_KEY);
        this.engineMeta.close();
        return nDocs;
    }

    /**
     * @return the same tokenizer that is used in the parser.
     */
    public Tokenizer<List<String>> getParserTokenizer() {
        return this.parser.getTokenizer();
    }
}
