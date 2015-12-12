package ilz534;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
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
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

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

	private static final String PATH = "/nfs/nfs4/home/arivero/mongodb/index";
	private Connector con;
	private IndexReader reader;
	private IndexSearcher searcher;
	private Analyzer analyzer;
	private List<String> unprocessedRevs;
	private CharArraySet stopWords;
	private Analyzer stopAnalyzer;

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
		initStopWords();
	}

	public void close() throws IOException {
		this.con.close();
		this.reader.close();
		this.analyzer.close();
		this.stopAnalyzer.close();
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
	 * @throws Exception
	 */
	public String parseQuery(String queryString, String field) throws Exception {

		Set<Term> terms = new LinkedHashSet<Term>();
		List<String> vec = removeStopWords(queryString);
		String newquery = puttogether(vec);

		QueryParser parser = new QueryParser(field, this.analyzer);
		Query query = parser.parse(newquery);
		this.searcher.createNormalizedWeight(query, false).extractTerms(terms);

		Map<String, Double> m = rankTerms(terms);
		String q = selectBestTerms(m);

		return q;
	}

	public String selectBestTerms(Map<String, Double> m) {
		StringBuilder query = new StringBuilder();
		
		
		List<Entry<String, Double>> greatest = findGreatest(m, 5);
        System.out.println("Top "+5+" entries:");
        for (Entry<java.lang.String, java.lang.Double> entry : greatest)
        {
            System.out.println(entry);
            query.append(entry.getKey());
            query.append(" ");
        }
		


		return query.toString();
	}

	public Map<String, Double> rankTerms(Set<Term> terms) throws IOException {
		int N = this.reader.maxDoc();
		List<LeafReaderContext> leafContexts = this.reader.getContext()
				.reader().leaves();
		DefaultSimilarity dSimi = new DefaultSimilarity();
		Map<String, Double> map = new HashMap<String, Double>();

		for (Term t : terms) {
			double termScore = 0.0;

			for (int i = 0; i < leafContexts.size(); i++) {
				LeafReaderContext leafContext = leafContexts.get(i);
				PostingsEnum de = MultiFields.getTermDocsEnum(
						leafContext.reader(), "REVIEW", new BytesRef(t.text()));
				int doc;
				if (de != null) {
					while ((doc = de.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
						// de.advance(docID);
						if (de.freq() > 0) {

							int df = this.reader.docFreq(new Term("REVIEW", t
									.text()));
							double IDF = Math.log(1 + (N / df));
							double normDocLength = dSimi
									.decodeNormValue(leafContext.reader()
											.getNormValues("REVIEW").get(doc));
							int freq = de.freq();
							double length = 1 / (normDocLength * normDocLength);
							double TF = freq / (double) length;
							double res = TF * IDF;
							termScore += res;
						}
					}
				}
			}
			map.put(t.text(), termScore);

		}

		return map;
	}

	
	
	public void rankDocumentWithTFIDFTerm(String field, int numberOfHits, String path)
			throws IOException, ParseException {

		QueryParser parser = new QueryParser(field, this.analyzer);

		List<org.bson.Document> testingSet = this.getTestingSet();
		// extract the remaining dataset (testing)
		for (org.bson.Document doc : testingSet) {

			String docID = doc.getString("business_id");
			List<org.bson.Document> list = new ArrayList<org.bson.Document>();

			// get review list based on doc ids
			if (field.equals("REVIEW")) {
				list = this.getReviewList(docID);
			} else if (field.equals("TIP")) {
				list = this.getTipList(docID);
			}
			String txt = this.getText(list);

			// parse queries
			try {
				//Query query = parser.parse(QueryParser.escape(txt));
				String test1 = parseQuery(txt, "REVIEW");
				Query query = parser.parse(test1);
				// get top 1000 result
				TopDocs results = this.searcher.search(query, numberOfHits);
				ScoreDoc[] hits = results.scoreDocs;
				List<Entry<String, Double>> hitsProcessed = processHits(hits);
				try {
					writeToFile(docID, hitsProcessed, path);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {

			}

		}
	}
	
	/**
	 * rankDocuments produces long queries (uses the testing set's review text)
	 * and pulls the best match from the Index
	 * 
	 * @param field
	 * @param numberOfHits
	 * @param path
	 * @param n
	 * @throws IOException
	 * @throws ParseException
	 */
	public void rankDocuments(String field, int numberOfHits, String path, int n)
			throws IOException, ParseException {

		QueryParser parser = new QueryParser(field, this.analyzer);

		List<org.bson.Document> testingSet = this.getTestingSet();
		// extract the remaining dataset (testing)
		for (org.bson.Document doc : testingSet) {

			String docID = doc.getString("business_id");
			List<org.bson.Document> list = new ArrayList<org.bson.Document>();

			// get review list based on doc ids
			if (field.equals("REVIEW")) {
				list = this.getReviewList(docID);
			} else if (field.equals("TIP")) {
				list = this.getTipList(docID);
			}
			String txt = this.getText(list);

			// parse queries
			try {
				//Query query = parser.parse(QueryParser.escape(txt));
				String test1 = parseQuery(txt, "REVIEW");
				Query query = parser.parse(test1);
				// get top 1000 result
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
				if (n > 0) {
					rankDocumentShortQuery(docID, field, numberOfHits, path, n);
				}
			}

		}
	}

	/**
	 * puttogether puts together a string that contains no stopwords
	 * 
	 * @param vector
	 * @return string
	 */
	public String puttogether(List<String> vector) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < vector.size(); i++) {
			str.append(vector.get(i));
			str.append(" ");
		}
		return str.toString();
	}

	/**
	 * rankDocumentShortQuery is used to catch TooManyClausesException in an
	 * attempt to improve the accuracy with a mix approach of long and short
	 * queries
	 * 
	 * @param id
	 * @param field
	 * @param numberOfHits
	 * @param path
	 * @param n
	 * @throws IOException
	 * @throws ParseException
	 */
	public void rankDocumentShortQuery(String id, String field,
			int numberOfHits, String path, int n) throws IOException,
			ParseException {

		QueryParser parser = new QueryParser(field, this.analyzer);
		// extract the remaining dataset (testing)

		List<org.bson.Document> list = this.getReviewList(id);

		String txt = this.getText(list);

		// parse queries
		try {
			List<String> vector = removeStopWords(txt);
			String str = selectRandomWords(vector, n);
			Query query = parser.parse(str);
			// get top 1000 result
			TopDocs results = this.searcher.search(query, numberOfHits);
			ScoreDoc[] hits = results.scoreDocs;
			List<Entry<String, Double>> hitsProcessed = processHits(hits);
			try {
				writeToFile(id, hitsProcessed, path);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			this.unprocessedRevs.add(id);
		}

	}

	/**
	 * rankDocumentsShortQuery forms a query using random words, the user
	 * selects how many words to use. The user gives the field to be examined.
	 * This method uses selectRandomWords to complete
	 * 
	 * @param field
	 * @param numberOfHits
	 * @param path
	 * @param n
	 * @throws IOException
	 * @throws ParseException
	 */
	public void rankDocumentsShortQuery(String field, int numberOfHits,
			String path, int n) throws IOException, ParseException {

		QueryParser parser = new QueryParser(field, this.analyzer);

		List<org.bson.Document> testingSet = this.getTestingSet();
		// extract the remaining dataset (testing)
		for (org.bson.Document doc : testingSet) {

			String docID = doc.getString("business_id");
			List<org.bson.Document> list = new ArrayList<org.bson.Document>();

			// get review list based on doc ids
			if (field.equals("REVIEW")) {
				list = this.getReviewList(docID);
			} else if (field.equals("TIP")) {
				list = this.getTipList(docID);
			}

			String txt = this.getText(list);

			// parse queries
			try {
				List<String> vector = removeStopWords(txt);
				String str = selectRandomWords(vector, n);
				Query query = parser.parse(str);
				// get top 1000 result
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
			}

		}
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
	 * selectRandomWords generates X random numbers and fetches the words at
	 * those locations to form a query
	 * 
	 * @param vector
	 * @param num
	 * @return string of random words
	 */
	public String selectRandomWords(List<String> vector, int num) {
		int[] nums = generateRandomNums(vector, num);
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < nums.length; i++) {
			str.append(vector.get(nums[i]));
			str.append(" ");
		}
		return str.toString();
	}

	/**
	 * removeStopWords delete stop words from query string
	 * 
	 * @param textFile
	 * @return string - string with no stopwords
	 * @throws Exception
	 */
	public List<String> removeStopWords(String text) throws Exception {
		// Analyzer analyzer = new StandardAnalyzer();
		TokenStream ts = stopAnalyzer.tokenStream("content", text);
		CharTermAttribute charTerm = ts.addAttribute(CharTermAttribute.class);
		List<String> vector = new ArrayList<String>();

		try {
			ts.reset(); // Resets this stream to the beginning. (Required)
			while (ts.incrementToken()) {
				vector.add(charTerm.toString());
			}
			ts.end(); // Perform end-of-stream operations, e.g. set the final
						// offset.
		} finally {
			ts.close(); // Release resources associated with this stream.
		}
		return vector;
	}

	/**
	 * generaterandomnums generate X unique random integers
	 * 
	 * @param text
	 * @return array of 4 ints
	 */
	public int[] generateRandomNums(List<String> text, int num) {
		Random random = new Random();
		int[] ints = new int[num];

		for (int i = 0; i < num; i++) {
			ints[i] = random.nextInt(text.size());
			for (int j = i; j > 0; j--) {
				if (ints[i] == ints[j]) {
					ints[i] = random.nextInt(text.size());
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
				// System.out.println("\t\t" + document.getString("text"));
				strBuilder.append(document.getString("text"));
				strBuilder.append(System.getProperty("line.separator"));
			}
		}

		return strBuilder.toString();
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
	 * getUnprocessedRevs stores the documents that the program was unable to
	 * analyze from testing set
	 * 
	 * @return unprocessedReviews
	 */
	public List<String> getUnprocessedRevs() {
		return unprocessedRevs;
	}

	/**
	 * initStopWords initializes the stop word analyzer with a customized list
	 * of stop words extracted from very frequent words in the internet
	 * 
	 * @throws IOException
	 */
	public void initStopWords() throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("words.txt").getFile());
		// String file = "/Users/drusc0/Documents/IUB/ILS-Z 534/words.txt";
		BufferedReader br = new BufferedReader(new FileReader(file));
		List<String> words = new ArrayList<String>();
		String line = "";
		while ((line = br.readLine()) != null) {
			words.add(line);
		}
		br.close();

		this.stopWords = StopFilter.makeStopSet(words, true);
		this.stopAnalyzer = new StopAnalyzer(this.stopWords);
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
			// System.out.println(entry.toString());
			bw.write(entry.toString() + "\n");
		}
		bw.write("</Categories>\n");
		bw.close();
	}

	
	
	
	
	
	private static <String, Double extends Comparable<? super Double>> List<Entry<String, Double>> findGreatest(
			Map<String, Double> map, int n) {
		Comparator<? super Entry<String, Double>> comparator = new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> e0, Entry<String, Double> e1) {
				Double v0 = e0.getValue();
				Double v1 = e1.getValue();
				return v0.compareTo(v1);
			}
		};
		PriorityQueue<Entry<String, Double>> highest = new PriorityQueue<Entry<String, Double>>(n,
				comparator);
		for (Entry<String, Double> entry : map.entrySet()) {
			highest.offer(entry);
			while (highest.size() > n) {
				highest.poll();
			}
		}

		List<Entry<String, Double>> result = new ArrayList<Map.Entry<String, Double>>();
		while (highest.size() > 0) {
			result.add(highest.poll());
		}
		return result;
	}

	
	
	
	
	// MAIN for testing
	public static void main(String[] args) throws Exception {
		String path = System.getProperty("user.home");
		Search s = new Search();
		s.rankDocumentWithTFIDFTerm("REVIEW", 1000, path + "/test1.txt");

		/*
		 * Analyzer analyzer = new StandardAnalyzer(); QueryParser parser = new
		 * QueryParser("REVIEW", analyzer); IndexReader reader =
		 * DirectoryReader.open(FSDirectory.open(Paths .get(PATH)));
		 * IndexSearcher searcher = new IndexSearcher(reader);
		 * 
		 * Query query = parser.parse(QueryParser
		 * .escape("I am looking for a good place to relax")); // get top 1000
		 * results
		 * 
		 * TopDocs results = searcher.search(query, 10); ScoreDoc[] hits =
		 * results.scoreDocs; for (int i = 0; i < hits.length; i++) {
		 * 
		 * Document document = searcher.doc(hits[i].doc);
		 * System.out.println(document.get("DOCNO") + ": " + hits[i].score); }
		 */
	}
}
