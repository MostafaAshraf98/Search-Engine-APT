package com.Backend.API;

import java.util.ArrayList;

public class Search {

    private ArrayList<String> search;

    public Search(ArrayList<String> search) {
        this.search = search;
    }

    // Add getter
    public ArrayList<String> getSearch() {
        return search;
    }

    // Add setter
    public void setSearch(ArrayList<String> search) {
        this.search = search;
    }

}
