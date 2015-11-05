package ilz534;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


/*
 * Conncetor
 * 
 * Provides access to the localhost mongodb collection
 * So far we are using only the collection review and tip
 * Depending on assignment 2, this might be extended to use more
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
	
	/*
	 * getReviewCollection
	 * returns the full review collection
	 */
	public MongoCollection<Document> getReviewCollection() {
		return this.review;
	}
	
	/*
	 * getBusinessCollection
	 * returns the full business collection
	 */
	public MongoCollection<Document> getBusinessCollection() {
		return this.business;
	}
	
	/*
	 * getBusinessList
	 * returns the full review list collection as an array list
	 */
	public List<Document> getBusinessList() {
		List<Document> businessList = this.business.find().into(new ArrayList<Document>());
		return businessList;
	}
	
	/*
	 * getTipCollection
	 * returns the full tip collection
	 */
	public MongoCollection<Document> getTipCollection() {
		return this.tip;
	}
	
}
