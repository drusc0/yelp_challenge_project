package ilz534;

public class Main {

	public static void main(String[] args) throws Exception {
		String top10ReviewHits = "/top10reviewhitsMult.txt";
		String top10Review = "/top10reviewhits.txt";
		String top10ReviewNew = "/top10reviewhitsnew.txt";
		String top10Hybrid = "/top10reviewhybrid.txt";
		String path = System.getProperty("user.home");

		// Index ind = new Index();
		// index for task 1
		// ind.indexDocs();
		// query the 70% business that conform the index
		Search search = new Search();
		search.rankDocuments("REVIEW", 10, path+top10Hybrid, 5);
		//search.rankDocumentsShortQuery("REVIEW", 10, path+top10ReviewNew, 5);
		
		/*Analyze analyze = new Analyze(path + top10ReviewHits);
		analyze.analyze();
		System.out.println("Files that matched (true positives): "
				+ analyze.getTruePositive());
		System.out.println("Files that DID NOT match (true negatives): "
				+ analyze.getTrueNegative());

		analyze.setReader(path + top10ReviewNew);
		analyze.analyze();
		System.out.println("Files that matched (true positives): "
				+ analyze.getTruePositive());
		System.out.println("Files that DID NOT match (true negatives): "
				+ analyze.getTrueNegative());*/
	}

}
