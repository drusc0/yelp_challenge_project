package ilz534;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {

		Index ind = new Index();
		Search search = new Search();
		//index for task 1
		ind.indexDocs();
		//query the 70% business that conform the index
		//search.search();
	}

}
