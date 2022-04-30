import java.util.ArrayList;
import java.util.*;
import java.io.*;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.net.MalformedURLException;

public class Crawler {

	Object lock;
	int count = 0;

	public static void main(String[] args) throws InterruptedException, FileNotFoundException {

		Scanner s = new Scanner(System.in);
		System.out.println("Please Enter the File name of the Seeds: ");
		String fileName = s.next();
		System.out.println("Please Enter the number of threads: ");
		int numThreads = s.nextInt();
		s.close();

		s = new Scanner(new File(fileName));
		Queue<String> Q = new LinkedList<String>();
		Hashtable<String, Integer> visited = new Hashtable<String, Integer>();

		while (s.hasNextLine()) {
			String line = s.nextLine();
			Q.add(line);
		}
		Thread[] threadsArr = new Thread[numThreads];
		Crawler crawler = new Crawler();
		for (int i = 0; i < numThreads; i++) {
			// Recursive function
			threadsArr[i] = crawler.new CrawlerThread(Q, visited);
			threadsArr[i].start();
		}
		for (int i = 0; i < numThreads; i++) {
			threadsArr[i].join();
		}

	}

	private class CrawlerThread extends Thread {

		private Queue<String> Q;
		private Hashtable<String, Integer> visited;

		CrawlerThread(Queue<String> Q, Hashtable<String, Integer> visited) {
			this.Q = Q;
			this.visited = visited;
		}

		public void run() {
			String url = null;
			synchronized (lock) {
				if (!Q.isEmpty()) {
					url = Q.remove();
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
			String next_url;
			synchronized (lock) {
				if (Q.isEmpty())
					return;
				next_url = Q.remove();
			}
			if (!visited.containsKey(next_url))
				crawl(next_url);

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
				// Status Code 200 means that the request to visit this WebSite was successful,
				// as it might be rejected due to Robot exlusion protocol.
				if (con.response().statusCode() == 200) {
					// Document is a data type that comes with the imported Library JSoup, It
					// represents the URL's document.
					Document doc = con.get();
					System.out.println("Link: " + url);
					System.out.println(doc.title());
					visited.put(url);
					return doc;
				} else {
					return null;
				}
			} catch (IOException e) {
				return null;
			}
		}

		private void DownloadWebPage(String webPage) throws IOException {
			URL url = new URL(webPage);
			BufferedReader rd = null;
			BufferedWriter wr = null;
			try {
				rd = new BufferedReader(new InputStreamReader(url.openStream()));
				// The file name where we want to download the webpage
				wr = new BufferedWriter(new FileWriter("./webPages/" + url.toString() + ".html"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Read line by line
			String line = rd.readLine();
			while (line != null) {
				wr.write(line);
				line = rd.readLine();
			}
			rd.close();
			wr.close();
		}

	}
}
