package com.codebind.rankerPack;

public class WebPage {
    public Integer id;
    public String[] idpointingto;
    public double currentPRScore;
    public double previousPRScore;
    public Integer outgoinglinks;

    // set data from downloaded URLS and then wehn updating get data from downloaded
    // URLS
    public WebPage(Integer id, String[] idpointingto, double currentPRScore, double previousPRScore,
            Integer outgoinglinks) {
        this.id = id;
        this.idpointingto = idpointingto;
        this.currentPRScore = currentPRScore;
        this.previousPRScore = previousPRScore;
        this.outgoinglinks = outgoinglinks;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String[] getIdpointingto() {
        return idpointingto;
    }

    public void setIdpointingto(String[] idpointingto) {
        this.idpointingto = idpointingto;
    }

    public double getCurrentPRScore() {
        return currentPRScore;
    }

    public void setCurrentPRScore(double currentPRScore) {
        this.currentPRScore = currentPRScore;
    }

    public double getPreviousPRScore() {
        return previousPRScore;
    }

    public void setPreviousPRScore(double previousPRScore) {
        this.previousPRScore = previousPRScore;
    }

    public Integer getOutgoinglinks() {
        return outgoinglinks;
    }

    public void setOutgoinglinks(Integer outgoinglinks) {
        this.outgoinglinks = outgoinglinks;
    }

}
