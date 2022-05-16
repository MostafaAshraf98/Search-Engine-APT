package Indexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.javatuples.Pair;

public class indexer
{
    static Map<String, Map<String, Pair<Integer, Map<String, Integer>>>> invertedFile = new HashMap<String, Map<String, Pair<Integer, Map<String, Integer>>>>();

    public static MongoCollection<org.bson.Document> downloadedURLs;
    static String webpagesPath = "./webPages/";
    public static void main(String[] args) throws IOException
    {
        MongoClient client = MongoClients.create(
					"mongodb+srv://Mostafa_98:mostafa123@webcrawler.6mfpo.mongodb.net/myFirstDatabase?retryWrites=true&w=majority");
		// Getting the dataBase from this client.
		MongoDatabase db = client.getDatabase("WebCrawler");
		// Getting the collections from this database.
		downloadedURLs = db.getCollection("downloadedURLs");
        

    }

    private static void index() throws IOException {
        ArrayList<Document> docs = readAllHTML();
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
                    // TODO: remove words with length < 3

                    // add word to inverted file
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

    private static ArrayList<Document> readAllHTML() throws IOException {
        ArrayList<Document> docs = new ArrayList<Document>();
        File folder = new File(webpagesPath);
        File[] listOfFiles = folder.listFiles();
        System.out.println("Number of files: " + listOfFiles.length);
        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".html")) {
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