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

    static String webpagesPath = "./webPages/";
    public static void main(String[] args) throws IOException
    {
        var docs = readAllHTML();
        for (var doc : docs)
        {
            //Apply logic here and add to database
        }

    }

    private static ArrayList<Document> readAllHTML() throws IOException
    {
        ArrayList<Document> docs = new ArrayList<Document>();
        File folder = new File(webpagesPath);
        File[] listOfFiles = folder.listFiles();
        System.out.println("Number of files: " + listOfFiles.length);
        for (File file : listOfFiles)
        {
            if (file.isFile())
            {
                System.out.println(file.getName());
                Document doc = readHTMLFile(webpagesPath +file.getName());
                docs.add(doc);
            }
        }
        return docs;
    }

    private static Document readHTMLFile(String fileName) throws IOException
    {
        File htmlFile = new File(fileName);
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        return doc;
    }
    
}