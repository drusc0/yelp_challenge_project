package ilz534;

import java.io.IOException;

import org.apache.lucene.queryparser.classic.ParseException;

public class Main {

	public static void main(String[] args) throws IOException, ParseException {
		String top10ReviewHits = "/top10reviewhitsMult.txt";
		String top10TipHits = "/top10tiphits.txt";
		String path = System.getProperty("user.home");

		//Index ind = new Index();
		Search search = new Search();
		//index for task 1
		//ind.indexDocs();
		//query the 70% business that conform the index
		//search.search();
		search.rankDocuments("REVIEW", 10, path+top10ReviewHits);
		//search.rankDoccuments("TIP", 10, path+top10TipHits);
	}

}
