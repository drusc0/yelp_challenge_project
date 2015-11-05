package ilz534;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/*
 * Index
 * 
 * provides the initial set up to build the index
 * we will use this in combination with mongo to iterate through
 * the entries and index the necessary information.
 */

public class Index {
	
	private static final String PATH = "/Volumes/SEAGATE1TB/index/index";
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
	
	/*
	 * write
	 * adds lucene document
	 */
	public void write(Document doc) throws IOException {
		this.writer.addDocument(doc);
	}
	
	/*
	 * indexReviewDocs
	 * itereates thru reviews
	 */
	public void indexReviewDocs() {

	}
	
	/*
	 * indexTipDocs
	 * iterates thru tips
	 */
	public void indexTipDocs() {
		
	}
}
