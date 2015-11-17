package connector;

import ilz534.Connector;
import ilz534.Index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class IndexTest extends TestCase {

	public Index ind;
	public Connector con;

	public void setUp() throws IOException {
		ind = new Index();
		con = new Connector();
	}


	public void testReviewWithBusinessID() {
		org.bson.Document doc = con.getBusinessCollection().find().first();

		long count = ind.getReviewList(doc.getString("business_id")).size();
		List<org.bson.Document> lst = con
				.getReviewCollection()
				.find(new org.bson.Document("business_id", doc
						.getString("business_id")))
				.into(new ArrayList<org.bson.Document>());
		
		assertEquals(count, lst.size());
	}

	public void testTipWithBusinessID() {
		org.bson.Document doc = con.getBusinessCollection().find().first();
		long count = ind.getTipList(doc.getString("business_id")).size();
		List<org.bson.Document> lst = con
				.getTipCollection()
				.find(new org.bson.Document("business_id", doc
						.getString("business_id")))
				.into(new ArrayList<org.bson.Document>());
		
		assertEquals(count, lst.size());
	}
}
