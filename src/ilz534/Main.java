package ilz534;

import java.io.IOException;

import org.apache.lucene.queryparser.classic.ParseException;

public class Main {

	public static void main(String[] args) throws Exception {
		String top10ReviewHits = "/top10reviewhitsMult.txt";
		String top10Review = "/top10reviewhits.txt";
		String path = System.getProperty("user.home");

		// Index ind = new Index();
		// index for task 1
		// ind.indexDocs();
		// query the 70% business that conform the index
		Search search = new Search();
		search.rankDocuments("REVIEW", 10, path+top10ReviewHits);
		search.rankDocumentsShortQuery("REVIEW", 10, path+top10Review);
		
		Analyze analyze = new Analyze(path + top10Review);
		analyze.analyze();
		System.out.println("Files that matched (true positives): "
				+ analyze.getTruePositive());
		System.out.println("Files that DID NOT match (true negatives): "
				+ analyze.getTrueNegative());

		analyze.setReader(path + top10ReviewHits);
		analyze.analyze();
		System.out.println("Files that matched (true positives): "
				+ analyze.getTruePositive());
		System.out.println("Files that DID NOT match (true negatives): "
				+ analyze.getTrueNegative());
	}

}
