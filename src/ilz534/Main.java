package ilz534;

import java.io.IOException;

import org.apache.lucene.queryparser.classic.ParseException;

public class Main {

	public static void main(String[] args) throws IOException, ParseException {
		String top100hits = "/top100hits.txt";
		String top10hits = "/top10hits.txt";
		String path = System.getProperty("user.home");

		Index ind = new Index();
		Search search = new Search();
		//index for task 1
		//ind.indexDocs();
		//query the 70% business that conform the index
		//search.search();
		search.rankDoccuments(100, path+top100hits);
	}

}
