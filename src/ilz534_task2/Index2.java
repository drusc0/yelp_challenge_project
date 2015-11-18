package ilz534_task2;

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

public class Index2 {
	private static final String PATH = "/Volumes/SEAGATE1TB/Yelp/index2";
	final private Directory dir;
	final private Analyzer analyzer;
	final private IndexWriterConfig iwc;
	final private IndexWriter writer;
	private Connector2 con;

	public Index2() throws IOException {
		this.dir = FSDirectory.open(Paths.get(PATH));
		this.analyzer = new StandardAnalyzer();
		this.iwc = new IndexWriterConfig(this.analyzer);
		this.writer = new IndexWriter(this.dir, this.iwc);
		this.con = new Connector2();
	}


	/**
	 * getReviewListUsefulVotes
	 * passes the business id to find the reviews for a specific
	 * business
	 * @param businessID
	 * @return reviewList
	 */
	public List<org.bson.Document> getReviewListUsefulVotes(String businessID) {
		List<org.bson.Document> reviewList = this.con.getReviewCollection()
				.find(new org.bson.Document("business_id", businessID)
				.append("votes.useful", new org.bson.Document("$gte", 1)))
				.into(new ArrayList<org.bson.Document>());
		return reviewList;
	}


	/**
	 * indexDocs 
	 * takes the 70% of businesses from the database to index. 
	 * we first select the reviews related to the business ids with
	 * at least 1 useful vote.
	 */
	public void indexDocs() throws IOException {
		List<org.bson.Document> businessList = this.con.getBusinessTrainingSet();
		
		System.out.println("Indexing documents in " + PATH);

		// iterate through the list of business
		for (org.bson.Document businessDoc : businessList) {
			System.out.println("Processing business ID -> "
					+ businessDoc.getString("_id"));
			Document document = new Document();

			String businessID = businessDoc.getString("_id");
			document.add(new StringField("DOCNO", businessID, Field.Store.YES));

			// query review collection based on the businessID
			List<org.bson.Document> reviewList = getReviewListUsefulVotes(businessID);
			// pass the list to extract only text information
			String reviewText = getText(reviewList);
			document.add(new TextField("REVIEW", reviewText, Field.Store.YES));

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
