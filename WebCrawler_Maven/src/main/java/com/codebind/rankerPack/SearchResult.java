package com.codebind.rankerPack;

public class SearchResult {
    public WebPage[] searchResults;
    double tfIDF;
    double PR = 0;
    double combinedScore;

    public SearchResult() {
        WebPage P0 = new WebPage(0, new String[] { "2" }, 0, 0.25, 2);
        WebPage P1 = new WebPage(1, new String[] { "0", "2" }, 0, 0.25, 1);
        WebPage P2 = new WebPage(2, new String[] { "0", "3" }, 0, 0.25, 3);
        WebPage P3 = new WebPage(3, new String[] { "1", "2" }, 0, 0.25, 1);

        searchResults = new WebPage[4];
        searchResults[0] = P0;
        searchResults[1] = P1;
        searchResults[2] = P2;
        searchResults[3] = P3;

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
