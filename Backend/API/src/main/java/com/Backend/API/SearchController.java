package com.Backend.API;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    @GetMapping("/search")
    public Search search(@RequestParam(value = "text", defaultValue = "Hello World") String search) {
        // Call the query Processor that returns an array of strings
        String[] arr = new String[] { "Hello", "World" };

        return new Search(arr);
    }

}
