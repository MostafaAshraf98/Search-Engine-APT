package com.codebind.webCrawlerPack;

import static com.mongodb.client.model.Filters.eq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Set;

// Dependency for MongoDB connection 
// import com.mongodb.client.MongoClient;
// import com.mongodb.client.MongoClients;
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

	// Global Variables:

	// Serves as a lock to all the thread to avoid any race conditions (Common
	// variable is the count).
	// public Object lock = new Object();

	// The total required number of crawled webpages.
	final static int TOTAL_NUM_WEBPAGES = 500;

	// Maps every hostID to the robot.txt rules in it.
	static Hashtable<String, BaseRobotRules> robotsTxtRules;

	// The collection in MongoDB that contains the downloaded urls data.
	public static MongoCollection<org.bson.Document> downloadedURLs;

	// The collection in MongoDB that contains the urls to crawl next (FIFO).
	public static MongoCollection<org.bson.Document> inQueueURLs;

	// The collection in MongoDB that containes the referencing urls. to every url
	public static MongoCollection<org.bson.Document> References;

	// Main Crawling Function.
	public static void Web(String[] args, MongoDatabase db) {
		try {

			robotsTxtRules = new Hashtable<String, BaseRobotRules>();

			// Create a MongoClient passing the connection String of Mongo Atlas.
			// MongoClient client = MongoClients.create(
			// "mongodb+srv://Mostafa_98:mostafa123@webcrawler.6mfpo.mongodb.net/myFirstDatabase?retryWrites=true&w=majority");
			// // Getting the dataBase from this client.
			// MongoDatabase db = client.getDatabase("WebCrawler");

			// Getting the collections from the database.
			downloadedURLs = db.getCollection("downloadedURLs");
			inQueueURLs = db.getCollection("inQueueURLs");
			References = db.getCollection("References");

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
			// Add the seeds to the Queue (The collection in the dataBase).
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
			// Create threads with the number that the user entered.
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

	// Class for the crawling threads
	private class CrawlerThread extends Thread {

		// Overrides the run function.
		public void run() {
			String url = null;
			URL normalizedURL = null;
			// Keep looking for a url for the thread to start crawling with.
			// Conditions to keep looking are:
			// 1- The url is not initialize yet.
			// 2- This url is already visited.
			// It also exits if the number of downloaded webPages is reached.

			while (url == null || downloadedURLs.find(eq("normalizedURL", normalizedURL.getNormalizedUrl()))
					.first() != null) {
				if ((int) downloadedURLs.countDocuments() >= TOTAL_NUM_WEBPAGES)
					return;
				if (inQueueURLs.countDocuments() != 0) {
					// FIFO so we take the first document (sorted with the objectID) so the filter
					// is NULL
					try {
						url = inQueueURLs.findOneAndDelete(null).get("url").toString();
					} catch (NullPointerException e) {
						// System.out.println("Error while getting the next url from the queue: " + e);
					}
				}

				try {
					if (url != null)
						normalizedURL = (new URL(url));
				} catch (MalformedURLException e) {
					url = null;
					// System.out.println("Error while creating URL from the url: " + url + " " +
					// e);
				}

			}
			if (url != null)
				crawl(url);
		}

		// This is a recursive function.
		private void crawl(String url) {

			// Base/Stopping condition:
			if (downloadedURLs.countDocuments() >= TOTAL_NUM_WEBPAGES)
				return;
			// System.out.println("Crawling the url: " + url);
			// Request this document.
			Document doc = req(url);
			// doc is null if this url is not valid (visited before or not valid to
			// download).
			if (doc != null) {
				// Loop over all the links in the web page and add them to the to-download
				// Queue.
				for (Element link : doc.select("a[href]")) {
					// If the to-download urls in the queue are sufficient (the double) to
					// finish downloading the required number of webPages do not add any more urls.
					if (downloadedURLs.countDocuments() + inQueueURLs.countDocuments() >= TOTAL_NUM_WEBPAGES * 2)
						break;
					String next_link = link.absUrl("href");
					org.bson.Document document = new org.bson.Document("url", next_link);
					inQueueURLs.insertOne(document);
					// Add this url to the referencing of the next_link.
					// If it this the first time to meet this link.
					// Then add a new document to the collection.
					if (References.find(eq("url", next_link)).first() == null) {
						Set<String> set = new HashSet<String>();
						set.add(url);
						org.bson.Document doc2 = new org.bson.Document("url", next_link).append("referencedBy", set);
						References.insertOne(doc2);
					} else {
						// update the collection with the new referencedBy by adding it to the array.
						References.updateOne(eq("url", next_link),
								new org.bson.Document("$addToSet", new org.bson.Document("referencedBy", url)));
					}
				}
			}
			String next_url = null;
			URL normalizedURL = null;

			// Keep looking for a url for the thread to start crawling with.
			// Conditions to keep looking are:
			// 1- The url is not initialize yet.
			// 2- This url is already visited.
			// It also exits if the number of downloaded webPages is reached.
			while (next_url == null || downloadedURLs.find(eq("normalizedURL", normalizedURL.getNormalizedUrl()))
					.first() != null) {
				if (downloadedURLs.countDocuments() >= TOTAL_NUM_WEBPAGES)
					return;
				if (inQueueURLs.countDocuments() != 0) {
					try {
						next_url = inQueueURLs.findOneAndDelete(null).get("url").toString();
					} catch (NullPointerException e) {
						// System.out.println("Error while getting the next url from the queue: " + e);
					}
				}
				try {
					if (next_url != null)
						normalizedURL = (new URL(next_url));
				} catch (MalformedURLException e) {
					next_url = null;
					// System.out.println("Error while creating URL from the url: " + url + " " +
					// e);
				}

			}
			if (next_url != null) {
				crawl(next_url);
			}
		}

		// Helper function:
		// This function requests access to the link and returns a Document of the url
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

					if (downloadedURLs.countDocuments() < TOTAL_NUM_WEBPAGES) {
						// System.out.println("Downloading the url: " + url);
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
				// System.out.println("IO Exception raised while requesting the url: " + url + "
				// " + e);
				return null;
			} catch (IllegalArgumentException mue) {
				// System.out.println("Malformed URL Exception raised while requesting the url:
				// " + url + " " + mue);
				return null;
			}
		}

		private Boolean DownloadWebPage(String url, Document doc) {
			try {
				URL normalizedURL = (new URL(url));
				BufferedWriter wr = null;

				// Returns the compacted string of the content of this url (The DOM)
				String contentCompactString = compactStringHelper(doc.html());
				if (downloadedURLs.find(eq("compactedContent",
						contentCompactString)).first() != null) {
					// System.out.println("Download unsuccessful because the webPage: " + url
					// + " is already visited (FOUND SAME COMPACT STRING)");
					return false;
				} else if (downloadedURLs.find(eq("normalizedURL", normalizedURL.getNormalizedUrl()))
						.first() != null) {
					// System.out.println("Download unsuccessful because the webPage: " + url
					// + " is already visited (FOUND SAME NORMALIZED URL)");
					return false;
				} else if (downloadedURLs.countDocuments() >= TOTAL_NUM_WEBPAGES) {
					// System.out.println("Download unsuccessful because we reached the FINAL
					// COUNT");
					return false;
				} else if (isRobotExcluded(url)) {
					// System.out.println("Download unsuccessful because The url is EXCLUDED BY
					// ROBOT.TXT");
					return false;
				}
				// Write the html content (DOM) in a file with a unique name = to the hashcode
				// of this document
				wr = new BufferedWriter(
						new FileWriter(System.getProperty("user.dir") + "/webPages/" + doc.hashCode() + ".html"));
				wr.write(doc.html());
				wr.close();
				// Save this downloaded Webpage in the database
				// with the fields url, filname, compactedString and normalizedURL
				org.bson.Document document = new org.bson.Document("url", url)
						.append("normalizedURL", normalizedURL.getNormalizedUrl())
						.append("fileName", doc.hashCode())
						.append("linksCount", doc.select("a[href]").size())
						.append("compactedContent", contentCompactString)
						.append("currentPRScore", 0.0)
						.append("previousPRScore", 1.0 / TOTAL_NUM_WEBPAGES);

				downloadedURLs.insertOne(document);
				System.out.println("Successful Download of the webPage: " + url);
				return true;
			} // Exceptions
			catch (MalformedURLException mue) {
				// System.out.println(
				// "Malformed URL Exception raised while downloading the webPage: " + url + " "
				// + mue);
				return false;
			} catch (IOException ie) {
				// System.out.println("IOException raised While downloading the webPage: " + url
				// + " " + ie);
				return false;
			} catch (StringIndexOutOfBoundsException e) {
				// System.out.println(
				// "StringIndexOutOfBoundsException raised while downloading the webPage: " +
				// url + " " + e);
				return false;
			}
		}

		// This is a helper function that compacts the content of html page by appending
		// 1 char every 10 chars
		private String compactStringHelper(String html) {
			StringBuilder contentCompactString = new StringBuilder();
			String trimmedhtml = html.trim();
			for (int i = 0; i < trimmedhtml.length(); i += 100)
				contentCompactString.append(trimmedhtml.charAt(i));
			return contentCompactString.toString();
		}

		// Function that Checks if this url is robot excluded.
		// Returns true is it is excluded (cannot be crawled).
		private Boolean isRobotExcluded(String url) {

			// Initially the robot rules of this webpage is null.
			BaseRobotRules rules = null;
			try {
				// Creates CloseableHttpClient instance with default configuration.
				CloseableHttpClient httpclient = HttpClients.createDefault();

				// Arbitrary name for our robot/Agent.
				final String USER_AGENT = "APTBot";
				// Creates CloseableHttpClient instance with default configuration.
				java.net.URL urlObj = new java.net.URL(url);
				// Get the host ID.
				String hostId = urlObj.getProtocol() + "://" + urlObj.getHost()
						+ (urlObj.getPort() > -1 ? ":" + urlObj.getPort() : "");

				// Check in the global Hashtable if the rules of this host is already stored
				rules = robotsTxtRules.get(hostId);
				// If the rules of this host are not stored (First time to encounter this host
				// ID)
				if (rules == null) {
					// The request to be executed.
					HttpGet httpget = new HttpGet(hostId + "/robots.txt");

					// The default http context
					HttpContext context = new BasicHttpContext();

					// Executes HTTP request using the given context.
					// request: the request to execute
					// context the context to use for the execution, or null to use the default
					HttpResponse response = httpclient.execute(httpget, context);

					// If There is no robot.txt
					if (response.getStatusLine() != null && response.getStatusLine().getStatusCode() == 404) {
						// Then it this host allows to crawl the webPage
						rules = new SimpleRobotRules(RobotRulesMode.ALLOW_ALL);

						// consume entity to deallocate connection
						EntityUtils.consumeQuietly(response.getEntity());
					} else {
						// Creates a new buffered entity wrapper.
						BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
						SimpleRobotRulesParser robotParser = new SimpleRobotRulesParser();
						// parse the Robot rules.
						// Parse the robots.txt file in content, and return rules appropriate for
						// processing paths by userAgent.
						rules = robotParser.parseContent(hostId, IOUtils.toByteArray(entity.getContent()),
								"text/plain", USER_AGENT);
					}
					// Add the rules of this host to the map.
					robotsTxtRules.put(hostId, rules);
				}
			} catch (IOException e) {
				// System.out.println("Error while fetching the robots.txt file for the url: " +
				// url + " " + e);
				return false;
			}
			// return whether it is allowed or not to crawl this url.
			return !rules.isAllowed(url);
		}
	}
}
