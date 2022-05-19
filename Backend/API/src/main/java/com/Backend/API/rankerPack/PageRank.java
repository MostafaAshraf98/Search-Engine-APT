package com.Backend.API.rankerPack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import static com.mongodb.client.model.Filters.eq;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.mongodb.client.FindIterable;
// Dependency for MongoDB connection 
// import com.mongodb.client.MongoClient;
// import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
// import com.mongodb.client.model.Projections;
import com.mongodb.client.result.UpdateResult;

import org.bson.Document;

// import org.bson.conversions.Bson;

public class PageRank {
    public static ArrayList<WebPage> Result;
    public static Integer flag = 0;
    // public static double[] vec_PR;
    // public static double[][] H;

    public static final double dampingFactor = 0.85;

    // The collection in MongoDB that contains the downloaded urls data.
    public static MongoCollection<org.bson.Document> downloadedURLs;

    // The collection in MongoDB that containes the referencing urls. to every url
    public static MongoCollection<org.bson.Document> References;

    // The collection in MongoDB that contains the IndexerCollection word data.
    public static MongoCollection<org.bson.Document> IndexerCollection;
    FindIterable<org.bson.Document> downloadedURLsDocuments;

    public PageRank() {
        downloadedURLsDocuments = downloadedURLs.find();
    }

    public static ArrayList<String> rank(ArrayList<String> list, HashMap<String, Double> l, MongoDatabase db,
            ArrayList<org.bson.Document> wordObject) {
        // Getting the collections from the database.
        downloadedURLs = db.getCollection("downloadedURLs");
        References = db.getCollection("References");
        IndexerCollection = db.getCollection("IndexerCollection");

        if (flag == 0) {
            ArrayList<String> urlList = new ArrayList<String>();
            for (org.bson.Document doc : downloadedURLs.find()) {
                urlList.add(doc.get("url").toString());
            }
            PageRank.calculatePR(urlList, db);
            flag = 1;
        }

        // HashMap<String, Double> l = new HashMap<String, Double>();

        // Both Doc and list taken in Ranker
        // org.bson.Document wordObject = IndexerCollection
        // .find(eq("word", "popolazion")).first();
        // ArrayList<String> list =
        // getPointingToLinksFReferences("https://login.bigcommerce.com/login");

        // System.out.println(wordObject.toJson());
        // System.out.println("word data " + wordObject.get("TFIDF"));

        // Getting the search results from the SearchResult class.
        // for (String L : list) {
        // System.out.println("link " + L);
        // System.out.println("Count is " + getOutgoingLinksCount(L));
        // FindIterable<org.bson.Document> referencedIterator = downloadedURLs
        // .find(eq("url", L));
        // Iterator it = referencedIterator.iterator();
        // if (it.hasNext()) {
        // org.bson.Document filepointingToUrlObject = (org.bson.Document) it.next();
        // System.out.println("previousPRScore is " +
        // filepointingToUrlObject.get("previousPRScore"));
        // System.out.println("currentPRScore is " +
        // filepointingToUrlObject.get("currentPRScore"));

        // }

        // }

        // updateEntryFDownloadedURLs("https://www.python.org/about/gettingstarted/",
        // "currentPRScore",
        // 1);

        // for (WebPage WP : Result) {
        // System.out.println("1link " + WP.url);
        // System.out.println("1Count is " + WP.getOutgoinglinks());

        // System.out.println("1previousPRScore is " + WP.getPreviousPRScore());
        // System.out.println("1currentPRScore is " + WP.getCurrentPRScore());
        // System.out.println("1outgoinglinks is " + WP.getIdpointingto());

        // }

        // for (WebPage WP : Result) {
        // System.out.println("2link " + WP.url);
        // System.out.println("2Count is " + WP.getOutgoinglinks());

        // System.out.println("2previousPRScore is " + WP.getPreviousPRScore());
        // System.out.println("2currentPRScore is " + WP.getCurrentPRScore());
        // System.out.println("2outgoinglinks is " + WP.getIdpointingto());

        // }
        for (Document d : wordObject) {
            ArrayList<org.bson.Document> referencedIterator = (ArrayList<Document>) d.get("References");
            for (org.bson.Document doc : referencedIterator) {
                Double tfidfVal = ((Double) doc.get("TFIDF")).doubleValue();
                // System.out.println("Link " + doc.get("URL") + " TFIDF " + doc.get("TFIDF"));
                String Link = (String) doc.get("URL");
                Double PR = getDoubleValFDownloadedURLs(Link, "currentPRScore");
                // System.out.println("Link " + doc.get("URL") + " Popularity " + PR);
                Double ComScore = (5 * tfidfVal) + PR;
                // l.put(Link, ComScore);
                if (l.get(Link) != null) {
                    l.put(Link, l.get(Link) + ComScore);
                }
                System.out.println("Link " + doc.get("URL") + " Combined Score  " + l.get(Link));

            }

        }
        // ArrayList<org.bson.Document> referencedIterator = (ArrayList<Document>)
        // wordObject.get("References");

        // Collections.sort(l, new ScoreComparator());
        ArrayList<String> sortedList = new ArrayList<String>();
        // for (Pair<String, Double> p : l) {
        // sortedList.add(p.getKey());
        // System.out.println("Link" + p.getKey());
        // }
        HashMap<String, Double> hm1 = sortByValue(l);

        // print the sorted hashmap
        for (Map.Entry me : hm1.entrySet()) {
            // System.out.println("Key: " + me.getKey() + " & Value: " + me.getValue());
            sortedList.add((String) me.getKey());

        }
        return sortedList;
        // try {
        // Collections.sort(Result, new ScoreComparator());
        // } catch (Exception e) {
        // Comparator<PageResult> comparator = new ScoreComparator();
        // for (PageResult p1 : resultsList) {
        // for (PageResult p2 : resultsList) {
        // if (comparator.compare(p1, p2) != -comparator.compare(p2, p1)) {
        // System.out.println("Loooohl");
        // }
        // }
        // }
        // }
    }

