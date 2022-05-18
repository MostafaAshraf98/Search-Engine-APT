package com.codebind.rankerPack;

import java.util.ArrayList;

public class SearchResult {
    public ArrayList<WebPage> pagesResults;
    double tfIDF;
    double PR = 0;
    double combinedScore;

    // send links to be set
    // set the tfIDF and the PR from the IndexerCollection
    public SearchResult() {
        WebPage P0 = new WebPage(0, new String[] { "2" }, 0, 0.25, 2);
        WebPage P1 = new WebPage(1, new String[] { "0", "2" }, 0, 0.25, 1);
        WebPage P2 = new WebPage(2, new String[] { "0", "3" }, 0, 0.25, 3);
        WebPage P3 = new WebPage(3, new String[] { "1", "2" }, 0, 0.25, 1);

        pagesResults = new ArrayList<WebPage>(4);
        pagesResults.add(P0);
        pagesResults.add(P1);
        pagesResults.add(P2);
        pagesResults.add(P3);
        // for (WebPage WB : pagesResults) {
        // pagesResults.add(WB);
        // }

    }

    public double getTfIDF() {
        return tfIDF;
    }

    public void setTfIDF(double tfIDF) {
        this.tfIDF = tfIDF;
    }

    public double getPR() {
        return PR;
    }

    public void setPR(double PR) {
        this.PR = PR;
    }

    public double getCombinedScore() {
        return combinedScore;
    }

    public void setCombinedScore(double combinedScore) {
        this.combinedScore = combinedScore;
    }

}
