package com.codebind;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import io.mola.galimatias.GalimatiasParseException;
import io.mola.galimatias.URL;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;

// import ch.sentric.*;

public class WebCrawler {

	public Object lock = new Object(); // Serves as a lock to all the thread to avoid any race conditions.
	public static int count = 0; // Counts the number of downloaded webPages.
	final static int TOTAL_NUM_WEBPAGES = 5;
	public static MongoCollection<org.bson.Document> downloadedURLs;
	public static MongoCollection<org.bson.Document> inQueueURLs;
	public static MongoCollection<org.bson.Document> normalizedVisitedURLs;
	public static MongoCollection<org.bson.Document> compactStringURLs;

	public static void main(String[] args) {

		try {

			// Queue to store the webPages to be downloaded.
			Queue<String> Q = new LinkedList<String>();
			// Hashtable to store the webPages that have been downloaded (Avoid visiting
			// same url twice).
			Hashtable<String, Integer> visitedUrl = new Hashtable<String, Integer>();
			// Hashtable to store the compacted downloaded webPages (Avoid visiting same
			// webPage twice - from 2 different urls).
			Hashtable<String, Integer> compactString = new Hashtable<String, Integer>();

			// Create a MongoClient passing the connection String of Mongo Atlas.
			MongoClient client = MongoClients.create(
					"mongodb+srv://Mostafa_98:mostafa123@webcrawler.6mfpo.mongodb.net/myFirstDatabase?retryWrites=true&w=majority");
			// Getting the dataBase from this client.
			MongoDatabase db = client.getDatabase("WebCrawler");
			// Getting the collections from this database.
			downloadedURLs = db.getCollection("downloadedURLs");
			inQueueURLs = db.getCollection("inQueueURLs");
			normalizedVisitedURLs = db.getCollection("normalizedVisitedURLs");
			compactStringURLs = db.getCollection("compactStringURLs");

			count = (int) downloadedURLs.countDocuments();
			FindIterable<org.bson.Document> iterDoc = inQueueURLs.find();
			Iterator<org.bson.Document> it = iterDoc.iterator();
			while (it.hasNext()) {
				org.bson.Document doc = it.next();
				String url = doc.getString("url");
				Q.add(url);
			}

			iterDoc = normalizedVisitedURLs.find();
			it = iterDoc.iterator();
			while (it.hasNext()) {
				org.bson.Document doc = it.next();
				String url = doc.getString("url");
				visitedUrl.put(url, 1);
			}

			iterDoc = compactStringURLs.find();
			it = iterDoc.iterator();
			while (it.hasNext()) {
				org.bson.Document doc = it.next();
				String url = doc.getString("url");
				compactString.put(url, 1);
			}

			// Create a scanner to read from the console.
			Scanner s = new Scanner(System.in);
			// Read the file name of the seeds webPages.
			System.out.println("Please Enter the File name of the Seeds: ");
			String fileName = s.next();
			// Read the number of threads.
			System.out.println("Please Enter the number of threads: ");
			int numThreads = s.nextInt();
			s.close();

			// Create a scanner to read from the seeds file.
			s = new Scanner(new File(fileName));
			System.out.println("The seeds initially in the Seeds file are:");
			// Initialize the queue with the seeds.
			while (s.hasNextLine()) {
				String line = s.nextLine();
				System.out.println(line);
				Q.add(line);
			}
			s.close();
			// Create a new thread pool.
			Thread[] threadsArr = new Thread[numThreads];
			// Create a new Crawler object.
			WebCrawler crawler = new WebCrawler();
			for (int i = 0; i < numThreads; i++) {
				// Create threads with the Crawler object.
				threadsArr[i] = crawler.new CrawlerThread(Q, compactString, visitedUrl);
				System.out.println("Started the thread number: " + i);
				threadsArr[i].start();
			}
			for (int i = 0; i < numThreads; i++) {
				// Wait for all the threads to finish.
				threadsArr[i].join();
				System.out.println("Thread number" + i + " Is terminated.");
			}
			while (!Q.isEmpty()) {
				String urlToVisit = Q.remove();
				if (inQueueURLs.find(eq("url", urlToVisit)).first() == null) {
					org.bson.Document doc = new org.bson.Document("url", urlToVisit);
					inQueueURLs.insertOne(doc);
				}
			}

			Enumeration<String> e = visitedUrl.keys();
			while (e.hasMoreElements()) {

				// Getting the key of a particular entry
				String key = e.nextElement();
				if (normalizedVisitedURLs.find(eq("url", key)).first() == null) {
					org.bson.Document doc = new org.bson.Document("url", key);
					normalizedVisitedURLs.insertOne(doc);
				}

			}

			e = compactString.keys();
			while (e.hasMoreElements()) {

				// Getting the key of a particular entry
				String key = e.nextElement();

				if (compactStringURLs.find(eq("url", key)).first() == null) {
					org.bson.Document doc = new org.bson.Document("url", key);
					compactStringURLs.insertOne(doc);
				}
			}

		} catch (Exception e) {
			System.out.println("Error: " + e);
		}

	}

	private class CrawlerThread extends Thread {

		private Queue<String> Q; // Queue to store the webPages to be downloaded
		private Hashtable<String, Integer> compactString; // Hashtable to store the compacted downloaded
															// webPages.
		private Hashtable<String, Integer> visitedUrl; // Hashtable to store the webPages that have been
														// downloaded.

		CrawlerThread(Queue<String> Q, Hashtable<String, Integer> compactString,
				Hashtable<String, Integer> visitedUrl) {
			// Constructor to initialize the variables.
			this.Q = Q;
			this.compactString = compactString;
			this.visitedUrl = visitedUrl;
		}

