package com.codebind.QueryEnginePack;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException; // Import this class to handle errors
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Scanner;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Projections;
//import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import static com.mongodb.client.model.Filters.eq;

import org.bson.Document;
import org.javatuples.Pair;
import org.bson.conversions.Bson;
//import org.apache.lucene.analysis.PorterStemmer;

import opennlp.tools.stemmer.PorterStemmer;

public class QueryEngine {
    public static MongoCollection<org.bson.Document> IndexerCollection;
    static Map<String, Integer> stopwords;
    public static void QueryEngine(String[] args, MongoDatabase db) throws IOException {
        loadStopwords();
        // Getting the collections from this database.
        IndexerCollection = db.getCollection("IndexerCollection");
    }

    public static void loadStopwords() throws IOException {
        stopwords = new HashMap<String, Integer>();
        try {
            File inputFile = new File("english_stopwords.txt");
            Scanner myReader = new Scanner(inputFile);
            while (myReader.hasNextLine()) {
                String word = myReader.next();
                if (!word.equals("") && !stopwords.containsKey(word))
                    stopwords.put(word, 1);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static boolean isStopword(String word) {
        boolean result = false;
        if (!word.equals("") && stopwords.containsKey(word))
            result = true;
        return result;
    }

    public static String stemWord(String word) {
        PorterStemmer stemmer = new PorterStemmer();

        return stemmer.stem(word);
    }

}