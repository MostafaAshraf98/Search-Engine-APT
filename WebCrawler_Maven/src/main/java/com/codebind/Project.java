package com.codebind;

import com.codebind.webCrawlerPack.WebCrawler;

import java.io.IOException;

import com.codebind.indexerPack.Indexer;

public class Project {
    public static void main(String[] args) throws IOException {
        WebCrawler webcrawler = new WebCrawler();
        WebCrawler.Web(args);

//        Indexer ind = new Indexer();
//        ind.indexer(args);
    }
}
