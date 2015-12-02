package ilz534;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Analyze {
    
    private BufferedReader br;
    private int truePositive;
    private int falsePositive;
    private int trueNegative;
    private int falseNegative;
    private Connector con;
    
    public Analyze(String doc) {
        this.truePositive = 0;
        this.trueNegative = 0;
        this.falseNegative = 0;
        this.falsePositive = 0;
        this.con = new Connector();
        
        try {
        	this.br = new BufferedReader(new FileReader(doc));
        } catch(Exception e) {
        	e.printStackTrace();
        }
    }
    
    public List<org.bson.Document> getTestSet() {
        List<org.bson.Document> testList = this.con.getBusinessTestingSet();
        return testList;
    }
    
    public List<String> generateCategories(String docID) {
        List<String> categoriesList = new ArrayList<String>();
        categoriesList = this.con.getCategory(docID);
        return categoriesList;
    }
    
    public void analyze() {
    	List<org.bson.Document> testingList = getTestSet();
    }
    
    
    public static void main(String[] args) throws IOException {
    	Connector con = new Connector();
    	String top10ReviewHits = "/top10reviewhitsMult.txt";
		String path = System.getProperty("user.home");
		FileInputStream is = new FileInputStream(path + top10ReviewHits);
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		
		try{
			int count = 0;
			String line = "";
			while( (line=br.readLine()) != null) {
				if(line.contains("<ID>")) {
					count++;
				}
			}
			
			System.out.println("There are " + con.getBusinessTestingSet().size() + " entries for testing\n");
			System.out.println("There are " + count + " entries in the file\n");
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			br.close();
			isr.close();
			is.close();
		}
    }
}
