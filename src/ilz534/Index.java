package ilz534;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Index.java
 * 
 * provides the initial set up to build the index
 * we will use this in combination with mongo to iterate through
 * the entries and index the necessary information.
 */

public class Index {

	private static final String PATH = "/nfs/nfs4/home/arivero/mongodb/index";
	final private Directory dir;
	final private Analyzer analyzer;
	final private IndexWriterConfig iwc;
	final private IndexWriter writer;
	private Connector con;

	public Index() throws IOException {
		this.dir = FSDirectory.open(Paths.get(PATH));
		this.analyzer = new StandardAnalyzer();
		this.iwc = new IndexWriterConfig(this.analyzer);
		this.writer = new IndexWriter(this.dir, this.iwc);
		this.con = new Connector();
	}

	/**
	 * getReviewList 
	 * passes the business id to find the reviews for a specific
	 * business
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
	 * getTipList 
	 * passes the business id to find the reviews for a specific
	 * business
	 * @param businessID
	 * @return tipList
	 */
	public List<org.bson.Document> getTipList(String businessID) {
		List<org.bson.Document> tipList = this.con.getTipCollection()
				.find(new org.bson.Document("business_id", businessID))
				.into(new ArrayList<org.bson.Document>());
		return tipList;
	}

	/**
	 * indexDocs 
	 * takes the 70% of businesses from the database to index. each
	 * business is a document and each review and tip related to the document
	 * id. This index is related to task 1 of our final project, where we try to
	 * label business using review and tip.
	 */
	public void indexDocs() throws IOException {
		List<org.bson.Document> businessList = this.con
				.getBusinessTrainingSet();
		System.out.println("Indexing documents in " + PATH);

		// iterate through the list of business
		for (org.bson.Document businessDoc : businessList) {
			System.out.println("Processing business ID -> "
					+ businessDoc.getString("business_id"));
			Document document = new Document();

			String businessID = businessDoc.getString("business_id");
			document.add(new StringField("DOCNO", businessID, Field.Store.YES));

			// query review collection based on the businessID
			List<org.bson.Document> reviewList = getReviewList(businessID);
			// pass the list to extract only text information
			String reviewText = getText(reviewList);
			document.add(new TextField("REVIEW", reviewText, Field.Store.YES));

			// query tip collection based on businessID
			List<org.bson.Document> tipList = getTipList(businessID);
			// pass the list to extract only text information
			String tipText = getText(tipList);
			document.add(new TextField("TIP", tipText, Field.Store.YES));

			// write to index
			this.writer.addDocument(document);
		}

		writerCleanup();
	}
	

	/**
	 * getText 
	 * concatenates all text from the query list
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

	/**
	 * writerCleanup 
	 * merges, commits and closes writer
	 */
	public void writerCleanup() throws IOException {
		this.writer.commit();
		this.writer.close();
	}
}
