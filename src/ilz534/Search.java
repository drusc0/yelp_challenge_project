package ilz534;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

/**
 * Search.java
 * 
 * Performs a search on the index built for the purpose to find similar labels
 * or categories for the remaining 30% businesses. We use this subset to see how
 * accurate reviews and/or texts can be in labeling a business as some yelp
 * category
 * 
 * @author drusc0
 */
public class Search {

	private static final String PATH = "/Volumes/SEAGATE1TB 1/Yelp/index";
	private Connector con;
	private IndexReader reader;
	private IndexSearcher searcher;
	private Analyzer analyzer;
	private List<String> unprocessedRevs;

	/**
	 * establishes connection with mongodb and initialized reader, searcher, and
	 * analyzer
	 * 
	 * @throws IOException
	 */
	public Search() throws IOException {
		this.con = new Connector();
		this.reader = DirectoryReader.open(FSDirectory.open(Paths.get(PATH)));
		this.searcher = new IndexSearcher(this.reader);
		this.analyzer = new StandardAnalyzer();
		// set searcher to best match similarity model
		this.searcher.setSimilarity(new BM25Similarity());

		this.unprocessedRevs = new ArrayList<String>();
	}

	/**
	 * getTestingSet() we retrieve the remaining 30% of businesses to test
	 * accuracy of reviews for labeling categories for incoming businesses.
	 * 
	 * @return testingSet
	 */
	public List<org.bson.Document> getTestingSet() {
		List<org.bson.Document> testingSet = this.con.getBusinessTestingSet();
		return testingSet;
	}

	/**
	 * parseQuery Parses the text in each business tip and/or review
	 * 
	 * @param queryString
	 * @param field
	 *            - TIP|REVIEW
	 * @return terms
	 * @throws IOException
	 * @throws ParseException
	 */
	public Set<Term> parseQuery(String queryString, String field)
			throws IOException, ParseException {

		Set<Term> terms = new LinkedHashSet<Term>();
		QueryParser parser = new QueryParser(field, this.analyzer);
		Query query = parser.parse(queryString);
		this.searcher.createNormalizedWeight(query, false).extractTerms(terms);
		return terms;
	}

