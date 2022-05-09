package com.codebind.indexerPack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Indexer {
    static Map<String, ArrayList<ArrayList<String>>> index = new HashMap<String, ArrayList<ArrayList<String>>>();
    public static MongoCollection<org.bson.Document> downloadedURLs;
    static String webpagesPath = "./webPages/";

    public static void indexer(String[] args) throws IOException {
        MongoClient client = MongoClients.create(
                "mongodb+srv://Mostafa_98:mostafa123@webcrawler.6mfpo.mongodb.net/myFirstDatabase?retryWrites=true&w=majority");
        // Getting the dataBase from this client.
        MongoDatabase db = client.getDatabase("WebCrawler");
        // Getting the collections from this database.
        downloadedURLs = db.getCollection("downloadedURLs");
        ArrayList<Document> docs = readAllHTML();
        for (Document doc : docs) {
            // Apply logic here and add to database
        }

    }

    private static ArrayList<Document> readAllHTML() throws IOException {
        ArrayList<Document> docs = new ArrayList<Document>();
        File folder = new File(webpagesPath);
        File[] listOfFiles = folder.listFiles();
        System.out.println("Number of files: " + listOfFiles.length);
        for (File file : listOfFiles) {
            if (file.isFile()) {
                System.out.println(file.getName());
                Document doc = readHTMLFile(webpagesPath + file.getName());
                docs.add(doc);
            }
        }
        return docs;
    }

    private static Document readHTMLFile(String fileName) throws IOException {
        File htmlFile = new File(fileName);
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        return doc;
    }

}