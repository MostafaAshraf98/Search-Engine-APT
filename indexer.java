import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class indexer
{
    static Map<String,ArrayList<ArrayList<String>>> index = new HashMap<String,ArrayList<ArrayList<String>>>();

    public static void main(String[] args) throws IOException
    {
        String filename = "index.html";
        Document doc = readHTMLFile(filename);
        Element body = doc.select("body").first();
        Element head = doc.select("head").first();
        if (!(head.hasText()))
        {
            System.out.println("No head tag found");
            head = null;
        }
        
        if (!(body.hasText()))
        {
            System.out.println("No body tag found");
            body = null;
        }

    }

    private static Document readHTMLFile(String fileName) throws IOException
    {
        File htmlFile = new File("./" + fileName);
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        //System.out.println(doc);
        return doc;
    }
    
}