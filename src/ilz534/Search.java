package ilz534;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

/**
 * Search.java
 * 
 * Performs a search on the index built for the purpose to find similar 
 * labels or categories for the remaining 30% businesses. We use this subset
 * to see how accurate reviews and/or texts can be in labeling a business
 * as some yelp category
 * 
 * @author drusc0
 */
public class Search {
	
	private static final String PATH = "/Volumes/SEAGATE1TB/Yelp/index";
	private Connector con;
	private IndexReader reader;
	private IndexSearcher searcher;
	private Analyzer analyzer;
	
	/**
	 * establishes connection with mongo and initialized reader, searcher, and analyer
	 * @throws IOException
	 */
	public Search() throws IOException {
		this.con = new Connector();
		this.reader = DirectoryReader.open(FSDirectory.open(Paths.get(PATH)));
		this.searcher = new IndexSearcher(this.reader);
		this.analyzer = new StandardAnalyzer();
		//set searcher to best match similarity model
		this.searcher.setSimilarity(new BM25Similarity());
	}
	
	/**
	 * getTestingSet()
	 * we retrieve the remaining 30% of businesses to test accuracy of reviews
	 * for labeling categories for incoming businesses.
	 * @return testingSet
	 */
	public List<org.bson.Document> getTestingSet() {
		List<org.bson.Document> testingSet = this.con.getBusinessTestingSet();
		return testingSet;
	}
	
	public Set<Term> parseQuery(String queryString, String field) 
			throws IOException, ParseException {
		
		Set<Term> terms = new LinkedHashSet<Term>();
		QueryParser parser = new QueryParser(field, this.analyzer);
		Query query = parser.parse(queryString);
		this.searcher.createNormalizedWeight(query, false).extractTerms(terms);
		return terms;
	}

}
