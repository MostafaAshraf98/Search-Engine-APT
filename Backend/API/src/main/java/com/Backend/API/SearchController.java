package com.Backend.API;

import java.io.IOException;
import java.util.ArrayList;

import com.Backend.API.QueryProcessorPack.QueryProcessor;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    @GetMapping("/search")
    public Search search(@RequestParam(value = "text", defaultValue = "Hello World") String search) throws IOException {

        MongoClient client = MongoClients.create(
                "mongodb+srv://Mostafa_98:mostafa123@webcrawler.6mfpo.mongodb.net/myFirstDatabase?retryWrites=true&w=majority");
        // Getting the dataBase from this client.
        MongoDatabase db = client.getDatabase("WebCrawler");

        ArrayList<String> result = QueryProcessor.QueryProcessor(search, db);

        return new Search(result);
    }

}