package connector;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import ilz534.Connector;
import junit.framework.TestCase;

public class ConnectorTest extends TestCase {

	public Connector con;
	
	public void setUp() {
		con = new Connector();
	}
	
	public void testObjectNotNull() {
		assertNotNull(con);
	}
	
	public void testReviewCollection() {
		MongoCollection<Document> rev = con.getReviewCollection();
		assertNotNull(rev);
		assertEquals(rev, con.getReviewCollection());
	}
	
	public void testTipCollection() {
		MongoCollection<Document> tip = con.getTipCollection();
		assertNotNull(tip);
		assertEquals(tip, con.getTipCollection());
	}
}
