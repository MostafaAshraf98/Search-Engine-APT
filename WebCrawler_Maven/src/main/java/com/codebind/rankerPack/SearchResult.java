package com.codebind.rankerPack;

public class SearchResult {
    public WebPage[] searchResults;

    public SearchResult() {
        WebPage P0 = new WebPage(0, new String[] { "2" }, 0, 0.25, 2, new Integer[] { 1, 2 });
        WebPage P1 = new WebPage(1, new String[] { "0", "2" }, 0, 0.25, 1, new Integer[] { 3 });
        WebPage P2 = new WebPage(2, new String[] { "0", "3" }, 0, 0.25, 3, new Integer[] { 0, 1, 3 });
        WebPage P3 = new WebPage(3, new String[] { "1", "2" }, 0, 0.25, 1, new Integer[] { 2 });

        searchResults = new WebPage[4];
        searchResults[0] = P0;
        searchResults[1] = P1;
        searchResults[2] = P2;
        searchResults[3] = P3;

    }

}
