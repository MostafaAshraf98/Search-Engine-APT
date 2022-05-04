import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
// import java.net.URI;
// import java.net.URL;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import ch.sentric.*;

public class Crawler {

	public Object lock = new Object(); // Serves as a lock to all the thread to avoid any race conditions.
	int count = 0; // Counts the number of downloaded webPages.
	final static int TOTAL_NUM_WEBPAGES = 5000;

	public static void main(String[] args) {

		try {
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
			// Queue to store the webPages to be downloaded.
			Queue<String> Q = new LinkedList<String>();
			// Hashtable to store the webPages that have been downloaded (Avoid visiting
			// same url twice).
			Hashtable<String, Integer> visitedUrl = new Hashtable<String, Integer>();
			// Hashtable to store the compacted downloaded webPages (Avoid visiting same
			// webPage twice - from 2 different urls).
			Hashtable<String, Integer> compactString = new Hashtable<String, Integer>();
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
			Crawler crawler = new Crawler();
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
			URL normalizedURL = null;
			// Keep looking for a url for the thread to start crawling with.
			// Condition to keep looking is that the url is not initialize yet, or this url
			// is already visited. It exits if the number of downloaded webPages is reached.

			while (url == null || visitedUrl.containsKey(normalizedURL.getNormalizedUrl())) {
				if (count >= TOTAL_NUM_WEBPAGES)
					return;
				synchronized (lock) {
					if (!Q.isEmpty()) {
						url = Q.remove();
					}
				}
				try {
					normalizedURL = new URL(url);
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
			URL normalizedURL = null;
			// Loop until you find a valid(not visited before) url to crawl.
			while (next_url == null || visitedUrl.containsKey(normalizedURL.getNormalizedUrl())) {
				if (count >= TOTAL_NUM_WEBPAGES)
					return;
				synchronized (lock) {
					if (!Q.isEmpty()) {
						next_url = Q.remove();
					}
				}
				try {
					normalizedURL = new URL(next_url);
				} catch (MalformedURLException e) {
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
						Boolean result = DownloadWebPage(url, doc.hashCode(), doc);
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

		private Boolean DownloadWebPage(String webPage, int title, Document doc) {
			try {
				URL normalizedURL = new URL(webPage);
				URL url = new URL(webPage);

				BufferedReader rd = null;
				BufferedWriter wr = null;
				rd = new BufferedReader(new InputStreamReader(url.getURI().openStream()));
				wr = new BufferedWriter(
						new FileWriter(System.getProperty("user.dir") + "/webPages/" + title + ".html"));
				// Read line by line
				String line = rd.readLine();
				StringBuilder contentCompactString = new StringBuilder();
				while (line != null) {
					// wr.write(line);
					String trimmedLine = line.trim();
					if (trimmedLine.length() != 0) {
						contentCompactString.append(trimmedLine.charAt(0))
								.append(trimmedLine.charAt(trimmedLine.length() - 1));
					}
					line = rd.readLine();
				}

				if (count >= TOTAL_NUM_WEBPAGES ||
						compactString.containsKey(contentCompactString.toString())
						|| visitedUrl.containsKey(normalizedURL.getNormalizedUrl())) {
					System.out.println("Download unsuccessful because the webPage: " + webPage + " is already visited");
					// Delete the file
					// File f = new File(System.getProperty("user.dir") + "/webPages/" + title +
					// ".html");
					// f.delete();
					// Return false as the downloading was unsucessfull
					return false;
				}
				synchronized (lock) {
					compactString.put(contentCompactString.toString(), 1);
					visitedUrl.put(normalizedURL.getNormalizedUrl(), 1);
					count++;
				}
				System.out.println("Successful Download of the webPage: " + webPage);
				wr.write(doc.html());
				rd.close();
				wr.close();
				return true;
			} // Exceptions
			catch (MalformedURLException mue) {
				System.out.println(
						"Malformed URL Exception raised while downloading the webPage: " + webPage + " " + mue);
				return false;
			} catch (IOException ie) {
				System.out.println("IOException raised While downloading the webPage: " + webPage + " " + ie);
				return false;
			} catch (StringIndexOutOfBoundsException e) {
				System.out.println(
						"StringIndexOutOfBoundsException raised while downloading the webPage: " + webPage + " " + e);
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