    public static void calculatePR(ArrayList<String> list, MongoDatabase db) {
        System.out.println("USING CALCPR");
        SearchResult sR = new SearchResult(list, db);
        Result = sR.pagesResults;
        calculatePopularity(5);

    }
    // Set Combined Score

    // Function to get Pointing To links for a URL
    public static ArrayList<String> getPointingToLinksFReferences(String url) {
        FindIterable<org.bson.Document> iterDoc = References
                .find(eq("url", url));
        Iterator it = iterDoc.iterator();
        if (it.hasNext()) {
            org.bson.Document fileUrlObject = (org.bson.Document) it.next();

            System.out.println("val " + fileUrlObject.get("referencedBy"));
            return (ArrayList<String>) fileUrlObject.get("referencedBy");
        }
        return null;
    }

    // Function to get the LinksCount of the url.
    public static int getOutgoingLinksCount(String url) {
        FindIterable<org.bson.Document> referencedIterator = downloadedURLs
                .find(eq("url", url));
        Iterator it = referencedIterator.iterator();
        if (it.hasNext()) {
            org.bson.Document filepointingToUrlObject = (org.bson.Document) it.next();
            // System.out.println("Count is " + filepointingToUrlObject.get("linksCount"));
            return (Integer) filepointingToUrlObject.get("linksCount");

        }
        return 0;
    }

    // Function to calculate the popularity for a given number of times
    public static void calculatePopularity(int numIterations) {
        for (int i = 0; i < numIterations; i++) {
            setPagesPopularity();

        }
    }

    /* calculate the page rank value for 1 iteration with damping factor */
    public static void setPagesPopularity() {

        for (WebPage WP : Result) {
            double tempScore = 0;
            if (WP.getIdpointingto().size() != 0) {
                for (String id : WP.idpointingto) {
                    // System.out.println(" id " + id);
                    tempScore += (getDoubleValFDownloadedURLs(id, "previousPRScore")
                            / getIntegerVal(id, "linksCount"));
                }
                tempScore = (double) ((1.0 - dampingFactor) / Result.size()) + (dampingFactor * tempScore);
            }
            WP.setCurrentPRScore(tempScore);
            updateEntryFDownloadedURLs(WP.url, "currentPRScore", tempScore);
        }
        for (WebPage WP : Result) {
            // WP.previousPRScore = WP.currentPRScore;
            WP.setPreviousPRScore(WP.getCurrentPRScore());
            updateEntryFDownloadedURLs(WP.url, "previousPRScore",
                    getDoubleValFDownloadedURLs(WP.url, "currentPRScore"));

        }
    }

    // helper function to update entry
    public static void updateEntryFDownloadedURLs(String url, String field, double value) {
        UpdateResult result = downloadedURLs.updateOne(eq("url", url),
                new org.bson.Document("$set", new org.bson.Document(field, value)));
    }

    // helper function to get double value from db
    private static Double getDoubleValFDownloadedURLs(String L, String searchKey) {
        org.bson.Document linksIterator = downloadedURLs.find(eq("url", L)).first();
        if (linksIterator == null)
            return 1.0;
        return ((Double) linksIterator.get(searchKey)).doubleValue();

    }

    // helper function to get integer value from db
    private static Integer getIntegerVal(String L, String searchKey) {
        org.bson.Document linksIterator = downloadedURLs.find(eq("url", L)).first();
        return ((Integer) linksIterator.get(searchKey)).intValue();

    }

    public static HashMap<String, Double> sortByValue(HashMap<String, Double> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1,
                    Map.Entry<String, Double> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<String, Double> temp = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static void PRCalcMatrix() {
        // vec_PR = new double[Result.length];
        // H = new double[Result.length][Result.length];
        // for (int i = 0; i < Result.length; i++) {
        // for (int j = 0; j < Result.length; j++) {
        // H[i][j] = 0;
        // }

        // }

        // /* Initialize the PR vector */
        // for (int i = 0; i < Result.length; i++) {
        // vec_PR[i] = Result[i].previousPRScore;
        // }
        // /* Initialize the H vector */
        // for (int i = 0; i < Result.length; i++) {
        // for (int j : Result[i].outgoingIDs) {
        // H[j][i] = (double) (1.0 / Result[i].outgoingIDs.length);
        // }
        // }

        // for (int i = 0; i < 2; i++) {
        // // vec_PR = H * vec_PR;
        // }
    }
}
