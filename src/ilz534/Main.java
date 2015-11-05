package ilz534;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {

		Connector con = new Connector();
		Index ind = new Index();
		
		ind.indexDocs();
	}

}
