package com.codebind.rankerPack;

import java.util.Arrays;

public class PageRank {
    public static WebPage[] Result;
    public static double[] vec_PR;
    public static double[][] H;

    public static final double dampingFactor = 0.85;

    public PageRank() {
    }

    public static void rank() {
        SearchResult sR = new SearchResult();
        Result = sR.searchResults;

        calculatePopularity(2);
        for (WebPage WP : Result) {
            System.out.println(" id " + WP.id + " pointed to by " +
                    Arrays.toString((WP.idpointingto)) + " Scores "
                    + WP.currentPRScore + " " + WP.previousPRScore);
        }
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
                tempScore += (Result[Integer.parseInt(id)].previousPRScore
                        / Result[Integer.parseInt(id)].outgoinglinks);
            }
            tempScore = (double) ((1.0 - dampingFactor) / Result.length) + (dampingFactor * tempScore);
            WP.currentPRScore = tempScore;
        }
        for (WebPage WP : Result) {
            WP.previousPRScore = WP.currentPRScore;
        }
    }

    public static void PRCalcMatrix() {
        vec_PR = new double[Result.length];
        H = new double[Result.length][Result.length];
        for (int i = 0; i < Result.length; i++) {
            for (int j = 0; j < Result.length; j++) {
                H[i][j] = 0;
            }

        }

        /* Initialize the PR vector */
        for (int i = 0; i < Result.length; i++) {
            vec_PR[i] = Result[i].previousPRScore;
        }
        /* Initialize the H vector */
        for (int i = 0; i < Result.length; i++) {
            for (int j : Result[i].outgoingIDs) {
                H[j][i] = (double) (1.0 / Result[i].outgoingIDs.length);
            }
        }

        for (int i = 0; i < 2; i++) {
            // vec_PR = H * vec_PR;
        }
    }
}
