# yelp_challenge_project
Information Retrieval final project where we index review and tips and use this information to label.

#task 1

Predict label of businesses
	- Every business contains a category key with an array of items that describe the nature of the business.
	- For this activity we will use the collections "Review" and "Tip".
	- The goal is to use the information given by users in each review and tip to attempt to determine the nature of the business. In a way, using datamining algorithms to label information.
	
IR Approach:
	-> Categories will be considered queries
	-> Build lucene index where the business is a document and review and tips are separate fields
	-> Search in the review or tip for a business
	
Machine Learning Approach:
	-> Separate dataset into training and testing sets
	-> Allow algorithms to take care of labeling
	
#task 2

Haven't been discussed with the group yet