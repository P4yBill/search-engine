package com.p4ybill.engine.search;

import com.p4ybill.engine.store.EngineIndexPB;

import java.util.*;

public class SearchBooleanQuery {

    /**
     * Calculate the intersection of two postings list
     *
     * @param p1 posting list for termA
     * @param p2 posting list for termB
     * @return intersected posting list containing the docs that appear in p1 and p2.
     */
    public List<EngineIndexPB.PostingList.Posting> intersect(List<EngineIndexPB.PostingList.Posting> p1, List<EngineIndexPB.PostingList.Posting> p2) {
        List<EngineIndexPB.PostingList.Posting> intersectList = new ArrayList<>();
        this.sortPostingList(p1);
        this.sortPostingList(p2);

        int p1Index = 0, p2Index = 0;
        int p1PostingsCount = p1.size();
        int p2PostingsCount = p2.size();
        while (p1Index < p1PostingsCount && p2Index < p2PostingsCount) {
            if (p1.get(p1Index).getDocId() < p2.get(p2Index).getDocId()) {
                p1Index++;
            } else if (p2.get(p1Index).getDocId() > p2.get(p2Index).getDocId()) {
                p2Index++;
            } else {
                intersectList.add(p1.get(p1Index));
                p1Index++;
                p2Index++;
            }
        }
        return intersectList;
    }

    /**
     * Calculate the intersection of many posting lists.
     * This method uses the method intersect {@link #intersect(List, List)}.
     * This is the implemented method as described in Figure 1.7 of Christopher's Manning IR book.
     *
     * @param termPostingLists List that contains the posting lists to be intersected
     * @return List that represents the posting list that was made from termPostingLists
     */
    public List<EngineIndexPB.PostingList.Posting> intersectMany(List<EngineIndexPB.PostingList> termPostingLists) {
        this.sortListAsc(termPostingLists);

        List<EngineIndexPB.PostingList.Posting> result = termPostingLists.remove(0).getPostingsList();
        result = new ArrayList<>(result);
        while (termPostingLists.size() != 0 && result != null) {
            List<EngineIndexPB.PostingList.Posting> pl = termPostingLists.remove(0).getPostingsList();
            // protobuf send an unmodifiableList so we have to create one that can be modified
            result = intersect(result, new ArrayList<>(pl));
        }

        return result;
    }

    private void sortListAsc(List<EngineIndexPB.PostingList> termsPostings) {
        termsPostings.sort(new Comparator<EngineIndexPB.PostingList>() {

            public int compare(EngineIndexPB.PostingList p1, EngineIndexPB.PostingList p2) {
//                return if size of p1 is greater return +1, if size of p1 is smaller return -1 otherwise 0
                if (p1.getPostingsCount() > p2.getPostingsCount()) {
                    return 1;
                } else if (p1.getPostingsCount() < p2.getPostingsCount()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }

    private void sortPostingList(List<EngineIndexPB.PostingList.Posting> postingList) {
        postingList.sort(new Comparator<EngineIndexPB.PostingList.Posting>() {

            public int compare(EngineIndexPB.PostingList.Posting posting1, EngineIndexPB.PostingList.Posting posting2) {
                if (posting1.getDocId() > posting2.getDocId()) {
                    return 1;
                } else if (posting1.getDocId() < posting2.getDocId()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }

}
