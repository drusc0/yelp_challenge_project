package ilz534;

public class Main {

	public static void main(String[] args) throws Exception {
		String path = System.getProperty("user.home");

		Index ind = new Index();
		//index for task 1
		ind.indexDocs();


		 Search search = new Search();
		 
		 String top10ReviewHits = "/top10reviewhitsMult.txt";
		 search.rankDocuments("REVIEW", 10, path+top10ReviewHits, 0);
		 
		 String top10ReviewLongQueryNoSW = "/top10reviewhitslongnosw.txt";
		 search.rankDocuments("REVIEW", 10, path+top10ReviewLongQueryNoSW, 5);
		 
		 String top10ReviewShort5Words = "/top10reviewhits5random.txt";
		 search.rankDocumentsShortQuery("REVIEW", 10,
		 path+top10ReviewShort5Words, 5);
		 
		 String top10ReviewShort7Words = "/top10reviewhits7random.txt";
		 search.rankDocumentsShortQuery("REVIEW", 10,
		 path+top10ReviewShort7Words, 7);
		 
		 String top10ReviewHybridNoSW = "/top10reviewhitshybridnosw.txt";
		 search.rankDocuments("REVIEW", 10, path+top10ReviewHybridNoSW, 5);
		 
		 String top10ReviewHybrid = "/top10reviewhitshybrid.txt";
		 search.rankDocuments("REVIEW", 10, path+top10ReviewHybrid, 5);
		 
		 CategoryAnalyzer analyze = new CategoryAnalyzer(path + top10ReviewHits);
		 analyze.analyze(); System.out.println(
		 "\"Long Query\" Categories that matched (true positives): " +
		 analyze.getTruePositive());
		 System.out.println("Categories that DID NOT match (false positives): "
				 + analyze.getFalsePositive());
		 
		 analyze.setReader(path + top10ReviewLongQueryNoSW);
		 analyze.analyze(); System.out.println(
				 "\"Long Query w/ customized stop word removal\" Categories that matched (true positives): "
						 + analyze.getTruePositive());
		 System.out.println("Categories that DID NOT match (false positives): "
				 + analyze.getFalsePositive());
		 
		 analyze.setReader(path + top10ReviewShort5Words); analyze.analyze();
		 System.out.println(
				 "\"5 Random Words Query\" Categories that matched (true positives): "
						 + analyze.getTruePositive());
		 System.out.println("Categories that DID NOT match (false positives): "
				 + analyze.getFalsePositive());
		 
		 analyze.setReader(path + top10ReviewShort7Words); analyze.analyze();
		 System.out.println(
				 "\"7 Random Words Query\"Categories that matched (true positives): "
						 + analyze.getTruePositive());
		 System.out.println("Categories that DID NOT match (false positives): "
				 + analyze.getFalsePositive());
		 
		 analyze.setReader(path + top10ReviewHybridNoSW); analyze.analyze();
		 System.out.println(
				 "\"Long query & 5 Words for really long queries (no stopw words)\" Categories that matched (true positives): "
						 + analyze.getTruePositive());
		 System.out.println("Categories that DID NOT match (false positives): "
				 + analyze.getFalsePositive());
		 
		 analyze.setReader(path + top10ReviewHybrid); analyze.analyze();
		 System.out.println(
				 "\"Long query & 5 Words for really long queries\" Categories that matched (true positives): "
						 + analyze.getTruePositive());
		 System.out.println("Categories that DID NOT match (false positives): "
				 + analyze.getFalsePositive());


		String evaluationDoc = "/evalDoc.txt";
		String evaluationShortDoc = "/evalShortDoc.txt";
		IREvaluation eval = new IREvaluation();
		eval.evaluate(path+evaluationDoc);
		eval.evaluateShort(path+evaluationShortDoc);
		System.out.println(eval.getPrecision(path+evaluationDoc) + " out of "
				+ eval.getTotalTest());
		System.out.println(eval.getPrecision(path+evaluationShortDoc) +
				" out of " + eval.getTotalTest());
		System.out.println(eval.getRecall(path+evaluationDoc));
		System.out.println(eval.getRecall(path + evaluationShortDoc));
	}

}
