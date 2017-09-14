package ibm.cs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Optional;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;

import org.json.*;

import java.util.Iterator;
import org.json.JSONObject;
//import org.json.parser.JSONParser;
//import org.json.parser.ParseException;

public class talkFCCERestAPI implements Runnable {
	private Thread t;
	private String threadName;
	private String querykey;
	private String keytype;
	private String graphN;
	private long stTime;

	// private String dirName;

	private ArrayList<String> eventStringList = new ArrayList<String>();
	// private final CountDownLatch latch;

	// KafkaAnalyzer.mutex.acquire();
	// Optional.ofNullable(eventList).ifPresent(eventStringList::addAll);
	// System.out.println("Copied!!! " + name);
	// suuidHex = suuidHexStr;
	// dirName = dir;
	talkFCCERestAPI(String keyType, String queryK, String graphName, String name, long startTime) {
		threadName = name;
		querykey = queryK;
		keytype = keyType;
		stTime = startTime;
		graphN = graphName;
	}

	public void run() {
		try {
			HttpClient client = new DefaultHttpClient();
			// specify the host, protocol, and port
			HttpHost target = new HttpHost("ta2-marple-fcce1", 8080, "http");
			String queryStr = "";
			// specify the get request
			// HttpGet getRequest = new HttpGet("/forecastrss?p=80020&u=f");
			switch (keytype) {
		
			case "querypropbytype":
				queryStr = "/" + keytype + "/" + graphN + "/" + querykey;
				break;
			case "queryelembyproperty":
				queryStr = "/" + keytype + "/" + graphN + "/" + querykey;
				break;
			case "queryelembyuuid":
				queryStr = "/" + keytype + "/" + graphN + "/" + querykey;
				break;
			

			case "queryeventbytime":
				queryStr = "/" + keytype + "/" + graphN + "/" + querykey;
				break;
			case "queryeventbyentity":
				queryStr = "/" + keytype + "/" + graphN + "/" + querykey;
				break;
			}
			 System.out.println("HERE........INqueryStr" + queryStr);


			if (queryStr != "") {				
				HttpGet getRequest = new HttpGet(queryStr);
				System.out.println("Executing request:" +queryStr+ " to " + target);
				HttpResponse httpResponse = client.execute(target, getRequest);
				// HttpEntity entity = httpResponse.getEntity();

				System.out.println("----------------------------------------");
				//System.out.println(httpResponse.getStatusLine());
				Header[] headers = httpResponse.getAllHeaders();
				/*
				for (int i = 0; i < headers.length; i++) {
					System.out.println(headers[i]);
				}
				*/
				/*
				 * BufferedReader rd = new BufferedReader(new
				 * InputStreamReader(httpResponse.getEntity().getContent()));
				 * String line = ""; while ((line = rd.readLine()) != null) {
				 * System.out.println(line); }
				 */
				InputStream isResult = httpResponse.getEntity().getContent();
				System.out.println(isResult.toString());
				JsonParser parser = Json.createParser(isResult);
				while (parser.hasNext()) {
					Event e = parser.next();					
					if (e == Event.KEY_NAME) {
						String name = parser.getString();				
						switch (name) {
						
						case "event" :
							parser.next();
							System.out.print(name + ":..." +';');// + parser.toString());
							break;
						
						case "events" :
							parser.next(); 
							System.out.print(name + ":..." +';');// + parser.getString().replace("\n", "")+';');
							break;
						
							
						case "entities":
							parser.next();							
							System.out.print(name + ":..." +';');// + parser.getString().replace("\n", "")+';');					
							//System.out.print(name + ":" + parser.getString()+';');
							break;
							
						case "uuid":
							parser.next();
							//System.out.print(name + ":" + parser.getString().replace("\n", "")+';');
							// System.out.print(": ");
							break;
						case "type":
							parser.next();
							System.out.print(name + ":" + parser.getString().replace("\n", "")+';');
							// System.out.println("---------");
							break;
						case "subject":
							parser.next();
							System.out.print(name + ":" + parser.getString().replace("\n", "")+';');
							// System.out.println("---------");
							break;
						case "subtype":
							parser.next();
							System.out.print(name + ":" + parser.getString().replace("\n", "")+';');
							// System.out.println("---------");
							break;
						case "path":
							parser.next();
							System.out.print(name + ":" + parser.getString().replace("\n", "")+';');
							// System.out.println("---------");
							break;
						case "predicate":
							parser.next();
							//System.out.print(name + ":" + parser.getString().replace("\n", "")+';');
							// System.out.println("---------");
							break;
						case "path2":
							parser.next();
							System.out.print(name + ":" + parser.getString().replace("\n", "")+';');
							// System.out.println("---------");
							break;
						case "predicate2":
							parser.next();
							//System.out.print(name + ":" + parser.getString().replace("\n", "")+';');
							break;
						case "name":
							parser.next();
							System.out.print(name + ":" + parser.getString().replace("\n", "")+';');
							break;
							// System.out.println("---------");						
						case "programPoint":
							parser.next();
							System.out.print(name + ":" + parser.getString().replace("\n", "")+';');
							break;
						//case "timestampNanos": 
						//	parser.next();
						//	System.out.print("\n" + name + ":" + parser.getString()+';');
						case "timestampNanos":
							parser.next();	
							break;
							
						case "timeStampNano":
							parser.next();
							System.out.print("\n" + name + ":" + parser.getString().replace("\n", "")+';');
							// System.out.println("---------");
							break;
							
						default:
							System.out.println("NO handled nameType " + name);
							break;
						}
						}
					}
				}
				System.out.printf("\n" + "Takes: " + (System.nanoTime() - stTime) / 1000000 + "(MicroS) to process oneFCCE" + "\n");
				KafkaAnalyzer.suspEventSet.clear();
			
			/*
			 * HttpClient client = new DefaultHttpClient(); HttpPost post = new
			 * HttpPost("http://restUrl"); ///querypropbytype/cadets/_filename
			 * StringEntity input = new StringEntity("product");
			 * post.setEntity(input); HttpResponse response =
			 * client.execute(post); BufferedReader rd = new BufferedReader(new
			 * InputStreamReader(response.getEntity().getContent())); String
			 * line = ""; while ((line = rd.readLine()) != null) {
			 * System.out.println(line); }
			 */
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}

	public void start() {
		System.out.println("Starting " + threadName);
		if (t == null) {
			t = new Thread(this, threadName);
			t.start();
		}

	}

}
