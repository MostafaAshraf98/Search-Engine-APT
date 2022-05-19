package com.codebind.indexerPack;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException; // Import this class to handle errors
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

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
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
//import org.bson.Document;
import org.javatuples.Triplet;
import org.bson.conversions.Bson;
//import org.apache.lucene.analysis.PorterStemmer;

import opennlp.tools.stemmer.PorterStemmer;

public class Indexer {
    static Map<String, Map<String, Triplet<Integer, Integer, Map<String, Integer>>>> invertedFile = new HashMap<String, Map<String, Triplet<Integer, Integer, Map<String, Integer>>>>();
    static Map<String, ArrayList<ArrayList<String>>> index = new HashMap<String, ArrayList<ArrayList<String>>>();
    public static MongoCollection<org.bson.Document> downloadedURLs, IndexerCollection;
    static String webpagesPath = "./webPages/";
    // static ArrayList<String> stopwords;
    static Map<String, Integer> stopwords;
    static Map<String, String> file_URL;
    // static Map<String, String> File_size;

    // Those are used in the indexer map
    static String headName = "head";
    static String bodyName = "body";

    static Integer totDocCount = 0;

    public static void indexer(String[] args, MongoDatabase db) throws IOException {

        loadStopwords();

        // Getting the collections from this database.

        IndexerCollection = db.getCollection("IndexerCollection");
        downloadedURLs = db.getCollection("downloadedURLs");

        ArrayList<Document> docs = readAllHTML();
        index(docs);

        // System.out.println(stemWord("universal"));

        // Indexing the documents
        // print the inverted file
        // printInvertedFile();
        AddToDatabase();
        // UpdateDatabase();

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

    // private static void index(Document doc, String Url) throws IOException {
    private static void index(ArrayList<Document> docs) throws IOException {
        System.out.println("Indexing the documents");
        int count = 0;

        totDocCount = docs.size();

        for (Document doc : docs) {
            System.out.print("\rIndexing: " + ++count + "/" + totDocCount);
            // int count=0;
            // Get all tags in a document
            Elements all = doc.getAllElements();

            Integer wordCount = 0;

            // doc.baseUri() returns the url of the document
            // ex. C:\Users\dusername\Desktop\Java\.\125241216.html
            // get the last part of the url
            // ex. 125241216.html
            final String fileName = file_URL.get(doc.baseUri().substring(doc.baseUri().lastIndexOf("\\") + 1));

            for (final Element e : all) {
                // For each tag, get the text

                String text = e.text();
                // Get the tag name
                final String tag = e.tagName();

                // Do not index the tags that are not text
                if (tag == "script" || tag == "style" || tag == "noscript" || tag == "head" || tag == "meta"
                        || tag == "link"
                        || tag == "input" || tag == "button" || tag == "select" || tag == "option" || tag == "form") {
                    continue;
                }

                String[] words = text.split(" ");
                wordCount += words.length;
                for (String word : words) {

                    word = word.toLowerCase();
                    // Replacing all non-alphanumeric (not from "[^a-zA-Z0-9]") characters with
                    // empty string
                    word = word.replaceAll("[^a-zA-Z]", "");
                    // TODO: remove stop words
                    boolean isStopWord = isStopword(word);
                    // TODO: stemming
                    word = stemWord(word);

                    // check whether to add word or not
                    boolean addWord = !isStopWord && word.length() > 0;
                    if (addWord) {
                        if (invertedFile.containsKey(word)) {
                            // count=+1;
                            // If the word is already in the inverted file

                            if (invertedFile.get(word).containsKey(fileName)) {
                                // If the documnet is already in the inverted file
                                Triplet<Integer, Integer, Map<String, Integer>> triplet = invertedFile.get(word)
                                        .get(fileName);

                                // Update the term frequency
                                int newTermFreq = triplet.getValue0() + 1;

                                // Check if the position of the word in the document is whether in head or body
                                // If the word is in the head, add the word to the head list
                                // If the word is in the body, add the word to the body list

                                if (tag == "title" || tag == "h1" || tag == "h2" || tag == "h3" || tag == "h4"
                                        || tag == "h5" || tag == "h6" || tag == "root" || tag == "html"
                                        || tag == "main") {
                                    // It is in the head
                                    if (triplet.getValue2().containsKey(headName)) {
                                        int pos = triplet.getValue2().get(headName);
                                        pos++;
                                        Map<String, Integer> newMap = triplet.getValue2();
                                        newMap.put(headName, pos);
                                        triplet = new Triplet<Integer, Integer, Map<String, Integer>>(newTermFreq,
                                                wordCount, newMap);
                                        invertedFile.get(word).put(fileName, triplet);
                                    } else {
                                        Map<String, Integer> newMap = triplet.getValue2();
                                        newMap.put(headName, 1);
                                        triplet = new Triplet<Integer, Integer, Map<String, Integer>>(newTermFreq,
                                                wordCount, newMap);
                                        invertedFile.get(word).put(fileName, triplet);
                                    }
                                } else {
                                    // It is in the body
                                    if (triplet.getValue2().containsKey(bodyName)) {
                                        int pos = triplet.getValue2().get(bodyName);
                                        pos++;
                                        Map<String, Integer> newMap = triplet.getValue2();
                                        newMap.put(bodyName, pos);
                                        triplet = new Triplet<Integer, Integer, Map<String, Integer>>(newTermFreq,
                                                wordCount, newMap);
                                        invertedFile.get(word).put(fileName, triplet);
                                    } else {
                                        Map<String, Integer> newMap = triplet.getValue2();
                                        newMap.put(bodyName, 1);
                                        triplet = new Triplet<Integer, Integer, Map<String, Integer>>(newTermFreq,
                                                wordCount, newMap);
                                        invertedFile.get(word).put(fileName, triplet);
                                    }

                                }
                            } else {
                                // If the documnet is not in the inverted file
                                invertedFile.get(word).put(fileName,
                                        new Triplet<Integer, Integer, Map<String, Integer>>(1, wordCount,
                                                new HashMap<String, Integer>() {
                                                    {
                                                        if (tag == "title" || tag == "h1" || tag == "h2" || tag == "h3"
                                                                || tag == "h4" || tag == "h5" || tag == "h6"
                                                                || tag == "root" || tag == "html" || tag == "main") {
                                                            put(headName, 1);
                                                        } else {
                                                            put(bodyName, 1);
                                                        }
                                                    }
                                                }));
                            }

                        } else {
                            // If the word is not in the inverted file

                            // String fileName1 = doc.baseUri().substring(doc.baseUri().lastIndexOf("\\") +
                            // 1);
                            // System.out.print(fileName1+" ");

                            // System.out.print(fileName+" ");
                            Map<String, Triplet<Integer, Integer, Map<String, Integer>>> newMap = new HashMap<String, Triplet<Integer, Integer, Map<String, Integer>>>();
                            newMap.put(fileName,
                                    new Triplet<Integer, Integer, Map<String, Integer>>(1, wordCount,
                                            new HashMap<String, Integer>() {
                                                {
                                                    if (tag == "title" || tag == "h1" || tag == "h2" || tag == "h3"
                                                            || tag == "h4" || tag == "h5" || tag == "h6"
                                                            || tag == "root" || tag == "html" || tag == "main") {
                                                        put(headName, 1);
                                                    } else {
                                                        put(bodyName, 1);
                                                    }
                                                }
                                            }));
                            invertedFile.put(word, newMap);
                        }
                    }
                }
            }

            // AddToDatabase(invertedFile);
        }
        // for (String word : invertedFile.keySet()) {
        // Map<String, Pair<Integer, Map<String, Integer>>> Occurances =
        // invertedFile.get(word);
        // for (String URL : Occurances.keySet()) {
        //
        // Pair<Integer, Map<String, Integer>> OccurancesInfo = Occurances.get(URL);
        // for (String title : OccurancesInfo.getValue1().keySet()) {
        // Integer count = OccurancesInfo.getValue1().get(title);
        // // System.out.println(word + " "+URL+" "+ OccurancesInfo.getValue0()+"
        // "+title+"
        // // "+OccurancesInfo.getValue1().get(title));
        // break;
        // }
        // }
        // }
    }

    private static void AddToDatabase() {
        System.out.println("adding to database");
        // Loop through the inverted file

        ArrayList<org.bson.Document> documentsToInsert = new ArrayList<org.bson.Document>();
        for (String word : invertedFile.keySet()) {

            // org.bson.Document
            Double totTFIDF = 0.0;
            // QuerryDocuments=IndexerCollection.find(eq("word",word)).first();
            org.bson.Document IndexerDocument = new org.bson.Document("word", word);
            // add IDF value = Math.log10(total # if doc aka 5000 /# of documents this word
            // in )
            // org.bson.Document Occurancedocument=null;
            Map<String, Triplet<Integer, Integer, Map<String, Integer>>> Occurances = invertedFile.get(word);
            ArrayList<org.bson.Document> referencedat = new ArrayList<org.bson.Document>();
            for (String URL : Occurances.keySet()) {

                Integer tf = Occurances.get(URL).getValue0();
                Integer wordCount = Occurances.get(URL).getValue1();
                Double normalisedTF = (double) (tf) / (double) (wordCount);

                Double idf = Math.log10((double) totDocCount / (double) Occurances.size());

                Double tfidf = 0.35 * normalisedTF + 0.65 * idf;
                totTFIDF += tfidf;

                org.bson.Document ReferenceDocument = new org.bson.Document("URL", URL);
                Triplet<Integer, Integer, Map<String, Integer>> OccurancesInfo = Occurances.get(URL);
                ReferenceDocument.append("TF", OccurancesInfo.getValue0());
                // TFBody value / # of words in this document
                // TFTitle value / # of words in this document
                ReferenceDocument.append("TFIDF", tfidf);
                ArrayList<org.bson.Document> importanceInfo = new ArrayList<org.bson.Document>();
                for (String title : OccurancesInfo.getValue2().keySet()) {
                    Integer count = OccurancesInfo.getValue2().get(title);
                    org.bson.Document titleDocument = new org.bson.Document("tag", title);
                    titleDocument.append("tag frequency", count);
                    importanceInfo.add(titleDocument);
                    // System.out.println(word + " "+URL+" "+ OccurancesInfo.getValue0()+" "+title+"
                    // "+OccurancesInfo.getValue1().get(title));

                    // TF = TFBody*0.3+TFTitle*0.7
                }
                ReferenceDocument.append("Appeared as", importanceInfo);
                referencedat.add(ReferenceDocument);
                // TF-IDF = TF * IDF

            }
            IndexerDocument.append("TFIDF", totTFIDF);
            IndexerDocument.append("References", referencedat);

            documentsToInsert.add(IndexerDocument);
            // IndexerCollection.insertOne(IndexerDocument);
            // break;
        }
        IndexerCollection.insertMany(documentsToInsert);
    }

    private static void UpdateDatabase() {
        System.out.println("updating database");
        // Loop through the inverted file
        for (String word : invertedFile.keySet()) {
            // org.bson.Document
            Double totTFIDF = 0.0;
            Bson query = Filters.eq("word", word);
            Bson update = null;
            Map<String, Triplet<Integer, Integer, Map<String, Integer>>> Occurances = invertedFile.get(word);
            ArrayList<org.bson.Document> referencedat = new ArrayList<org.bson.Document>();
            for (String URL : Occurances.keySet()) {
                Integer tf = Occurances.get(URL).getValue0();
                Integer wordCount = Occurances.get(URL).getValue1();
                Double normalisedTF = (double) (tf) / (double) (wordCount);

                Double idf = Math.log10((double) totDocCount / (double) Occurances.size());

                Double tfidf = 0.35 * normalisedTF + 0.65 * idf;
                totTFIDF += tfidf;
                org.bson.Document ReferenceDocument = new org.bson.Document("URL", URL);
                Triplet<Integer, Integer, Map<String, Integer>> OccurancesInfo = Occurances.get(URL);
                ReferenceDocument.append("TF", OccurancesInfo.getValue0());
                // TFBody value / # of words in this document
                // TFTitle value / # of words in this document
                ReferenceDocument.append("TFIDF", tfidf);
                ArrayList<org.bson.Document> importanceInfo = new ArrayList<org.bson.Document>();
                for (String title : OccurancesInfo.getValue2().keySet()) {
                    Integer count = OccurancesInfo.getValue2().get(title);
                    org.bson.Document titleDocument = new org.bson.Document("tag", title);
                    titleDocument.append("tag frequency", count);
                    importanceInfo.add(titleDocument);
                    // System.out.println(word + " "+URL+" "+ OccurancesInfo.getValue0()+" "+title+"
                    // "+OccurancesInfo.getValue1().get(title));

                    // TF = TFBody*0.3+TFTitle*0.7
                }
                ReferenceDocument.append("Appeared as", importanceInfo);
                referencedat.add(ReferenceDocument);
                // TF-IDF = TF * IDF
            }
            update = Updates.pushEach("References", referencedat);
            org.bson.Document oldDocument = IndexerCollection.findOneAndUpdate(query, update);
            if (oldDocument == null) {
                AddWordToDatabase(word);
            }
        }
        // IndexerCollection.insertMany(documentsToInsert);
    }

    private static void AddWordToDatabase(String word) {
        // Loop through the inverted file
        Double totTFIDF = 0.0;
        // QuerryDocuments=IndexerCollection.find(eq("word",word)).first();
        org.bson.Document IndexerDocument = new org.bson.Document("word", word);
        // add IDF value = Math.log10(total # if doc aka 5000 /# of documents this word
        // in )
        // explain map

        Map<String, Triplet<Integer, Integer, Map<String, Integer>>> Occurances = invertedFile.get(word);
        ArrayList<org.bson.Document> referencedat = new ArrayList<org.bson.Document>();
        for (String URL : Occurances.keySet()) {

            Integer tf = Occurances.get(URL).getValue0();
            Integer wordCount = Occurances.get(URL).getValue1();
            Double normalisedTF = (double) (tf) / (double) (wordCount);

            Double idf = Math.log10((double) totDocCount / (double) Occurances.size());

            Double tfidf = 0.35 * normalisedTF + 0.65 * idf;
            totTFIDF += tfidf;

            org.bson.Document ReferenceDocument = new org.bson.Document("URL", URL);
            Triplet<Integer, Integer, Map<String, Integer>> OccurancesInfo = Occurances.get(URL);
            ReferenceDocument.append("TF", OccurancesInfo.getValue0());
            // TFBody value / # of words in this document
            // TFTitle value / # of words in this document
            ReferenceDocument.append("TFIDF", tfidf);
            ArrayList<org.bson.Document> importanceInfo = new ArrayList<org.bson.Document>();
            for (String title : OccurancesInfo.getValue2().keySet()) {
                Integer count = OccurancesInfo.getValue2().get(title);
                org.bson.Document titleDocument = new org.bson.Document("tag", title);
                titleDocument.append("tag frequency", count);
                importanceInfo.add(titleDocument);
                // System.out.println(word + " "+URL+" "+ OccurancesInfo.getValue0()+" "+title+"
                // "+OccurancesInfo.getValue1().get(title));

                // TF = TFBody*0.3+TFTitle*0.7
            }
            ReferenceDocument.append("Appeared as", importanceInfo);
            referencedat.add(ReferenceDocument);
            // TF-IDF = TF * IDF

        }
        IndexerDocument.append("TFIDF", totTFIDF);
        IndexerDocument.append("References", referencedat);
        IndexerCollection.insertOne(IndexerDocument);
    }

    private static ArrayList<Document> readAllHTML() throws IOException {
        file_URL = new HashMap<String, String>();
        ArrayList<Document> docs = new ArrayList<Document>();

        // File folder = new File(webpagesPath);
        // File[] listOfFiles = folder.listFiles();

        // if (end > listOfFiles.length)
        // {
        // end = listOfFiles.length;
        // }
        // if (start > end) {
        // System.out.println("start is greater than end");
        // return docs;
        // }
        // // System.out.println("Number of files: " + listOfFiles.length);
        // for (int i = start; i < end; i++) {
        // if (listOfFiles[i].isFile()) {
        // String fileName = listOfFiles[i].getName();
        // String filePath = listOfFiles[i].getAbsolutePath();
        // //file_URL.put(fileName, filePath);
        // Document doc = Jsoup.parse(new File(filePath), "UTF-8");
        // docs.add(doc);
        // }
        // }

        Bson projection = Projections.fields(Projections.include("url", "fileName"),
                Projections.excludeId());
        FindIterable<org.bson.Document> iterDoc = downloadedURLs.find().projection(projection);
        Iterator it = iterDoc.iterator();
        Document doc = null;
        while (it.hasNext()) {
            org.bson.Document fileUrlObject = (org.bson.Document) it.next();
            doc = readHTMLFile(webpagesPath + fileUrlObject.get("fileName") + ".html");
            if (doc == null) {
                continue;
            }
            String fileName = fileUrlObject.get("fileName") + ".html";
            String URL = fileUrlObject.get("url") + "";
            file_URL.put(fileName, URL);
            docs.add(doc);
        }
        return docs;
    }

    private static Document readHTMLFile(String fileName) throws IOException {
        File htmlFile = new File(fileName);
        try {
            Document doc = Jsoup.parse(htmlFile, "UTF-8");
            return doc;
        } catch (IOException e) {
            System.out.println("Error in reading file: " + fileName + "\nFound in db but not in file system");
            return null;
        }
        // Document doc = Jsoup.parse(htmlFile, "UTF-8");
        // return doc;
    }

    // print the inverted file
    private static void printInvertedFile() {
        for (String word : invertedFile.keySet()) {
            Map<String, Triplet<Integer, Integer, Map<String, Integer>>> Occurances = invertedFile.get(word);
            for (String URL : Occurances.keySet()) {
                Triplet<Integer, Integer, Map<String, Integer>> OccurancesInfo = Occurances.get(URL);
                Integer count = OccurancesInfo.getValue0();
                System.out.println(
                        word + " " + URL + " Count " + count + " Doc word Count " + OccurancesInfo.getValue1()
                                + " Head " + OccurancesInfo.getValue2().get(headName) + " "
                                + " Body " + OccurancesInfo.getValue2().get(bodyName));
            }
        }
    }

}