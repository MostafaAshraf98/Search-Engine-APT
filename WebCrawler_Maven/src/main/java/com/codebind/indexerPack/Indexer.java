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
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Projections;
//import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import static com.mongodb.client.model.Filters.eq;

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
    public static MongoCollection<org.bson.Document> downloadedURLs, IndexerCollection;
    static String webpagesPath = "./webPages/";
    // static ArrayList<String> stopwords;
    static Map<String, Integer> stopwords;
    static Map<String, String> file_URL;

    public static void indexer(String[] args, MongoDatabase db) throws IOException {
        loadStopwords();
        // Getting the collections from this database.
        IndexerCollection = db.getCollection("IndexerCollection");
        downloadedURLs = db.getCollection("downloadedURLs");
        ArrayList<Document> docs = readAllHTML();

        // System.out.println(stemWord("universal"));
        // Indexing the documents
        System.out.print(file_URL.get("539506411.html"));
        index(docs);
        AddToDatabase();
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

    // private static void index(Document doc, String Url) throws IOException {
    private static void index(ArrayList<Document> docs) throws IOException {
        for (Document doc : docs) {
            // Get all tags in a document
            Elements all = doc.getAllElements();
            for (final Element e : all) {
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
                            // System.out.print(fileName+" ");
                            fileName = file_URL.get(fileName);
                            // System.out.print(fileName+" ");
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

                            String fileName1 = doc.baseUri().substring(doc.baseUri().lastIndexOf("\\") + 1);
                            // System.out.print(fileName1+" ");
                            final String fileName = file_URL.get(fileName1);
                            // System.out.print(fileName+" ");
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
            // AddToDatabase(invertedFile);
        }
        for (String word : invertedFile.keySet()) {
            Map<String, Pair<Integer, Map<String, Integer>>> Occurances = invertedFile.get(word);
            for (String URL : Occurances.keySet()) {

                Pair<Integer, Map<String, Integer>> OccurancesInfo = Occurances.get(URL);
                for (String title : OccurancesInfo.getValue1().keySet()) {
                    Integer count = OccurancesInfo.getValue1().get(title);
                    // System.out.println(word + " "+URL+" "+ OccurancesInfo.getValue0()+" "+title+"
                    // "+OccurancesInfo.getValue1().get(title));
                    break;
                }
            }
        }
    }

    private static void AddToDatabase() {
        System.out.println("adding to database");
        for (String word : invertedFile.keySet()) {
            // org.bson.Document
            // QuerryDocuments=IndexerCollection.find(eq("word",word)).first();
            org.bson.Document IndexerDocument = new org.bson.Document("word", word);
            // org.bson.Document Occurancedocument=null;
            Map<String, Pair<Integer, Map<String, Integer>>> Occurances = invertedFile.get(word);
            ArrayList<org.bson.Document> referencedat = new ArrayList<org.bson.Document>();
            for (String URL : Occurances.keySet()) {

                org.bson.Document ReferenceDocument = new org.bson.Document("URL", URL);
                Pair<Integer, Map<String, Integer>> OccurancesInfo = Occurances.get(URL);
                ReferenceDocument.append("TF", OccurancesInfo.getValue0());
                ArrayList<org.bson.Document> importanceInfo = new ArrayList<org.bson.Document>();
                for (String title : OccurancesInfo.getValue1().keySet()) {
                    Integer count = OccurancesInfo.getValue1().get(title);
                    org.bson.Document titleDocument = new org.bson.Document("title", title);
                    titleDocument.append("title frequency", count);
                    importanceInfo.add(titleDocument);
                    // System.out.println(word + " "+URL+" "+ OccurancesInfo.getValue0()+" "+title+"
                    // "+OccurancesInfo.getValue1().get(title));
                }
                ReferenceDocument.append("Appeared as", importanceInfo);
                referencedat.add(ReferenceDocument);
            }
            IndexerDocument.append("References", referencedat);
            IndexerCollection.insertOne(IndexerDocument);
        }
    }

    private static ArrayList<Document> readAllHTML() throws IOException {
        file_URL = new HashMap<String, String>();
        ArrayList<Document> docs = new ArrayList<Document>();
        File folder = new File(webpagesPath);
        Bson projection = Projections.fields(Projections.include("url", "fileName"), Projections.excludeId());
        FindIterable<org.bson.Document> iterDoc = downloadedURLs.find().projection(projection);
        Iterator it = iterDoc.iterator();
        int count = 0;
        Document doc = null;
        while (it.hasNext()) {
            count += 1;
            org.bson.Document fileUrlObject = (org.bson.Document) it.next();
            doc = readHTMLFile(webpagesPath + fileUrlObject.get("fileName") + ".html");
            String fileName = fileUrlObject.get("fileName") + ".html";
            String URL = fileUrlObject.get("url") + "";
            file_URL.put(fileName, URL);

            docs.add(doc);
            // break;
        }

        return docs;
    }

    private static Document readHTMLFile(String fileName) throws IOException {
        File htmlFile = new File(fileName);
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        return doc;
    }

}