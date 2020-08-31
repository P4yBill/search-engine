package com.p4ybill.engine.index;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;
import com.p4ybill.engine.queryparser.ScoreDocument;
import com.p4ybill.engine.store.ProtobufSerializerUtils;
import com.p4ybill.engine.store.DocFileMapperPB;

import java.io.*;
import java.util.*;

public class MapDocid2Files {
    private File mapFile = null;
    private List<DocFileMapperPB.DocFileMapper.Doc> docList;

    public MapDocid2Files() {
        this.docList = new ArrayList<>();
    }

    /**
     * Thread safe method that adds an identifier-filename pair to the map
     *
     * @param docId    integer representing a document identifier
     * @param fileName string that represents the filename of the document.
     */
    public synchronized void add(int docId, String fileName) {
        this.docList.add(DocFileMapperPB.DocFileMapper.Doc.newBuilder()
                .setDocId(docId).setFileName(fileName).build());
    }

    /**
     * Get correspoding file name based on document id.
     *
     * @param docId Identifier of the document.
     * @return String if document is found.
     * null if document was not found or there was an expection.
     */
    public String get(int docId) {
        try {
            FileInputStream fis = new FileInputStream(this.mapFile);
            Parser<DocFileMapperPB.DocFileMapper.Doc> parser = DocFileMapperPB.DocFileMapper.Doc.parser();

            while (true) {
                DocFileMapperPB.DocFileMapper.Doc doc = parser.parseDelimitedFrom(fis);
                // EOF
                if (doc == null) {
                    break;
                } else {
                    doc.getDocId();
                    if (doc.getDocId() == docId) {
                        return doc.getFileName();
                    }
                }

            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Processes each message separately instead of loading the whole mapper in memory.
     *
     * @param listDocId A list containing score documents.
     * @return Map with document identifier and its corresponding filename.
     */
    protected Map<Integer, String> getList(List<ScoreDocument> listDocId) {
        Map<Integer, String> docs = new HashMap<>();
        if (listDocId == null) {
            return docs;
        }
        int counter = listDocId.size();
        try {
            FileInputStream fis = new FileInputStream(this.mapFile);
            Parser<DocFileMapperPB.DocFileMapper.Doc> parser = DocFileMapperPB.DocFileMapper.Doc.parser();
            while (true) {
                DocFileMapperPB.DocFileMapper.Doc doc = parser.parseDelimitedFrom(fis);
                // EOF or all docs were processed
                if (doc == null || counter == 0) {
                    break;
                } else {
                    int docId = doc.getDocId();
                    ScoreDocument docToCompare = new ScoreDocument(docId);
                    if (listDocId.contains(docToCompare)) {
                        docs.put(docId, doc.getFileName());
                        counter--;
                    }
                }

            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return docs;
    }

    public void setDirToSave(File filePath) {
        this.mapFile = filePath;
    }

    /**
     * Saves the mapper to a file and releases resources.
     */
    public void save() throws IOException {
        if (this.mapFile == null) {
            throw new IllegalStateException("You have to set the dir in order to flush the mapper doc id to files");
        }

        this.writeDocs();
        this.docList = Collections.emptyList();
    }

    /**
     * Writes the list of messages to a file.
     *
     * @throws IOException
     */
    private void writeDocs() throws IOException {
        FileOutputStream fos = new FileOutputStream(this.mapFile, true);
        ProtobufSerializerUtils.writeListDelimited(this.docList, fos);
        fos.close();
    }
}
