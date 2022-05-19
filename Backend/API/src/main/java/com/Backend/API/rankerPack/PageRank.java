package com.codebind.rankerPack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static com.mongodb.client.model.Filters.eq;

import java.util.Iterator;

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
    // public static double[] vec_PR;
    // public static double[][] H;

    public static final double dampingFactor = 0.85;

    // The collection in MongoDB that contains the downloaded urls data.
    public static MongoCollection<org.bson.Document> downloadedURLs;

    // The collection in MongoDB that containes the referencing urls. to every url
    public static MongoCollection<org.bson.Document> References;

    // The collection in MongoDB that contains the IndexerCollection word data.
    public static MongoCollection<org.bson.Document> IndexerCollection;

    public PageRank() {
    }

    public static void rank(String[] args, MongoDatabase db) {

        // Getting the collections from the database.
        downloadedURLs = db.getCollection("downloadedURLs");
        References = db.getCollection("References");
        IndexerCollection = db.getCollection("IndexerCollection");
        org.bson.Document wordObject = IndexerCollection
                .find(eq("word", "pick")).first();
        ArrayList<org.bson.Document> referencedIterator = (ArrayList<Document>) wordObject.get("References");
        for (org.bson.Document doc : referencedIterator) {
            System.out.println("Link " + doc.get("URL") + " TFIDF " + doc.get("TFIDF"));
            String Link = (String) doc.get("URL");
            System.out.println("Link " + doc.get("URL") + " Popularity "
                    + getDoubleValFDownloadedURLs(Link, "currentPRScore"));

        }
        // System.out.println(wordObject.toJson());
        System.out.println("word data " + wordObject.get("TFIDF"));

        // Getting the search results from the SearchResult class.
        ArrayList<String> list = getPointingToLinksFReferences("https://login.bigcommerce.com/login");
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
        SearchResult sR = new SearchResult(list, db);
        Result = sR.pagesResults;
        // for (WebPage WP : Result) {
        // System.out.println("1link " + WP.url);
        // System.out.println("1Count is " + WP.getOutgoinglinks());

        // System.out.println("1previousPRScore is " + WP.getPreviousPRScore());
        // System.out.println("1currentPRScore is " + WP.getCurrentPRScore());
        // System.out.println("1outgoinglinks is " + WP.getIdpointingto());

        // }

        // calculatePopularity(5);

        // for (WebPage WP : Result) {
        // System.out.println("2link " + WP.url);
        // System.out.println("2Count is " + WP.getOutgoinglinks());

        // System.out.println("2previousPRScore is " + WP.getPreviousPRScore());
        // System.out.println("2currentPRScore is " + WP.getCurrentPRScore());
        // System.out.println("2outgoinglinks is " + WP.getIdpointingto());

        // }
        double combinedScore = (5 * sR.getTfIDF()) + sR.getPR();

        sR.setCombinedScore(combinedScore);// sR.setCombinedScore(combinedScore * 10000000);

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
            for (String id : WP.idpointingto) {
                // System.out.println(" id " + id);
                tempScore += (getDoubleValFDownloadedURLs(id, "previousPRScore")
                        / getIntegerVal(id, "linksCount"));
            }
            tempScore = (double) ((1.0 - dampingFactor) / Result.size()) + (dampingFactor * tempScore);
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
        return ((Double) linksIterator.get(searchKey)).doubleValue();

    }

    // helper function to get integer value from db
    private static Integer getIntegerVal(String L, String searchKey) {
        org.bson.Document linksIterator = downloadedURLs.find(eq("url", L)).first();
        return ((Integer) linksIterator.get(searchKey)).intValue();

    }

    private static class ScoreComparator implements Comparator<SearchResult> {

        public int compare(SearchResult a, SearchResult b) {
            return Double.compare(a.getCombinedScore(), b.getCombinedScore());
        }

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
