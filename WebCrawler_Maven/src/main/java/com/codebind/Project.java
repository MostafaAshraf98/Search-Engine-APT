package com.codebind;

import com.codebind.webCrawlerPack.WebCrawler;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.io.IOException;

import com.codebind.indexerPack.Indexer;
import com.codebind.rankerPack.PageRank;
import com.codebind.rankerPack.SearchResult;

public class Project {
    public static void main(String[] args) throws IOException {
        MongoClient client = MongoClients.create(
                "mongodb+srv://Mostafa_98:mostafa123@webcrawler.6mfpo.mongodb.net/myFirstDatabase?retryWrites=true&w=majority");
        // Getting the dataBase from this client.
        MongoDatabase db = client.getDatabase("WebCrawler");

        WebCrawler webcrawler = new WebCrawler();
        System.out.println("Started Crawling");
        long start = System.currentTimeMillis();
        WebCrawler.Web(args, db);
        long end = System.currentTimeMillis();
        System.out.println("Crawling Finished in " + (end - start) / 60000.0 + " minutes");
        // Indexer ind = new Indexer();
        // ind.indexer(args, db);
        // SearchResult sR = new SearchResult();

        // PageRank.rank(args, db, sR);
    }
}
