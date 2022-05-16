package com.codebind.indexerPack;

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
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Projections;
//import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
//import org.bson.Document;
import org.javatuples.Pair;
import org.bson.conversions.Bson;
//import org.apache.lucene.analysis.PorterStemmer;

import opennlp.tools.stemmer.PorterStemmer;

public class Indexer {
    static Map<String, Map<String, Pair<Integer, Map<String, Integer>>>> invertedFile = new HashMap<String, Map<String, Pair<Integer, Map<String, Integer>>>>();
    static Map<String, ArrayList<ArrayList<String>>> index = new HashMap<String, ArrayList<ArrayList<String>>>();
    public static MongoCollection<org.bson.Document> downloadedURLs;
    static String webpagesPath = "./webPages/";
    // static ArrayList<String> stopwords;
    static Map<String, Integer> stopwords;

    public static void indexer(String[] args, MongoDatabase db) throws IOException {
        loadStopwords();
        // Getting the collections from this database.
        ArrayList<Document> docs = readAllHTML(db);
        stemWord("news");
        stemWord("things");
        stemWord("computation");
        stemWord("specifications");
        stemWord("specify");
        stemWord("specified");
        stemWord("good");
        stemWord("better");

        // Indexing the documents
        index(docs);

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

    private static void index(ArrayList<Document> docs) throws IOException {
        for (Document doc : docs) {
            // Get all tags in a document
            Elements all = doc.getAllElements();
            for (Element e : all) {
                // For each tag, get the text
                String text = e.text();
                String[] words = text.split(" ");
                for (String word : words) {
                    word = word.toLowerCase();
                    // Replacing all non-alphanumeric (not from "[^a-zA-Z0-9]") characters with
                    // empty string
                    word = word.replaceAll("[^a-zA-Z0-9]", "");
                    // TODO: remove stop words
                    boolean isStopWord = isStopword(word);
                    // TODO: stemming
                    word = stemWord(word);

                    // check whether to add word or not
                    boolean addWord = !isStopWord && word.length() > 0;
                    if (addWord) {
                        if (invertedFile.containsKey(word)) {
                            // If the word is already in the inverted file

                            // doc.baseUri() returns the url of the document
                            // ex. C:\Users\dusername\Desktop\Java\.\125241216.html
                            // get the last part of the url
                            // ex. 125241216.html
                            String fileName = doc.baseUri().substring(doc.baseUri().lastIndexOf("\\") + 1);

                            if (invertedFile.get(word).containsKey(fileName)) {
                                // If the documnet is already in the inverted file
                                Pair<Integer, Map<String, Integer>> pair = invertedFile.get(word).get(fileName);

                                // Update the term frequency
                                int newTermFreq = pair.getValue0() + 1;

                                // Check if the position of the word in the document is already in the inverted
                                // file
                                if (pair.getValue1().containsKey(e.tagName())) {
                                    // If the position is already in the inverted file, update the term frequency
                                    int pos = pair.getValue1().get(e.tagName());
                                    pos++;
                                    Map<String, Integer> newMap = pair.getValue1();
                                    newMap.put(e.tagName(), pos);
                                    pair = new Pair<Integer, Map<String, Integer>>(newTermFreq, newMap);
                                    invertedFile.get(word).put(fileName, pair);
                                } else {
                                    // If the position is not in the inverted file, add it
                                    Map<String, Integer> newMap = pair.getValue1();
                                    newMap.put(e.tagName(), 1);
                                    pair = new Pair<Integer, Map<String, Integer>>(newTermFreq, newMap);
                                    invertedFile.get(word).put(fileName, pair);

                                }
                            } else {
                                // If the documnet is not in the inverted file
                                invertedFile.get(word).put(fileName,
                                        new Pair<Integer, Map<String, Integer>>(1, new HashMap<String, Integer>() {
                                            {
                                                put(e.tagName(), 1);
                                            }
                                        }));
                            }

                        } else {
                            // If the word is not in the inverted file

                            String fileName = doc.baseUri().substring(doc.baseUri().lastIndexOf("\\") + 1);
                            invertedFile.put(word, new HashMap<String, Pair<Integer, Map<String, Integer>>>() {
                                {
                                    put(fileName,
                                            new Pair<Integer, Map<String, Integer>>(1, new HashMap<String, Integer>() {
                                                {
                                                    put(e.tagName(), 1);
                                                }
                                            }));
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    private static ArrayList<Document> readAllHTML(MongoDatabase db) throws IOException {
        ArrayList<Document> docs = new ArrayList<Document>();
        File folder = new File(webpagesPath);
        // File[] listOfFiles = folder.listFiles();
        // System.out.println("Number of files: " + listOfFiles.length);
        downloadedURLs = db.getCollection("downloadedURLs");
        Bson projection = Projections.fields(Projections.include("url", "fileName"), Projections.excludeId());
        FindIterable<org.bson.Document> iterDoc = downloadedURLs.find().projection(projection);
        Iterator it = iterDoc.iterator();
        int count = 0;
        while (it.hasNext()) {
            count += 1;
            // System.out.println(fileUrlObject.getElementsByAttribute("filename"));
            org.bson.Document fileUrlObject = (org.bson.Document) it.next();
            // System.out.println(fileUrlObject.getElementsByAttribute("filename"));
            Document doc = readHTMLFile(webpagesPath + fileUrlObject.get("fileName") + ".html");
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