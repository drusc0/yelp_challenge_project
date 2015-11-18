package ilz534_task2;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Connector2.java
 * 
 * provides an interface with mongodb
 * This varies from the connector class in that we use this to
 * accomplish task 2 of our assignment.
 * Task2: we will use 70% of all the businesses with reviews that
 * have useful votes of more than 0.
 * @author drusc0
 */

public class Connector2 {
	final private MongoClient client;
	final private MongoDatabase db;
	private MongoCollection<Document> review;
	private MongoCollection<Document> tip;
	private MongoCollection<Document> business;

	public Connector2() {
		this.client = new MongoClient(new MongoClientURI("mongodb://localhost"));
		this.db = client.getDatabase("yelp");
		this.review = this.db.getCollection("review");
		this.tip = this.db.getCollection("tip");
		this.business = this.db.getCollection("business");
	}

	/**
	 * getReviewCollection 
	 * @returns the full review collection
	 */
	public MongoCollection<Document> getReviewCollection() {
		System.out.println("Retrieving Review Collection....");
		return this.review;
	}
	
	/**
	 * getTipCollection 
	 * @returns the full tip collection
	 */
	public MongoCollection<Document> getTipCollection() {
		System.out.println("Retrieving Tip Collection....");
		return this.tip;
	}

	/**
	 * getBusinessCollection 
	 * @returns the full business collection
	 */
	public MongoCollection<Document> getBusinessCollection() {
		System.out.println("Retrieving Business Collection....");
		return this.business;
	}

	/**
	 * getBusinessList 
	 * @returns the full review list collection as an array list
	 */
	public List<Document> getBusinessList() {
		List<Document> businessList = this.business.find()
				.into(new ArrayList<Document>());
		return businessList;
	}


	/**
	 * getBusinessTrainingSet
	 * divides the list into 70% for training This is
	 * for task 2, and we need only businesses with reviews that have useful
	 * votes.
	 * @return training list
	 */
	public List<Document> getBusinessTrainingSet() {
		
		List<Document> pipeline;
		pipeline = asList(	new Document("$group",
										new Document("_id", "$business_id")
										.append("useful_votes", 
												new Document("$sum","$votes.useful"))),
							new Document("$match",
										new Document("useful_votes",
												new Document("$gte", 1))));
		
		List<Document> trainingList = this.review.aggregate(pipeline)
				.into(new ArrayList<Document>());
		
		long count = (long) (trainingList.size() * .7);
		
		for(long i = count+1; i < trainingList.size(); i++) {
			trainingList.remove(count);
		}

		return trainingList;
	}

	/**
	 * getBusinessTestingSet
	 * divides the list into 30% for testing (last 30%)
	 * The remaining businesses with useful votes will be used for testing
	 * @return testing list
	 */
	public List<Document> getBusinessTestingSet() {

		List<Document> pipeline;
		pipeline = asList(	new Document("$group",
										new Document("_id", "$business_id")
										.append("useful_votes", 
												new Document("$sum","$votes.useful"))),
							new Document("$match",
										new Document("useful_votes",
												new Document("$gte", 1))));

		List<Document> testingList = this.review.aggregate(pipeline)
				.into(new ArrayList<Document>());
		
		long count = (long) (testingList.size() * .7);
		
		for(long i = 0; i <= count; i++) {
			testingList.remove(count);
		}

		return testingList;
	}

}
