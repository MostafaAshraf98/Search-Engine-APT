package com.codebind.rankerPack;

public class WebPage {
    public Integer id;
    public String[] idpointingto;
    public double currentPRScore;
    public double previousPRScore;
    public Integer outgoinglinks;
    public Integer[] outgoingIDs;

    public WebPage(Integer id, String[] idpointingto, double currentPRScore, double previousPRScore,
            Integer outgoinglinks, Integer[] outgoingIDs) {
        this.id = id;
        this.idpointingto = idpointingto;
        this.currentPRScore = currentPRScore;
        this.previousPRScore = previousPRScore;
        this.outgoinglinks = outgoinglinks;
        this.outgoingIDs = outgoingIDs;
    }
}
