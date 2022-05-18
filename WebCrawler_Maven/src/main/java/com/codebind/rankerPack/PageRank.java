package com.codebind.rankerPack;

import java.util.ArrayList;
import java.util.Arrays;
import static com.mongodb.client.model.Filters.eq;

import java.util.Iterator;

import com.mongodb.client.FindIterable;
// Dependency for MongoDB connection 
// import com.mongodb.client.MongoClient;
// import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
// import com.mongodb.client.model.Projections;

// import org.bson.conversions.Bson;

public class PageRank {
    public static WebPage[] Result;
    public static double[] vec_PR;
    public static double[][] H;

    public static final double dampingFactor = 0.85;

    // The collection in MongoDB that contains the downloaded urls data.
    public static MongoCollection<org.bson.Document> downloadedURLs;

    // The collection in MongoDB that containes the referencing urls. to every url
    public static MongoCollection<org.bson.Document> References;

    public PageRank() {
    }

    public static void rank(String[] args, MongoDatabase db, SearchResult sR) {

        // Getting the collections from the database.
        downloadedURLs = db.getCollection("downloadedURLs");
        References = db.getCollection("References");

        // Getting the search results from the SearchResult class.
        ArrayList<String> list = getPointingToLinks("https://www.bbc.co.uk");
        for (String L : list) {
            System.out.println("link " + L);
            System.out.println("Count is  " + getOutgoingLinksCount(L));
            FindIterable<org.bson.Document> referencedIterator = downloadedURLs
                    .find(eq("url", L));
            Iterator it = referencedIterator.iterator();
            if (it.hasNext()) {
                org.bson.Document filepointingToUrlObject = (org.bson.Document) it.next();
                System.out.println("previousPRScore is " + filepointingToUrlObject.get("previousPRScore"));
                System.out.println("currentPRScore is " + filepointingToUrlObject.get("currentPRScore"));

            }

        }

        // SearchResult sR = new SearchResult();
        Result = sR.searchResults;

        calculatePopularity(5);

        double combinedScore = (5 * sR.getTfIDF()) + sR.getPR();

        sR.setCombinedScore(combinedScore);// sR.setCombinedScore(combinedScore * 10000000);

        for (WebPage WP : Result) {
            System.out.println(" id " + WP.id + " pointed to by " +
                    Arrays.toString((WP.idpointingto)) + " Scores "
                    + WP.currentPRScore + " " + WP.previousPRScore);
        }
    }

    // Set Combined Score

    // Function to get Pointing To links for a URL
    public static ArrayList<String> getPointingToLinks(String url) {
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
                tempScore += (Result[Integer.parseInt(id)].getPreviousPRScore()
                        / Result[Integer.parseInt(id)].getOutgoinglinks());
            }
            tempScore = (double) ((1.0 - dampingFactor) / Result.length) + (dampingFactor * tempScore);
            WP.setCurrentPRScore(tempScore);
        }
        for (WebPage WP : Result) {
            // WP.previousPRScore = WP.currentPRScore;
            WP.setPreviousPRScore(WP.getCurrentPRScore());
        }
    }

    // private static class ScoreComparator implements Comparator<searchResults> {

    // public int compare(searchResults a, searchResults b) {
    // return Double.compare(a.getCombinedScore(), b.getCombinedScore());
    // }

    // }

    public static void PRCalcMatrix() {
        vec_PR = new double[Result.length];
        H = new double[Result.length][Result.length];
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
