package ilz534;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Connector
 * 
 * Provides access to the localhost mongodb collection So far we are using only
 * the collection review and tip Depending on assignment 2, this might be
 * extended to use more
 * 
 */

public class Connector {

	final private MongoClient client;
	final private MongoDatabase db;
	private MongoCollection<Document> review;
	private MongoCollection<Document> tip;
	private MongoCollection<Document> business;

	public Connector() {
		this.client = new MongoClient(new MongoClientURI("mongodb://localhost"));
		this.db = client.getDatabase("yelp");
		this.review = this.db.getCollection("review");
		this.tip = this.db.getCollection("tip");
		this.business = this.db.getCollection("business");
	}

	public void close() {
		this.client.close();
	}

	/**
	 * getReviewCollection
	 * 
	 * @returns the full review collection
	 */
	public MongoCollection<Document> getReviewCollection() {
		// System.out.println("Retrieving Review Collection....");
		return this.review;
	}

	/**
	 * getBusinessCollection
	 * 
	 * @returns the full business collection
	 */
	public MongoCollection<Document> getBusinessCollection() {
		// System.out.println("Retrieving Business Collection....");
		return this.business;
	}

	/**
	 * getBusinessList
	 * 
	 * @returns the full review list collection as an array list
	 */
	public List<Document> getBusinessList() {
		List<Document> businessList = this.business.find().into(
				new ArrayList<Document>());
		return businessList;
	}

	/**
	 * getBusinessTrainingSet divides the list into 70% for training
	 * 
	 * @return training list
	 */
	public List<Document> getBusinessTrainingSet() {
		long count = this.business.count();
		int limit = (int) (.7 * count);

		List<Document> trainingList = this.business.find().limit(limit)
				.into(new ArrayList<Document>());

		return trainingList;
	}

	/**
	 * getBusinessTestingSet divides the list into 30% for testing (last 30%)
	 * 
	 * @return testing list
	 */
	public List<Document> getBusinessTestingSet() {
		long count = this.business.count();
		int skip = (int) (.7 * count);

		List<Document> testingList = this.business.find().skip(skip)
				.limit(1000)
				.into(new ArrayList<Document>());

		return testingList;
	}

	/**
	 * exists checks whethere a field exists for given key
	 * 
	 * @param field
	 * @param key
	 * @return
	 */
	public boolean exists(String field, String key) {
		boolean flag = false;
		Document doc = new Document("business_id", key).append(field,
				new Document("$exists", true));
		if (this.business.count(doc) > 0) {
			flag = true;
		}
		return flag;
	}

	/**
	 * getCategory generates a query for the business collection to retrieve
	 * categories for the business Id provided
	 * 
	 * @param businessID
	 * @return categoryList
	 */
	public ArrayList<String> getCategory(String businessID) {
		Document filter = new Document("business_id", businessID);
		Document categories = this.business.find(filter).first();

		if (exists("categories", businessID)) {
			@SuppressWarnings("unchecked")
			List<String> list = (ArrayList<String>) categories
					.get("categories");
			return (ArrayList<String>) list;
		}

		return new ArrayList<String>();
		// return categories;
	}

	/**
	 * getTipCollection
	 * 
	 * @returns the full tip collection
	 */
	public MongoCollection<Document> getTipCollection() {
		// System.out.println("Retrieving Tip Collection....");
		return this.tip;
	}

	public static void main(String[] args) {
		Connector con = new Connector();
		ArrayList<String> cat = con.getCategory("VZLTYr_v1vLSFKBw1aqhaA");
		for (int i = 0; i < cat.size(); i++) {
			System.out.println(cat.get(i));
		}
	}
}
