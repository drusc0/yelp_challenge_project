package ilz534;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Analyze {

	private BufferedReader br;
	private int truePositive;
	private int trueNegative;
	private Connector con;

	public Analyze(String doc) {
		this.truePositive = 0;
		this.trueNegative = 0;
		this.con = new Connector();

		try {
			this.br = new BufferedReader(new FileReader(doc));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setReader(String path) throws IOException {
		this.trueNegative = 0;
		this.truePositive = 0;
		try {
			this.br = new BufferedReader(new FileReader(path));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<String> generateCategories(String docID) {
		List<String> categoriesList = this.con.getCategory(docID);
		return categoriesList;
	}

	public void analyze() throws Exception {
		String line = "";
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		try {
			String id = "";
			while ((line = this.br.readLine()) != null) {
				
				if (line.contains("<ID>")) {
					//remove tags
					line = line.replaceFirst("<ID>", "");
					line = line.replaceFirst("</ID>", "");
					id = line;
				} else if (line.contains("<Categories>")) {
					this.br.readLine();
					List<String> lst = new ArrayList<String>();
					for (int i = 0; i < 5; i++) {
						line = br.readLine();
						line = line.split("=")[0];
						lst.add(line);
					}
					map.put(id, lst);
				}
			}
			
			checkValidity(map);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.br.close();
		}

	}
	
	public void checkValidity(Map<String, List<String>> map) {
		
		for(String key : map.keySet()) {
			
			if(checkCategories(key, map.get(key))) {
				this.truePositive++;
			} else {
				this.trueNegative++;
			}
			
		}
	}

	/**
	 * 
	 * @param businessID
	 * @return
	 */
	public boolean checkCategories(String businessID, List<String> topLabels) {
		List<String> categories = this.generateCategories(businessID);
		boolean flag = false;

		for (String label : topLabels) {
			for (String category : categories) {
				if (category.equals(label)) {
					flag = true;
				}
			}
		}
		System.out.println(businessID);
		System.out.println("labels:\t\t" + topLabels);
		System.out.println("categories:\t\t" +categories);
		return flag;
	}

	public void generateTrueFile(String fileName) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
		List<org.bson.Document> testingList = this.con.getBusinessTestingSet();
		try {
			for (org.bson.Document doc : testingList) {
				String docID = doc.getString("business_id");
				bw.write("<ID>" + docID + "</ID>\n");
				List<String> categories = this.con.getCategory(docID);
				bw.write("<Categories>\n");
				for (String category : categories) {
					bw.write(category + "\n");
				}
				bw.write("</Categories>\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			bw.close();
		}
	}

	public int getTruePositive() {
		return truePositive;
	}

	public void setTruePositive(int truePositive) {
		this.truePositive = truePositive;
	}

	public int getTrueNegative() {
		return trueNegative;
	}

	public void setTrueNegative(int trueNegative) {
		this.trueNegative = trueNegative;
	}

	public static void main(String[] args) throws IOException {
		Connector con = new Connector();
		String top10ReviewHits = "/top10reviewhitsnew.txt";
		String path = System.getProperty("user.home");
		FileInputStream is = new FileInputStream(path + top10ReviewHits);
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		try {
			int count = 0;
			String line = "";
			while ((line = br.readLine()) != null) {
				if (line.contains("<ID>")) {
					count++;
				}
			}

			System.out.println("There are "
					+ con.getBusinessTestingSet().size()
					+ " entries for testing\n");
			System.out.println("There are " + count + " entries in the file\n");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			br.close();
			isr.close();
			is.close();
		}
	}
}
