package com.Backend.API;

public class Search {

    private String[] search;

    public Search(String[] search) {
        this.search = search;
    }

    // Add getter
    public String[] getSearch() {
        return search;
    }

    // Add setter
    public void setSearch(String[] search) {
        this.search = search;
    }

}
