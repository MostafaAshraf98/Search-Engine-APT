package com.codebind.indexerPack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Projections;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
//import org.bson.Document;

import org.bson.conversions.Bson;

public class Indexer {
    static Map<String, ArrayList<ArrayList<String>>> index = new HashMap<String, ArrayList<ArrayList<String>>>();
    public static MongoCollection<org.bson.Document> downloadedURLs;
    static String webpagesPath = "./webPages/";

    public static void indexer(String[] args,MongoDatabase db) throws IOException {
        
        // Getting the collections from this database.
        
        ArrayList<Document> docs = readAllHTML(db);
        for (Document doc : docs) {
            // Apply logic here and add to database
        }

    }

    private static ArrayList<Document> readAllHTML(MongoDatabase db) throws IOException {
        ArrayList<Document> docs = new ArrayList<Document>();
        File folder = new File(webpagesPath);
//        File[] listOfFiles = folder.listFiles();
//        System.out.println("Number of files: " + listOfFiles.length);
        downloadedURLs = db.getCollection("downloadedURLs");
        Bson projection = Projections.fields(Projections.include("url", "fileName"), Projections.excludeId());
        FindIterable<org.bson.Document> iterDoc = downloadedURLs.find().projection(projection);
        Iterator it = iterDoc.iterator();
        int count=0;
        while (it.hasNext()) {
        	count+=1;
//           System.out.println(fileUrlObject.getElementsByAttribute("filename"));
        	org.bson.Document fileUrlObject= (org.bson.Document) it.next();
//        	System.out.println(fileUrlObject.getElementsByAttribute("filename"));
        	Document doc = readHTMLFile(webpagesPath + fileUrlObject.get("fileName")+".html");
        	docs.add(doc);
        }
        return docs;
    }

    private static Document readHTMLFile(String fileName) throws IOException {
        File htmlFile = new File(fileName);
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        return doc;
    }

}