	public void rankDoccuments(String field, int numberOfHits, String path)
			throws IOException, ParseException {

		QueryParser parser = new QueryParser(field, this.analyzer);

		List<org.bson.Document> testingSet = this.getTestingSet();
		// extract the remaining dataset (testing)
		for (org.bson.Document doc : testingSet) {

			String docID = doc.getString("business_id");
			List<org.bson.Document> list = new ArrayList<org.bson.Document>();

			// get review list based on doc ids
			if (field.equals("REVIEW")) {
				// List<org.bson.Document> reviewList =
				// this.getReviewList(docID);
				list = this.getReviewList(docID);
			} else if (field.equals("TIP")) {
				// List<org.bson.Document> tipList = this.getReviewList(docID);
				list = this.getTipList(docID);
			}
			String txt = this.getText(list);

			// parse queries
			try {
				Query query = parser.parse(QueryParser.escape(txt));
				// get top 1000 results

				TopDocs results = this.searcher.search(query, numberOfHits);
				ScoreDoc[] hits = results.scoreDocs;
				List<Entry<String, Double>> hitsProcessed = processHits(hits);
				try {
					writeToFile(docID, hitsProcessed, path);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				this.unprocessedRevs.add(docID);
				// DO NOTHING, SKIP
			}

		}
	}

	/**
	 * writeToFile writes entries to a file for analysis
	 * 
	 * @param docID
	 * @param entries
	 * @param path
	 * @throws IOException
	 */
	public void writeToFile(String docID, List<Entry<String, Double>> entries,
			String path) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(path, true));
		bw.write("<ID>" + docID + "</ID>\n");
		bw.write("<Categories>\n");
		for (Entry<String, Double> entry : entries) {
			System.out.println(entry.toString());
			bw.write(entry.toString() + "\n");
		}
		bw.write("</Categories>\n");
		bw.close();
	}

	/**
	 * processHits produces a list of unique categories that are candidates to
	 * be category for the document being processed
	 * 
	 * @param hits
	 * @return listProcessedHits
	 * @throws IOException
	 */
	public List<Entry<String, Double>> processHits(ScoreDoc[] hits)
			throws IOException {
		HashMap<String, Double> labelScore = new HashMap<String, Double>();
		List<Entry<String, Double>> lst;

		for (int i = 0; i < hits.length; i++) {

			Document document = this.searcher.doc(hits[i].doc);

			for (String category : this.con.getCategory(document.get("DOCNO"))) {
				// if it exists get an average
				if (labelScore.containsKey(category)) {
					double val = labelScore.get(category);
					val += hits[i].score;
					val = val / 2;
					labelScore.replace(category, val);
					// if it is a new element add to list with score of document
				} else {
					labelScore.put(category, (double) hits[i].score);
				}
			}
		}

		return (lst = sort(labelScore));
	}

	/**
	 * sort sorts the items in the hash map in decreasing order
	 * 
	 * @param map
	 * @return sortedEntries
	 */
	private List<Entry<String, Double>> sort(Map<String, Double> map) {
		List<Entry<String, Double>> sortedEntries = new ArrayList<Entry<String, Double>>(
				map.entrySet());

		Collections.sort(sortedEntries,
				new Comparator<Entry<String, Double>>() {

					public int compare(Entry<String, Double> o1,
							Entry<String, Double> o2) {
						return o2.getValue().compareTo(o1.getValue());
					}

				});
		return sortedEntries;
	}

	/**
	 * removeStopWords delete stop words from query string
	 * 
	 * @param textFile
	 * @return string - string with no stopwords
	 * @throws Exception
	 */
	public String removeStopWords(String textFile) throws Exception {
		int[] randomSet = generate4Random(textFile);
		Analyzer analyzer = new StandardAnalyzer();
		TokenStream ts = analyzer.tokenStream("content", textFile);
		StringBuilder builder = new StringBuilder();
		CharTermAttribute charTerm = ts.addAttribute(CharTermAttribute.class);

		try {
			ts.reset(); // Resets this stream to the beginning. (Required)
			int counter = 0;
			while (ts.incrementToken()) {
				if (counter == randomSet[0] || counter == randomSet[1]
						|| counter == randomSet[2] || counter == randomSet[3]) {
					builder.append(charTerm.toString() + " ");
				}
				counter++;
			}
			ts.end(); // Perform end-of-stream operations, e.g. set the final
						// offset.
		} finally {
			ts.close(); // Release resources associated with this stream.
		}
		return builder.toString();
	}

	/**
	 * generate4random generate 4 unique random integers
	 * 
	 * @param text
	 * @return array of 4 ints
	 */
	public int[] generate4Random(String text) {
		int size = text.toCharArray().length;
		Random random = new Random();
		int[] ints = new int[4];

		for (int i = 0; i < 4; i++) {
			ints[i] = random.nextInt(size);
			for (int j = i; j > 0; j--) {
				if (ints[i] == ints[j]) {
					ints[i] = random.nextInt(size);
				}
			}
		}
		return ints;
	}

	/**
	 * getReviewList passes the business id to find the reviews for a specific
	 * business
	 * 
	 * @param businessID
	 * @return reviewList
	 */
	public List<org.bson.Document> getReviewList(String businessID) {
		List<org.bson.Document> reviewList = this.con.getReviewCollection()
				.find(new org.bson.Document("business_id", businessID))
				.into(new ArrayList<org.bson.Document>());
		return reviewList;
	}

	/**
	 * getTipList passes the business id to find the reviews for a specific
	 * business
	 * 
	 * @param businessID
	 * @return reviewList
	 */
	public List<org.bson.Document> getTipList(String businessID) {
		List<org.bson.Document> tipList = this.con.getTipCollection()
				.find(new org.bson.Document("business_id", businessID))
				.into(new ArrayList<org.bson.Document>());
		return tipList;
	}

	/**
	 * getText concatenates all text from the query list
	 * 
	 * @params List<org.bson.Document>
	 * @return String of all text items
	 */
	public String getText(List<org.bson.Document> list) {
		StringBuilder strBuilder = new StringBuilder();

		// iterate the list of documents extracting the text
		for (org.bson.Document document : list) {
			// only if collection contains the key 'text'
			if (document.containsKey("text")) {
				System.out.println("\t\t" + document.getString("text"));
				strBuilder.append(document.getString("text"));
				strBuilder.append(System.getProperty("line.separator"));
			}
		}

		return strBuilder.toString();
	}

	public List<String> getUnprocessedRevs() {
		return unprocessedRevs;
	}

	public void setUnprocessedRevs(List<String> unprocessedRevs) {
		this.unprocessedRevs = unprocessedRevs;
	}

	public static void main(String[] args) throws IOException, ParseException {
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("REVIEW", analyzer);
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths
				.get(PATH)));
		IndexSearcher searcher = new IndexSearcher(reader);

		Query query = parser.parse(QueryParser
				.escape("I am looking for a good place to relax"));
		// get top 1000 results

		TopDocs results = searcher.search(query, 10);
		ScoreDoc[] hits = results.scoreDocs;
		for (int i = 0; i < hits.length; i++) {

			Document document = searcher.doc(hits[i].doc);
			System.out.println(document.get("DOCNO") + ": " + hits[i].score);
		}
	}
}
