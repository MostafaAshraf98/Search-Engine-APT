package com.Backend.API.rankerPack;

import java.util.ArrayList;
import java.util.Iterator;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.FindIterable;
// Dependency for MongoDB connection 
// import com.mongodb.client.MongoClient;
// import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class SearchResult {
    public ArrayList<WebPage> pagesResults;
    double tfIDF;
    double PR = 0;
    double combinedScore;

    // The collection in MongoDB that contains the downloaded urls data.
    public static MongoCollection<org.bson.Document> downloadedURLs;

    // The collection in MongoDB that containes the referencing urls. to every url
    public static MongoCollection<org.bson.Document> References;

    // send links to be set
    // set the tfIDF and the PR from the IndexerCollection
    public SearchResult(ArrayList<String> links, MongoDatabase db) {
        // Getting the collections from the database.
        downloadedURLs = db.getCollection("downloadedURLs");
        References = db.getCollection("References");

        // WebPage P0 = new WebPage(0, new ArrayList<String>("2"), 0, 0.25, 2);
        // WebPage P1 = new WebPage(1, new ArrayList<String>("0", "2"), 0, 0.25, 1);
        // WebPage P2 = new WebPage(2, new ArrayList<String>("0", "3"), 0, 0.25, 3);
        // WebPage P3 = new WebPage(3, new ArrayList<String>("1", "2"), 0, 0.25, 1);

        pagesResults = new ArrayList<WebPage>(links.size());
        setLinks(links);

        // pagesResults.add(P0);
        // pagesResults.add(P1);
        // pagesResults.add(P2);
        // pagesResults.add(P3);
        // for (WebPage WB : pagesResults) {
        // pagesResults.add(WB);
        // }

    }

    public void setLinks(ArrayList<String> links) {
        WebPage Temp;
        for (String L : links) {
            // System.out.println("link " + L);
            org.bson.Document linksIterator = downloadedURLs.find(eq("url", L)).first();
            Integer outgoingLinksTemp = ((Integer) linksIterator.get("linksCount")).intValue();
            // System.out.println("Count is " + outgoingLinksTemp);

            Double currentScoreTemp = ((Double) linksIterator.get("currentPRScore")).doubleValue();
            // System.out.println("currentScoreTemp is " + currentScoreTemp);

            Double previousScoreTemp = ((Double) linksIterator.get("previousPRScore")).doubleValue();
            // System.out.println("previousScoreTemp is " + previousScoreTemp);

            ArrayList<String> pointingtoLinksTemp = getPointingToLinks(L);
            Temp = new WebPage(0, L, pointingtoLinksTemp, currentScoreTemp, previousScoreTemp, outgoingLinksTemp);
            pagesResults.add(Temp);

        }
    }

    // Function to get Pointing To links for a URL
    public static ArrayList<String> getPointingToLinks(String url) {
        org.bson.Document iterDoc = References.find(eq("url", url)).first();
        if (iterDoc == null) {
            return new ArrayList<String>();
        }
        // System.out.println("val " + iterDoc.get("referencedBy"));
        return (ArrayList<String>) iterDoc.get("referencedBy");

    }

    public double getTfIDF() {
        return tfIDF;
    }

    public void setTfIDF(double tfIDF) {
        this.tfIDF = tfIDF;
    }

    public double getPR() {
        return PR;
    }

    public void setPR(double PR) {
        this.PR = PR;
    }

    public double getCombinedScore() {
        return combinedScore;
    }

    public void setCombinedScore(double combinedScore) {
        this.combinedScore = combinedScore;
    }

}
