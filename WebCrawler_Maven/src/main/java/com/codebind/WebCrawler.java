package com.codebind;

import static com.mongodb.client.model.Filters.eq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.Scanner;

// Dependency for MongoDB connection 
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

// Dependency for HTTPClient
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

// Dependency for JSoup html parsing and manipulation
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

// Dependency for URL normalization
import ch.sentric.URL;

// Dependency for robots.txt crawler Commons
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRules.RobotRulesMode;
import crawlercommons.robots.SimpleRobotRulesParser;

public class WebCrawler {

	public Object lock = new Object(); // Serves as a lock to all the thread to avoid any race conditions.
	public static int count = 0; // Counts the number of downloaded webPages.
	final static int TOTAL_NUM_WEBPAGES = 75;
	public static MongoCollection<org.bson.Document> downloadedURLs;
	public static MongoCollection<org.bson.Document> inQueueURLs;

	public static void main(String[] args) {
		try {

			// Create a MongoClient passing the connection String of Mongo Atlas.
			MongoClient client = MongoClients.create(
					"mongodb+srv://Mostafa_98:mostafa123@webcrawler.6mfpo.mongodb.net/myFirstDatabase?retryWrites=true&w=majority");
			// Getting the dataBase from this client.
			MongoDatabase db = client.getDatabase("WebCrawler");
			// Getting the collections from this database.
			downloadedURLs = db.getCollection("downloadedURLs");
			inQueueURLs = db.getCollection("inQueueURLs");

			count = (int) downloadedURLs.countDocuments();

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
				org.bson.Document doc = new org.bson.Document("url", line);
				inQueueURLs.insertOne(doc);
			}
			s.close();
			// Create a new thread pool.
			Thread[] threadsArr = new Thread[numThreads];
			// Create a new Crawler object.
			WebCrawler crawler = new WebCrawler();
			for (int i = 0; i < numThreads; i++) {
				// Create threads with the Crawler object.
				threadsArr[i] = crawler.new CrawlerThread();
				System.out.println("Started the thread number: " + i);
				threadsArr[i].start();
			}
			for (int i = 0; i < numThreads; i++) {
				// Wait for all the threads to finish.
				threadsArr[i].join();
				System.out.println("Thread number" + i + " Is terminated.");
			}

		} catch (Exception e) {
			System.out.println("Error: " + e);
		}

	}

	private class CrawlerThread extends Thread {

		public void run() {
			String url = null;
			URL normalizedURL = null;
			// Keep looking for a url for the thread to start crawling with.
			// Condition to keep looking is that the url is not initialize yet, or this url
			// is already visited. It exits if the number of downloaded webPages is reached.

			while (url == null || downloadedURLs.find(eq("normalizedURL", normalizedURL.getNormalizedUrl()))
					.first() != null) {
				if (count >= TOTAL_NUM_WEBPAGES)
					return;
				if (inQueueURLs.countDocuments() != 0) {
					url = inQueueURLs.findOneAndDelete(null).get("url").toString();
				}

				try {
					normalizedURL = (new URL(url));
				} catch (MalformedURLException e) {
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
			Document doc = null;
			if (!isRobotExcluded(url)) {
				System.out.println("The url is not excluded by robots.txt");
				doc = req(url);
			}
			// doc is null if this url is not valid (visited before or not valid to
			// download).
			if (doc != null) {
				// Loop over all the links in the web page and add them to the to-download
				// Queue.
				for (Element link : doc.select("a[href]")) {
					// If the to-download urls in the queue are sufficient to finish downloading the
					// required number of webPages do not add any more urls.
					if (count + inQueueURLs.countDocuments() >= TOTAL_NUM_WEBPAGES * 2)
						break;
					String next_link = link.absUrl("href");
					org.bson.Document document = new org.bson.Document("url", next_link);
					inQueueURLs.insertOne(document);
				}
			}
			String next_url = null;
			URL normalizedURL = null;
			// Loop until you find a valid(not visited before) url to crawl.
			while (next_url == null || downloadedURLs.find(eq("normalizedURL", normalizedURL.getNormalizedUrl()))
					.first() != null) {
				if (count >= TOTAL_NUM_WEBPAGES)
					return;
				synchronized (lock) {
					if (inQueueURLs.countDocuments() != 0) {
						next_url = inQueueURLs.findOneAndDelete(null).get("url").toString();
					}
				}
				try {
					normalizedURL = (new URL(next_url));
				} catch (MalformedURLException e) {
					next_url = null;
					System.out.println("Error while creating URL from the url: " + url + " " + e);
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
				URL normalizedURL = (new URL(url));
				BufferedWriter wr = null;

				String contentCompactString = compactStringHelper(doc.html());
				if (count >= TOTAL_NUM_WEBPAGES ||
						downloadedURLs.find(eq("compactedContent",
								contentCompactString)).first() != null
						|| downloadedURLs.find(eq("normalizedURL", normalizedURL.getNormalizedUrl()))
								.first() != null) {
					System.out.println("Download unsuccessful because the webPage: " + url + " is already visited");
					return false;
				}
				synchronized (lock) {
					count++;
				}
				wr = new BufferedWriter(
						new FileWriter(System.getProperty("user.dir") + "/webPages/" + doc.hashCode() + ".html"));
				wr.write(doc.html());
				wr.close();
				org.bson.Document document = new org.bson.Document("url", url).append("fileName", doc.hashCode())
						.append("compactedContent", contentCompactString)
						.append("normalizedURL", normalizedURL.getNormalizedUrl());
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
			}
		}

		private String compactStringHelper(String html) {
			StringBuilder contentCompactString = new StringBuilder();
			String trimmedhtml = html.trim();
			for (int i = 0; i < trimmedhtml.length(); i += 10)
				contentCompactString.append(trimmedhtml.charAt(i));
			return contentCompactString.toString();
		}

		private Boolean isRobotExcluded(String url) {

			BaseRobotRules rules = null;
			try {
				CloseableHttpClient httpclient = HttpClients.createDefault();

				final String USER_AGENT = "APTBot";
				java.net.URL urlObj = new java.net.URL(url);
				String hostId = urlObj.getProtocol() + "://" + urlObj.getHost()
						+ (urlObj.getPort() > -1 ? ":" + urlObj.getPort() : "");
				Hashtable<String, BaseRobotRules> robotsTxtRules = new Hashtable<String, BaseRobotRules>();
				rules = robotsTxtRules.get(hostId);
				if (rules == null) {
					HttpGet httpget = new HttpGet(hostId + "/robots.txt");
					HttpContext context = new BasicHttpContext();
					HttpResponse response = httpclient.execute(httpget, context);
					if (response.getStatusLine() != null && response.getStatusLine().getStatusCode() == 404) {
						rules = new SimpleRobotRules(RobotRulesMode.ALLOW_ALL);
						// consume entity to deallocate connection
						EntityUtils.consumeQuietly(response.getEntity());
					} else {
						BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
						SimpleRobotRulesParser robotParser = new SimpleRobotRulesParser();
						rules = robotParser.parseContent(hostId, IOUtils.toByteArray(entity.getContent()),
								"text/plain", USER_AGENT);
					}
					robotsTxtRules.put(hostId, rules);
				}
			} catch (IOException e) {
				System.out.println("Error while fetching the robots.txt file for the url: " + url + " " + e);
				return false;
			}

			return rules.isAllowed(url);
		}
	}
}
