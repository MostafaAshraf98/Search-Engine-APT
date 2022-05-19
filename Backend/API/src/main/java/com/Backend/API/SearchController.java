package com.Backend.API;

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.text.Document;

import com.Backend.API.QueryProcessorPack.QueryProcessor;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    @GetMapping("/search")
    public Search search(@RequestParam(value = "text", defaultValue = "Hello World") String search) throws IOException {
        System.out.println("Entered the endpoint");

        // MongoClient client = MongoClients.create(
        // "mongodb+srv://Mostafa_98:mostafa123@webcrawler.6mfpo.mongodb.net/myFirstDatabase?retryWrites=true&w=majority");
        MongoClient client = MongoClients.create("mongodb://127.0.0.1:27017");
        // Getting the dataBase from this client.
        MongoDatabase db = client.getDatabase("APTProject");

        ArrayList<String> result = QueryProcessor.QueryProcessor(search, db);

        // ArrayList<String> result = new ArrayList<>();
        // MongoCollection<org.bson.Document> References =
        // db.getCollection("References");
        // // get the first document in the colelction references
        // org.bson.Document document = new org.bson.Document("url", "Hello");
        // References.insertOne(document);

        // result.add(doc.get("url").toString());
        // result.add("https://en.wikipedia.org/wiki/Computer");
        // result.add("https://en.wikipedia.org/wiki/Computer_science");
        // result.add("https://en.wikipedia.org/wiki/Algorithm");

        return new Search(result);
    }

}
