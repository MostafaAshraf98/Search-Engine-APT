import java.util.*;
import java.io.*;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;

public class Crawler {

	public Object lock = new Object();
	int count = 0;

	public static void main(String[] args) {

		try {
			Scanner s = new Scanner(System.in);
			System.out.println("Please Enter the File name of the Seeds: ");
			String fileName = s.next();
			System.out.println("Please Enter the number of threads: ");
			int numThreads = s.nextInt();
			s.close();

			s = new Scanner(new File(fileName));
			Queue<String> Q = new LinkedList<String>();
			Hashtable<String, Integer> visitedUrl = new Hashtable<String, Integer>();
			Hashtable<String, Integer> compactString = new Hashtable<String, Integer>();
			System.out.println("The seeds initially in the Seeds file are:");
			while (s.hasNextLine()) {
				String line = s.nextLine();
				System.out.println(line);
				Q.add(line);
			}
			s.close();
			Thread[] threadsArr = new Thread[numThreads];
			Crawler crawler = new Crawler();
			for (int i = 0; i < numThreads; i++) {
				// Recursive function
				threadsArr[i] = crawler.new CrawlerThread(Q, compactString, visitedUrl);
				System.out.println("Started the thread number: " + i);
				threadsArr[i].start();
			}
			for (int i = 0; i < numThreads; i++) {
				threadsArr[i].join();
				System.out.println("Thread number" + i + " Is terminated.");
			}
		} catch (Exception e) {
			System.out.println("Error: " + e);
		}

	}

	private class CrawlerThread extends Thread {

		private Queue<String> Q;
		private Hashtable<String, Integer> compactString;
		private Hashtable<String, Integer> visitedUrl;

		CrawlerThread(Queue<String> Q, Hashtable<String, Integer> compactString,
				Hashtable<String, Integer> visitedUrl) {
			this.Q = Q;
			this.compactString = compactString;
			this.visitedUrl = visitedUrl;
		}

		public void run() {
			String url = null;
			while (url == null || visitedUrl.containsKey(URI.create(url).normalize().toString())) {
				if (count >= 5000)
					return;
				synchronized (lock) {
					if (!Q.isEmpty()) {
						url = Q.remove();
					}
				}
			}
			if (url != null)
				crawl(url);
		}

		// State:
		// URL: The URL of the web page.
		// visited: Keep track of the websites that we visited.
		private void crawl(String url) {

			if (count >= 5000)
				return;
			System.out.println("Crawling the url: " + url);
			Document doc = req(url);
			if (doc != null) {
				for (Element link : doc.select("a[href]")) {
					if (count + Q.size() >= 5000)
						break;
					String next_link = link.absUrl("href");
					synchronized (lock) {
						Q.add(next_link);
					}
				}
			}
			String next_url = null;
			while (next_url == null || visitedUrl.containsKey(URI.create(next_url).normalize().toString())) {
				if (count >= 5000)
					return;
				synchronized (lock) {
					if (!Q.isEmpty()) {
						next_url = Q.remove();
					}
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
				// Connection is a data type that comes with the imported Library JSoup.
				Connection con = Jsoup.connect(url);
				Document doc = con.get();
				// Status Code 200 means that the request to visit this WebSite was successful,
				// as it might be rejected due to Robot exlusion protocol.
				if (con.response().statusCode() == 200) {
					// Document is a data type that comes with the imported Library JSoup, It
					// represents the URL's document.
					if (count < 5000) {
						System.out.println("Downloading the url: " + url);
						Boolean result = DownloadWebPage(url, doc.hashCode());
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

		private Boolean DownloadWebPage(String webPage, int title) {
			try {
				URL url = new URL(webPage);
				BufferedReader rd = null;
				BufferedWriter wr = null;
				rd = new BufferedReader(new InputStreamReader(url.openStream()));
				wr = new BufferedWriter(
						new FileWriter(System.getProperty("user.dir") + "/webPages/" + title + ".html"));
				// Read line by line
				String line = rd.readLine();
				StringBuilder contentCompactString = new StringBuilder();
				while (line != null) {
					wr.write(line);
					String trimmedLine = line.trim();
					if (trimmedLine.length() != 0) {
						contentCompactString.append(trimmedLine.charAt(0))
								.append(trimmedLine.charAt(trimmedLine.length() - 1));
					}
					line = rd.readLine();
				}
				rd.close();
				wr.close();
				if (count >= 5000 ||
						compactString.containsKey(contentCompactString.toString())
						|| visitedUrl.containsKey(URI.create(webPage).normalize().toString())) {
					System.out.println("Download unsuccessful because the webPage: " + webPage + " is already visited");
					// Delete the file
					File f = new File(System.getProperty("user.dir") + "/webPages/" + title + ".html");
					f.delete();
					// Return false as the downloading was unsucessfull
					return false;
				}
				synchronized (lock) {
					compactString.put(contentCompactString.toString(), 1);
					visitedUrl.put(URI.create(webPage).normalize().toString(), 1);
					count++;
				}
				System.out.println("Successful Downloadof the webPage: " + webPage);
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

	}
}
