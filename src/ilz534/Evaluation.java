package ilz534;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

public class Evaluation {

	private static final String PATH = "/Volumes/SEAGATE1TB 1/Yelp/index";
	private Connector con;
	private IndexReader reader;
	private IndexSearcher searcher;
	private Analyzer analyzer;
	private List<String> unprocessedRevs;
	private float recall;
	private long precision;
	private long totalTest;
	private CharArraySet stopWords;
	private Analyzer stopAnalyzer;

	/**
	 * establishes connection with mongodb and initialized reader, searcher, and
	 * analyzer
	 * 
	 * @throws IOException
	 */
	public Evaluation() throws IOException {
		this.con = new Connector();
		this.reader = DirectoryReader.open(FSDirectory.open(Paths.get(PATH)));
		this.searcher = new IndexSearcher(this.reader);
		this.analyzer = new StandardAnalyzer();
		// set searcher to best match similarity model
		this.searcher.setSimilarity(new BM25Similarity());
		this.recall = 0;
		this.precision = 0;
		this.totalTest = 0;
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

	public void evaluate(String path) throws IOException, ParseException {

		QueryParser parser = new QueryParser("REVIEW", this.analyzer);

		List<org.bson.Document> testingSet = this.getTestingSet();
		// extract the remaining dataset (testing)
		for (org.bson.Document doc : testingSet) {

			String docID = doc.getString("business_id");

			// get review list based on doc ids
			List<org.bson.Document> list = this.getReviewList(docID);

			String txt = this.getText(list);

			// parse queries
			try {
				Query query = parser.parse(QueryParser.escape(txt));
				// get top 1000 result
				TopDocs results = this.searcher.search(query, 10);
				ScoreDoc[] hits = results.scoreDocs;
				// List<Entry<String, Double>> hitsProcessed =
				// processHits(hits);
				try {
					writeToFile(docID, hits, path);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				this.unprocessedRevs.add(docID);
				// rankDocumentsShortQuery("REVIEW", 10, path, n);
			}

		}
	}

	public void evaluateShort(String path) throws IOException, ParseException {

		QueryParser parser = new QueryParser("REVIEW", this.analyzer);

		List<org.bson.Document> testingSet = this.getTestingSet();
		// extract the remaining dataset (testing)
		for (org.bson.Document doc : testingSet) {

			String docID = doc.getString("business_id");

			// get review list based on doc ids
			List<org.bson.Document> list = this.getReviewList(docID);

			String txt = this.getText(list);

			// parse queries
			try {
				List<String> vector = removeStopWords(txt);
				System.out.println(vector);
				String str = selectRandomWords(vector, 5);
				Query query = parser.parse(str);
				// get top 1000 result
				TopDocs results = this.searcher.search(query, 10);
				ScoreDoc[] hits = results.scoreDocs;
				// List<Entry<String, Double>> hitsProcessed =
				// processHits(hits);
				try {
					writeToFile(docID, hits, path);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				this.unprocessedRevs.add(docID);
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
	public void writeToFile(String docID, ScoreDoc[] hits, String path)
			throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(path, true));
		bw.write("<ID>" + docID + "</ID>\n");
		bw.write("<Documents>\n");
		System.out.println(docID);
		for (int i = 0; i < hits.length; i++) {
			Document document = this.searcher.doc(hits[i].doc);
			System.out.println("\t" + document.get("DOCNO"));
			bw.write(document.get("DOCNO") + "\n");
		}
		bw.write("</Documents>\n");
		bw.close();
	}

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

	public void initStopWords() throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("words.txt").getFile());
		//String file = "/Users/drusc0/Documents/IUB/ILS-Z 534/words.txt";
		BufferedReader br = new BufferedReader(new FileReader(file));
		List<String> words = new ArrayList<String>();
		String line = "";
		while ((line = br.readLine()) != null) {
			words.add(line);
		}
		br.close();

		System.out.println(words);
		this.stopWords = StopFilter.makeStopSet(words, true);
		this.stopAnalyzer = new StopAnalyzer(this.stopWords);
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

	public float getRecall(String path) throws IOException {
		this.recall = 0;
		String line = "";
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		BufferedReader br = new BufferedReader(new FileReader(path));
		try {
			int breaker = 0;
			String id = "";
			while ((line = br.readLine()) != null && breaker < 500) {

				if (line.contains("<ID>")) {
					// remove tags
					line = line.replaceFirst("<ID>", "");
					line = line.replaceFirst("</ID>", "");
					id = line;
				} else if (line.contains("<Documents>")) {
					br.readLine();
					List<String> lst = new ArrayList<String>();
					for (int i = 0; i < 10; i++) {
						line = br.readLine();
						lst.add(line);
					}
					map.put(id, lst);
				}
				breaker++;
			}

			checkRecall(map);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			br.close();
		}

		return recall;
	}

	public void checkRecall(Map<String, List<String>> map) {
		for (String key : map.keySet()) {
			getActualCategoryDocumentNumber(key, map.get(key));
		}
	}

	public void getActualCategoryDocumentNumber(String id,
			List<String> listOfBestMatch) {
		List<String> catsList = generateCategories(id);
		long count = getCategoriesDocs(catsList);
		long ind = 0;
		boolean flag = true;

		for (String best : listOfBestMatch) {
			List<String> trainingCategories = this.generateCategories(best);
			flag = false;

			for (String category : catsList) {

				for (String traincat : trainingCategories) {
					if (category.equals(traincat)) {
						ind++;
						flag = true;
						break;
					}
				}
				if (flag)
					break;
			}
		}
		float newRecall = ind / ((float) (ind + count));
		this.recall = (this.recall + newRecall) / 2;
	}

	public long getCategoriesDocs(List<String> cats) {
		org.bson.Document d1 = new org.bson.Document("$in", cats);
		org.bson.Document d2 = new org.bson.Document("categories", d1);
		List<org.bson.Document> list = this.con.getBusinessCollection()
				.find(d2).into(new ArrayList<org.bson.Document>());

		return list.size();
	}

	public long getTotalTest() {
		return totalTest * 10;
	}

	public long getPrecision(String path) throws IOException {
		this.precision = 0;
		this.totalTest = 0;
		String line = "";
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		BufferedReader br = new BufferedReader(new FileReader(path));
		try {
			String id = "";
			while ((line = br.readLine()) != null) {

				if (line.contains("<ID>")) {
					this.totalTest++;
					// remove tags
					line = line.replaceFirst("<ID>", "");
					line = line.replaceFirst("</ID>", "");
					id = line;
				} else if (line.contains("<Documents>")) {
					br.readLine();
					List<String> lst = new ArrayList<String>();
					for (int i = 0; i < 10; i++) {
						line = br.readLine();
						lst.add(line);
					}
					map.put(id, lst);
				}
			}

			checkRecall(map);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			br.close();
		}

		return this.precision;
	}

	public void checkValidity(Map<String, List<String>> map) {

		for (String key : map.keySet()) {

			for (String topDoc : map.get(key)) {
				checkCategories(key, topDoc);
			}
		}
	}

	public void checkCategories(String businessID, String topDoc) {
		// categories for doc in test
		List<String> categories = this.generateCategories(businessID);
		List<String> trainingCategories = this.generateCategories(topDoc);

		for (String category : categories) {
			for (String traincat : trainingCategories) {
				if (category.equals(traincat)) {
					this.precision++;
					return;
				}
			}
		}
	}

	public List<String> generateCategories(String docID) {
		List<String> categoriesList = this.con.getCategory(docID);
		return categoriesList;
	}

}
