package com.Backend.API.QueryProcessorPack;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException; // Import this class to handle errors
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashSet;

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

public class QueryProcessor {
    public static MongoCollection<org.bson.Document> IndexerCollection;
    static Map<String, Integer> stopwords;

    public static ArrayList<String> QueryProcessor(String Query, MongoDatabase db) throws IOException {
        loadStopwords();
        // Getting the collections from this database.
        IndexerCollection = db.getCollection("IndexerCollection");
        String[] QueryWords = Query.split(" ");
        ArrayList<String> ProcessedWords = new ArrayList<String>();
        ArrayList<String> IndexedURLs = new ArrayList<String>();
        ArrayList<Document> QueryDocuments = new ArrayList<Document>();
        ArrayList<Document> Ref = new ArrayList<Document>();

        for (int i = 0; i < QueryWords.length; i++) {
            String word = QueryWords[i].toLowerCase();
            word.replaceAll("[^a-zA-Z0-9]", "");
            if (!isStopword(word)) {
                String Stem = stemWord(word);

                ProcessedWords.add(Stem);
            }
        }
        // IndexerCollection.find({"word":{$in:ProcessedWords}}).pretty();

        for (int i = 0; i < ProcessedWords.size(); i++) {
            String word = ProcessedWords.get(i);
            // System.out.println(word);
            Document wordIndexFile = IndexerCollection.find(eq("word", word)).first();
            if (wordIndexFile != null) {
                // System.out.println("check");
                ArrayList<Document> References = wordIndexFile.get("References", Ref);
                QueryDocuments.add(wordIndexFile);
                // System.out.println(" ");
                for (int j = 0; j < References.size(); j++) {
                    // System.out.println(References.get(j).get("URL"));
                    IndexedURLs.add(References.get(j).get("URL") + "");
                }
            }

        }
        LinkedHashSet<String> URLs = new LinkedHashSet<String>(IndexedURLs);
        IndexedURLs = new ArrayList<String>(URLs);
        PageRank pr = new PageRank();
        Document wordIndexFile =  IndexerCollection.find(eq("word", ProcessedWords.get(0))).first()
        ArrayList<String> sortedList= pr.rank(IndexedURLs, db,wordIndexFile);
        return sortedList;
    }

    public static void loadStopwords() throws IOException {
        stopwords = new HashMap<String, Integer>();
        try {
            File inputFile = new File("stop_words_english.txt");
            BufferedReader myReader = new BufferedReader(new FileReader(inputFile));
            String word;
            while ((word = myReader.readLine()) != null) {
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