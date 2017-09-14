package ibm.cs;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;


import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;






public class MaliciousPoint {
	public String orign;
	public String UUID;
	public Long timestamp;
	public double score = 0;
	public HashMap<String, Object> evidence = new HashMap<String, Object>();;
	
	
	
	public void add(String ngram)
	{
//		((HashSet)evidence.get("activities")).addAll(SignatureMatch.signatures.get(ngram));
	}
	
	public void addEvidence(String key,String value)
	{
		evidence.put(key, value);
	}
	
	public  String outputRecordAsJson()
	{
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonStr = "";
		try {
			jsonStr = mapper.writeValueAsString(this);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONObject tmpObject = new JSONObject(jsonStr);
//		System.out.println(tmpObject);
		return tmpObject.toString();
	}
}