		public void run() {
			String url = null;
			String normalizedURL = null;
			// Keep looking for a url for the thread to start crawling with.
			// Condition to keep looking is that the url is not initialize yet, or this url
			// is already visited. It exits if the number of downloaded webPages is reached.

			while (url == null || visitedUrl.containsKey(normalizedURL)) {
				if (count >= TOTAL_NUM_WEBPAGES)
					return;
				synchronized (lock) {
					if (!Q.isEmpty()) {
						url = Q.remove();
					}
				}

				try {
					normalizedURL = URL.parse(url).toString();
				} catch (GalimatiasParseException e) {
					url = null;
					System.out.println("Error while creating URL from the url: " + url + " " + e);

				}

			}
			if (url != null)
				crawl(url);
		}

		// This is a recursive function.
		// State:
		// URL: The URL of the web page to crawl.
		private void crawl(String url) {

			// Base/Stopping condition:
			if (count >= TOTAL_NUM_WEBPAGES)
				return;
			System.out.println("Crawling the url: " + url);
			Document doc = req(url);
			// doc is null if this url is not valid (visited before or not valid to
			// download).
			if (doc != null) {
				// Loop over all the links in the web page and add them to the to-download
				// Queue.
				for (Element link : doc.select("a[href]")) {
					// If the to-download urls in the queue are sufficient to finish downloading the
					// required number of webPages do not add any more urls.
					if (count + Q.size() >= TOTAL_NUM_WEBPAGES)
						break;
					String next_link = link.absUrl("href");
					synchronized (lock) {
						Q.add(next_link);
					}
				}
			}
			String next_url = null;
			String normalizedURL = null;
			// Loop until you find a valid(not visited before) url to crawl.
			while (next_url == null || visitedUrl.containsKey(normalizedURL)) {
				if (count >= TOTAL_NUM_WEBPAGES)
					return;
				synchronized (lock) {
					if (!Q.isEmpty()) {
						next_url = Q.remove();
					}
				}
				try {
					normalizedURL = URL.parse(next_url).toString();
				} catch (GalimatiasParseException e) {
					next_url = null;
					System.out.println("Error while creating URL from the url: " + next_url + " " + e);
				}
			}
			if (next_url != null) {
				crawl(next_url);
			}
		}

		// Helper function:
		// This function requests access to the link and returns a Document of the url
		// Parameters:
		// URL: The URL that we are requesting

		private Document req(String url) {
			// We need the try block because the connection might fail.
			try {
				// Connection and Document are data types that come with the imported Library
				// JSoup.
				Connection con = Jsoup.connect(url);
				Document doc = con.get();
				// Status Code 200 means that the request to visit this WebSite was successful,
				// as it might be rejected due to Robot exlusion protocol.
				if (con.response().statusCode() == 200) {

					if (count < TOTAL_NUM_WEBPAGES) {
						System.out.println("Downloading the url: " + url);
						// Result is a boolean is the download was success or failure.
						Boolean result = DownloadWebPage(url, doc);
						if (result == false)
							return null;
					}
					return doc;
				} else {
					return null;
				}
			} catch (IOException e) {
				System.out.println("IO Exception raised while requesting the url: " + url + " " + e);
				return null;
			} catch (IllegalArgumentException mue) {
				System.out.println("Malformed URL Exception raised while requesting the url: " + url + " " + mue);
				return null;
			}
		}

		private Boolean DownloadWebPage(String url, Document doc) {
			try {
				String normalizedURL = URL.parse(url).toString();
				BufferedWriter wr = null;

				String contentCompactString = compactStringHelper(doc.html());
				if (count >= TOTAL_NUM_WEBPAGES ||
						compactString.containsKey(contentCompactString)
						|| visitedUrl.containsKey(normalizedURL)) {
					System.out.println("Download unsuccessful because the webPage: " + url + " is already visited");
					return false;
				}
				synchronized (lock) {
					compactString.put(contentCompactString, 1);
					visitedUrl.put(normalizedURL, 1);
					count++;
				}
				wr = new BufferedWriter(
						new FileWriter(System.getProperty("user.dir") + "/webPages/" + doc.hashCode() + ".html"));
				wr.write(doc.html());
				wr.close();
				// org.bson.Document document = new org.bson.Document("url",
				// url).append("fileName", doc.hashCode())
				// .append("content", doc.html());
				org.bson.Document document = new org.bson.Document("url", url).append("fileName", doc.hashCode());
				downloadedURLs.insertOne(document);
				System.out.println("Successful Download of the webPage: " + url);
				return true;
			} // Exceptions
			catch (MalformedURLException mue) {
				System.out.println(
						"Malformed URL Exception raised while downloading the webPage: " + url + " " + mue);
				return false;
			} catch (IOException ie) {
				System.out.println("IOException raised While downloading the webPage: " + url + " " + ie);
				return false;
			} catch (StringIndexOutOfBoundsException e) {
				System.out.println(
						"StringIndexOutOfBoundsException raised while downloading the webPage: " + url + " " + e);
				return false;
			} catch (GalimatiasParseException e) {
				System.out.println("Error while creating URL from the url: " + url + " " + e);
				return false;
			}
		}

		private String compactStringHelper(String html) {
			StringBuilder contentCompactString = new StringBuilder();
			String trimmedhtml = html.trim();
			for (int i = 0; i < trimmedhtml.length(); i += 10)
				contentCompactString.append(trimmedhtml.charAt(i));
			return contentCompactString.toString();
		}

	}
}
