package ibm.cs;

/*	MATCH(f:EVENT_APP_UNKNOWN) WHERE f.uuid='00000000000000000000000019895559' return f;
		
		EDGE_EVENT_AFFECTS_MEMORY, EDGE_EVENT_AFFECTS_FILE, EDGE_EVENT_AFFECTS_NETFLOW, EDGE_EVENT_AFFECTS_SUBJECT, EDGE_EVENT_AFFECTS_SRCSINK, EDGE_EVENT_HASPARENT_EVENT, EDGE_EVENT_CAUSES_EVENT, EDGE_EVENT_ISGENERATEDBY_SUBJECT, EDGE_SUBJECT_AFFECTS_EVENT, EDGE_SUBJECT_HASPARENT_SUBJECT, EDGE_SUBJECT_HASLOCALPRINCIPAL, EDGE_SUBJECT_RUNSON, EDGE_FILE_AFFECTS_EVENT, EDGE_NETFLOW_AFFECTS_EVENT, EDGE_MEMORY_AFFECTS_EVENT, EDGE_SRCSINK_AFFECTS_EVENT, EDGE_OBJECT_PREV_VERSION, EDGE_FILE_HAS_TAG, EDGE_NETFLOW_HAS_TAG, EDGE_MEMORY_HAS_TAG, EDGE_SRCSINK_HAS_TAG, EDGE_SUBJECT_HAS_TAG, EDGE_EVENT_HAS_TAG, EDGE_EVENT_AFFECTS_REGISTRYKEY, EDGE_REGISTRYKEY_AFFECTS_EVENT, EDGE_REGISTRYKEY_HAS_TAG  

		*/

import java.nio.ByteBuffer;
import javax.xml.bind.DatatypeConverter;

import org.apache.kafka.clients.consumer.ConsumerConfig;

//import kafka.consumer.ConsumerConfig;
//import kafka.consumer.ConsumerIterator;
//import kafka.consumer.KafkaStream;
//import kafka.javaapi.consumer.ConsumerConnector;

import org.jgrapht.alg.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;

import java.awt.FontFormatException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
//import com.bbn.tc.schema.avro.cdm18.Principal;
import com.bbn.tc.schema.avro.cdm18.Event;
import com.bbn.tc.schema.avro.cdm18.Value;

import com.bbn.tc.schema.avro.cdm18.ValueDataType;
import com.bbn.tc.schema.avro.cdm18.NetFlowObject;
import com.bbn.tc.schema.avro.cdm18.ProvenanceTagNode;

//import com.bbn.tc.schema.avro.cdm18.SimpleEdge;
import com.bbn.tc.schema.avro.cdm18.SrcSinkObject;
import com.bbn.tc.schema.avro.cdm18.Subject;
import com.bbn.tc.schema.avro.cdm18.TCCDMDatum;
import com.bbn.tc.schema.avro.cdm18.UUID;
import com.bbn.tc.schema.serialization.AvroConfig;

import com.bbn.tc.schema.avro.cdm18.RegistryKeyObject; // fived
import com.bbn.tc.schema.avro.cdm18.TimeMarker; // fived

import com.bbn.tc.schema.avro.cdm18.UnitDependency; //trace 
import com.bbn.tc.schema.avro.cdm18.UnnamedPipeObject;//trace
import com.bbn.tc.schema.avro.cdm18.MemoryObject;


import com.bbn.tc.schema.avro.cdm18.ConfidentialityTag;
//import com.bbn.tc.schema.avro.cdm18.EdgeType;
import com.bbn.tc.schema.avro.cdm18.EventType;
import com.bbn.tc.schema.avro.cdm18.FileObject;
import com.bbn.tc.schema.avro.cdm18.IntegrityTag;

import com.bbn.tc.schema.avro.cdm18.Principal;

import com.bbn.tc.schema.avro.cdm18.SHORT;
import com.bbn.tc.schema.avro.cdm18.SubjectType;
//import com.bbn.tc.schema.avro.cdm18.TagEntity;
import com.bbn.tc.schema.avro.cdm18.TagOpCode;
import com.bbn.tc.schema.avro.cdm18.TagRunLengthTuple;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import kafka.Kafka;
//import src.edu.northwestern.marple.CUUID;
import scala.languageFeature.existentials;

public class KafkaAnalyzer {

	/*
	 * configuration variables
	 */
	private String zookeeper;
	private String consumerServer;
	private String producerServer;
	private String writeTopic;
	private String readTopic;
	private String groupId;
	private boolean exiton0;
	private String schemaFilename;
	public static ExecutorService service = Executors.newFixedThreadPool(5);

	/*
	 * kafka consumer and producer
	 */
	private KafkaConsumer consumer;
	// private ConsumerConnector consumer;
	private Producer producer;
	private String report_dir_path;
	public static String dumpSUUID;

	private static Map<String, Integer> typecnt = Collections.synchronizedMap((new HashMap<String, Integer>()));
	private Map<UUID, Integer> cntUUID = Collections.synchronizedMap(new HashMap<UUID, Integer>());

	/*
	 * global variables to store traces
	 */
	/*
	 * private HashMap<String, Double> blkSubjectScore = new HashMap<String,
	 * Double>();
	 */
	// stores all Subject nodes, and create index for uuid using hashmap : <uuid
	// of Subject, Subject>

	public static Set<String> suspSubUUID = Collections.synchronizedSet(new HashSet<String>());

	public static Map<String, String> whtSubjectStrs = Collections.synchronizedMap(new HashMap<String, String>());
	public static Map<String, String> cnnctSubjectStrs = Collections.synchronizedMap(new HashMap<String, String>());
	// private HashMap<UUID, String> whtSubjectNodes = new HashMap<UUID,
	// String>();
	public static Map<String, String> suspSubjectStrs = Collections.synchronizedMap(new HashMap<String, String>());
	// private HashMap<UUID, String> suspSubjectNodes = new HashMap<UUID,
	// String>();

	public static Object mutexAllSubjectStrs = new Object();
	public static Map<String, String> allSubjectStrs = Collections.synchronizedMap(new HashMap<String, String>());

	public static Map<String, String> allSuspBinderSubject = Collections.synchronizedMap(new HashMap<String, String>());

	// private HashMap<UUID, String> suspSubjectNodes = new HashMap<UUID,
	// String>();

	public static Map<String, String> hasLibArmSubjectNamesMap = Collections
			.synchronizedMap(new HashMap<String, String>());
	public static Map<String, String> subjectSuspStrMap = Collections.synchronizedMap(new HashMap<String, String>());
	public static Set<String> hasLibArmSubject = Collections.synchronizedSet(new HashSet<String>());
	// private HashMap<UUID, String> suspSubjectNodes = new HashMap<UUID,
	// String>();

	public static Map<String, Integer> subjectScoreVector = Collections.synchronizedMap(new HashMap<String, Integer>());
	// stores all Subject nodes, and create index for uuid using hashmap : <uuid
	// of Subject, Subject>

	// stores pending Events which haven't been linked to Subject, that is, no
	// EDGE_EVENT_ISGENERATEDBY_SUBJECT related to these Events found.
	private HashMap<String, Event> pendEventStrs = new HashMap<String, Event>();

	// stores all Event nodes, and create index for uuid using hashmap : <uuid
	// of Event, Event>

	public static Object mutexsuspEventStrs = new Object();
	public static Map<String, String> suspEventStrs = Collections.synchronizedMap(new HashMap<String, String>());
	public static Set<String> suspEventSet = Collections.synchronizedSet(new HashSet<String>());

	public static Object mutexsuspSubjEventUUIDs = new Object();
	public static Map<String, List<String>> suspSubjEventUUIDs = Collections
			.synchronizedMap(new HashMap<String, List<String>>());

	public static Map<String, List<String>> allSubjEventUUIDs = Collections
			.synchronizedMap(new HashMap<String, List<String>>());

	
	public static Map<String, String> pendingEventStrs = Collections.synchronizedMap(new HashMap<String, String>());

	// private HashMap<String, Event> suspEventNodes = new HashMap<String,
	// Event>();
	// private HashMap<String, String> suspEventNodeStrs = new HashMap<String,
	// String>();

	// Subject -> Event, 1:n, <uuid of Subject, ArrayList<uuid of Event>>
	// private HashMap<String, ArrayList<String>> subject2events = new
	// HashMap<String, ArrayList<String>>();

	// Event -> Subject, 1:1, <uuid of Event, uuid of Subject>
	private HashMap<String, String> event2subject = new HashMap<String, String>();

	// Subject -> Event, 1:n, <uuid of SrcSink, ArrayList<uuid of Event>>
	private HashMap<String, ArrayList<String>> srcsink2events = new HashMap<String, ArrayList<String>>();

	// Event -> Subject, 1:1, <uuid of Event, uuid of Sink>
	private HashMap<String, String> event2srcsink = new HashMap<String, String>();

	// sensitive SrcSinkObject, and create index for uuid using hashmap : <uuid
	// of SrcSinkObject, SrcSinkObject>
	public static Map<String, String> sensitiveSrcSink = Collections.synchronizedMap(new HashMap<String, String>());
	public static Map<String, String> sensitiveFile = Collections.synchronizedMap(new HashMap<String, String>());
	public static Map<String, String> sensitiveNetflow = Collections.synchronizedMap(new HashMap<String, String>());

	private HashMap<String, String> pendingSrcSinkObject = new HashMap<String, String>();

	public static Map<String, String> provUUID2ObjUUID = Collections.synchronizedMap(new HashMap<String, String>());

	// a set of white apps' name, <string of white app's name>
	public static Set<String> TRUSTAPP = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> TRUSTUUID = Collections.synchronizedSet(new HashSet<String>());

	public static Set<String> CONNECTAPP = Collections.synchronizedSet(new HashSet<String>());

	public static Set<String> TRUSTDIR = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> UNTRUSTDIR = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> MEDIADIR = Collections.synchronizedSet(new HashSet<String>());

	public static Set<String> UNTRUSTDIRCODE = Collections.synchronizedSet(new HashSet<String>());

	// private HashSet<String> SUSPAPI = new HashSet<String>();
	public static Set<String> PREEXFILAPI = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> EXFILAPI = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> FILEAPI = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> WIFIAPI = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> CONTENTAPI = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> LOCAPI = Collections.synchronizedSet(new HashSet<String>());

	public static Set<String> WLAPI = Collections.synchronizedSet(new HashSet<String>());

	public static Set<String> SUSPPARA = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> SENSRCSINK = Collections.synchronizedSet(new HashSet<String>());

	// a list of uuid of Subject, which inside the white app list, <uuid of
	// Subject>
	private HashSet<UUID> matchedWhiteApp = new HashSet<UUID>();

	public static long currTimeStamp = 0;
	private PrintWriter report_writer = null;
	public static PrintWriter eventTracWriter = null;
	public static PrintWriter eventAllWriter = null;
	public static PrintWriter subAllWriter = null;

	
	public static PrintWriter eventWriter = null;
	public static PrintWriter subWriter = null;
	public static PrintWriter subUUIDDetailWriter = null;
	public static PrintWriter cidGraphWriter = null;


	
	public static Object mutexProvGraph = new Object();
	public static DirectedGraph<String, DefaultEdge> provGraph = new DefaultDirectedGraph<String, DefaultEdge>(
			DefaultEdge.class);

	public static DirectedGraph<String, DefaultEdge> forkGraph = new DefaultDirectedGraph<String, DefaultEdge>(
			DefaultEdge.class);
	public static DirectedGraph<String, DefaultEdge> forkGraphUUID = new DefaultDirectedGraph<String, DefaultEdge>(
			DefaultEdge.class);
	public static HashMap<String, String> cid2UUIDMap = new HashMap<String, String>();


	
	DirectedGraph<String, Util.RelationshipEdge> scenarioGraph = new DirectedMultigraph<String, Util.RelationshipEdge>(
			new ClassBasedEdgeFactory<String, Util.RelationshipEdge>(Util.RelationshipEdge.class));

	public static Set<String> suspSrcTags = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> suspSinkTags = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> suspCtrlTags = Collections.synchronizedSet(new HashSet<String>());

	public static Set<String> processName = Collections.synchronizedSet(new HashSet<String>());

	// subj and its input Prov Tags to it, subj and its output Prov Tags to it.
	private HashMap<String, ArrayList<String>> subj2inputTags = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> subj2outputTags = new HashMap<String, ArrayList<String>>();

	public static Semaphore mutex = new Semaphore(1);

	// private String malProvNodesSrc =
	// "22592]:22579]:22581]:22581]:22582]:22562]:22565]:22594]:22593]:13531]"
	// + ":22575]:22570]:11168]:";
	// + ":25067]:23562]:23563:]";
	// private String malProvNodesSink =
	// "00000000000000000000000000005a19:00000000000000000000000000005c09:"
	// + "000000000000000000000000000061e9:00000000000000000000000000005832:" +
	// "0000000000000000000000000000583f:"
	// + "000000000000000000000000000061f3:";


	private HashMap<String, ArrayList<String>> filePolicyList = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> filePolicyListGen = new HashMap<String, ArrayList<String>>();

	
	public static Set<String> malSRC = Collections.synchronizedSet(new HashSet<String>());
	public static Set<String> malSINK = Collections.synchronizedSet(new HashSet<String>());

	
	
	
	private HashSet<String> fireFoxUUID = new HashSet<String>();
	// private boolean potentialSuspEvent = false;
	/**
	 * load configuration file
	 * 
	 * @param configFile
	 *            path/to/configFile
	 * @throws IOException
	 */

	private void addMalNodeToCheck(HashSet<String> malset, String malString) {
		String[] strs = malString.split(":");
		for (String s : strs) {
			malset.add(s);
		}
	}

	private String printEventParamDetails(Value v) {
		ValueDataType dataType = v.getValueDataType();
		String paraString = "";
		if (!v.isNull) {
			// parms.get(j).getValueBytes().
			ByteBuffer data;
			data = (ByteBuffer) v.getValueBytes();
			switch (dataType) {
			case VALUE_DATA_TYPE_INT:
				// System.out.print("INT: ");
				paraString += "INT: ";
				while (data.hasRemaining()) {
					// System.out.print(data.getInt());
					paraString += data.getInt();
				}
				// System.out.println("");
				break;

			case VALUE_DATA_TYPE_CHAR:
				paraString += ("CHAR: ");
				try {
					byte[] bytes = null;
					while (data.hasRemaining()) {
						bytes = new byte[data.remaining()];
						data.get(bytes);
					}
					String string = "";
					if (bytes != null) {
						string = new String(bytes, "Unicode");
						paraString += (string.replaceAll("\0", ""));
					}
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;

			case VALUE_DATA_TYPE_BOOL:
				paraString += "BOOL: ";
				break;

			case VALUE_DATA_TYPE_BYTE:
				paraString += "BYTE: ";
				while (data.hasRemaining()) {
					paraString += data.get();
				}
				// System.out.println("");
				break;

			case VALUE_DATA_TYPE_DOUBLE:
				paraString += ("DOUBLE: ");
				while (data.hasRemaining()) {
					paraString += (data.getDouble());
				}
				// System.out.println("");
				break;

			case VALUE_DATA_TYPE_FLOAT:
				paraString += ("FLOAT: ");
				while (data.hasRemaining()) {
					paraString += (data.getFloat());
				}
				// System.out.println("");
				break;

			case VALUE_DATA_TYPE_LONG:
				paraString += ("LONG: ");
				while (data.hasRemaining()) {
					paraString += (data.getLong());
				}
				// System.out.println("");
				break;

			case VALUE_DATA_TYPE_SHORT:
				paraString += ("SHORT: ");
				while (data.hasRemaining()) {
					paraString += data.getShort();
				}
				// System.out.println("");
				break;

			case VALUE_DATA_TYPE_COMPLEX: // not properly handled yet
				paraString += ("COMPLEX: ");
				// v.getRuntimeDataType()
				if (data != null) {
					while (data.hasRemaining()) {
						paraString += data.toString();
					}
				}
				// if (v!=null)
				// System.out.println("COMPLEX:" + v.toString());

				break;

			default:
				System.out.println("NO handled dataType" + dataType);
				break;
			}
		}
		return paraString + "";
	}

	private void matchPrintMal(String suspStr, String recordDetailStr) {
		for (String malnode : this.malSRC) {
			if (suspStr.contains(malnode)) {
				// report_writer.println("REPORT malNode SRC matched:: EventStr:
				// " + suspStr);
				System.out.println("REPORT malNode SRC:: " + suspStr);
				System.out.println("REPORT matched in:: " + recordDetailStr);
			}
		}

		for (String malnode : this.malSINK) {
			if (suspStr.contains(malnode)) {
				// report_writer.println("REPORT malNode SINK matched::
				// EventStr: " + suspStr);
				System.out.println("REPORT malNode SINK:: " + suspStr);
				System.out.println("REPORT matched in:: " + recordDetailStr);
			}
		}
	}
	/*
	 * private boolean matchAPI(String apicall, HashSet<String> suspAPI) {
	 * Iterator<String> itrAPI = suspAPI.iterator(); while (itrAPI.hasNext()) {
	 * if (apicall.contains(itrAPI.next())) return true; } return false; }
	 */

	/*
	 * private void processFile(String srcsinkUUID, String eventUUID, SimpleEdge
	 * edge) { // srcsink -> event if (!srcsink2events.containsKey(srcsinkUUID))
	 * { srcsink2events.put(srcsinkUUID, new ArrayList<String>()); }
	 * srcsink2events.get(srcsinkUUID).add(eventUUID); // event -> srcsink
	 * event2srcsink.put(eventUUID, srcsinkUUID);
	 * 
	 * if (this.pendingSrcSinkObject.containsKey(srcsinkUUID)) { if
	 * (this.suspEventNodes.containsKey(eventUUID)) { String fileDetailStr =
	 * this.pendingSrcSinkObject.get(srcsinkUUID); String eventDetailStr =
	 * this.suspEventNodes.get(event2srcsink.get(srcsinkUUID));
	 * this.sensitiveSrcSink.put(srcsinkUUID, fileDetailStr);
	 * report_writer.println("From fileDetailStr:" + fileDetailStr +
	 * "from Event:" + eventDetailStr); matchPrintMal(fileDetailStr); if
	 * (eventDetailStr != null) matchPrintMal(eventDetailStr); }
	 * this.pendingSrcSinkObject.remove(eventUUID); } }
	 * 
	 * private void processSrcSink(String srcsinkUUID, String eventUUID,
	 * SimpleEdge edge) { // srcsink -> event if
	 * (!this.srcsink2events.containsKey(srcsinkUUID)) {
	 * this.srcsink2events.put(srcsinkUUID, new ArrayList<String>()); }
	 * this.srcsink2events.get(srcsinkUUID).add(eventUUID);
	 * 
	 * // event -> srcsink this.event2srcsink.put(eventUUID, srcsinkUUID);
	 * 
	 * if (this.pendingSrcSinkObject.containsKey(srcsinkUUID)) { if
	 * (this.suspEventNodes.containsKey(eventUUID)) { String SrcSinkDetailStr =
	 * this.pendingSrcSinkObject.get(srcsinkUUID); String suspEventStr =
	 * this.suspEventNodes.get(event2srcsink.get(srcsinkUUID));
	 * this.sensitiveSrcSink.put(srcsinkUUID, SrcSinkDetailStr);
	 * report_writer.println("From SrcSinkDetail:" + SrcSinkDetailStr +
	 * "from Event:" + suspEventStr);
	 * matchPrintMal(this.sensitiveSrcSink.get(srcsinkUUID)); if (suspEventStr
	 * != null) matchPrintMal(this.sensitiveSrcSink.get(suspEventStr)); }
	 * this.pendingSrcSinkObject.remove(eventUUID); }
	 * 
	 * if (this.sensitiveSrcSink.containsKey(srcsinkUUID)) {
	 * report_writer.println("SrcSink:: " +
	 * this.sensitiveSrcSink.get(srcsinkUUID) + " effected by Event:: " +
	 * eventUUID + " Details::" + edge.get("timestamp"));
	 * matchPrintMal(this.sensitiveSrcSink.get(srcsinkUUID)); } }
	 */

	private void getProvPathforSubj() {
		// REPORT fileDetailStr
		// Details:FileObject::bf1661819b80c3942cc613648acf8962::
		// {"uuid": [-65, 22, 97, -127, -101, -128, -61, -108, 44, -58, 19, 100,
		// -118, -49, -119, 98], "baseObject": {"source":
		// "SOURCE_ANDROID_JAVA_CLEARSCOPE", "permission": null,
		// "lastTimestampMicros": null, "properties": null}, "url":
		// "/storage/emulated/0/gather.txt [-rw-rw----]", "isPipe": false,
		// "version": 1, "size": null}
		Set<String> subjs = this.suspSubjectStrs.keySet();
		// Set<String> subjsW = this.whtSubjectNodes.keySet();
		// "/data/data" "/storage" "/data/local/tmp" "/data/system"
		// "/dev/block/" "/proc"
		// System.out.println("PATHH start:\n");

		Iterator<String> itra = subjs.iterator();
		Iterator<String> itrb = subjs.iterator();
		while (itra.hasNext()) {
			String subja = itra.next();
			while (itrb.hasNext()) {
				String subjb = itrb.next();
				// Sysxtem.out.println("PATHH subja + subjb:"+ subja + ":" +
				// subjb +"\n");
				if (subja != subjb) {
					ArrayList<String> outputTags = this.subj2outputTags.get(subja);
					ArrayList<String> inputTags = this.subj2inputTags.get(subjb);
					if (inputTags != null && outputTags != null) {
						System.out.println("PATHH inputTags + outputTags:" + inputTags + ":" + outputTags + "\n");
						for (String oTag : outputTags) {
							for (String iTag : inputTags) {
								if (iTag != null && oTag != null && this.provGraph.containsVertex(iTag)
										&& this.provGraph.containsVertex(oTag)) {
									try {
										System.out.println("PATHH:" + "DijkstraShortestPath" + oTag + ":" + iTag);
										// System.out.println("PATHH:" +
										// DijkstraShortestPath.findPathBetween(this.provGraph,
										// oTag, iTag));

									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}
						}
					}
				}
			}
		}
		/*
		 * Iterator<String> itrc = subjs.iterator(); while (itrc.hasNext()) {
		 * String subjc = itrc.next(); ArrayList<String> outputTags =
		 * this.subj2outputTags.get(subjc); ArrayList<String> inputTags =
		 * this.subj2inputTags.get(subjc); for (String iTag : inputTags) { if
		 * (iTag != null) { try { //System.out.println("PATHH:" +
		 * DijkstraShortestPath.findPathBetween(this.provGraph, oTag, iTag)); }
		 * catch (Exception e) { e.printStackTrace(); }} } }
		 */

	}

	private void readConfiguration(String configFile) throws IOException {
		// load config file
		Properties props = new Properties();
		FileInputStream in = new FileInputStream(configFile);
		props.load(in);

		// set properties
		this.consumerServer = props.getProperty("ConsumerServer");
		this.producerServer = props.getProperty("ProducerServer");
		this.readTopic = props.getProperty("ReadTopic");
		this.writeTopic = props.getProperty("WriteTopic");
		this.exiton0 = Boolean.valueOf(props.getProperty("Exiton0"));
		this.groupId = props.getProperty("GroupId");
		this.schemaFilename = props.getProperty("SchemaFilename");
		this.zookeeper = props.getProperty("Zookeeper");

		// read global settings from system environments, if the corresponding
		// system env variables are set, use this value instead of the old one
		String producerServerEnv = System.getenv("PRODUCERSERVER");
		String writeTopicEnv = System.getenv("WRITETOPIC");
		String consumerServerEnv = System.getenv("CONSUMERSERVER");
		String readTopocEnv = System.getenv("READTOPIC");

		if (producerServerEnv != null)
			this.producerServer = producerServerEnv;
		if (writeTopicEnv != null)
			this.writeTopic = writeTopicEnv;
		if (consumerServerEnv != null)
			this.consumerServer = consumerServerEnv;
		if (readTopocEnv != null)
			this.readTopic = readTopocEnv;

	}

	/**
	 * load whitelist of app
	 * 
	 * @throws IOException
	 */
	private void readWhiteapplist(String whiteapplist) throws IOException {
		File file = new File(whiteapplist);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String tempString = null;
		while ((tempString = reader.readLine()) != null) {
			if (!tempString.contains("#")) {
				if (tempString.contains("::")) {
					String[] typeNames = tempString.trim().split("::");
					if (typeNames.length == 2) {
						String type = typeNames[0];
						String name = typeNames[1];
						switch (type) {
						case "TRUSTAPP":
							System.out.println("TRUSTAPP policy initialized :" + name);
							this.TRUSTAPP.add(name);
							break;
						case "TRUSTUUID":
							System.out.println("TRUSTUUID APP policy initialized :" + name);
							this.TRUSTUUID.add(name);
							break;

						case "CONNECTAPP":
							System.out.println("CONNECTAPP policy initialized :" + name);
							this.CONNECTAPP.add(name);
							break;
						case "UNTRUSTDIR":
							System.out.println("UNTRUSTDIR policy initialized :" + name);
							this.UNTRUSTDIR.add(name);
							break;
						case "UNTRUSTDIRCODE":
							System.out.println("UNTRUSTDIRCODE policy initialized :" + name);
							this.UNTRUSTDIRCODE.add(name);
							break;

						case "MEDIADIR":
							System.out.println("MEDIADIR policy initialized :" + name);
							this.MEDIADIR.add(name);
							break;

						case "EXFILAPI":
							System.out.println("EXFILAPI policy initialized :" + name);
							this.EXFILAPI.add(name);
							break;
						case "PREEXFILAPI":
							System.out.println("PREEXFILAPI policy initialized :" + name);
							this.PREEXFILAPI.add(name);
							break;
						case "FILEAPI":
							System.out.println("FILEAPI policy initialized :" + name);
							this.FILEAPI.add(name);
							break;
						case "WIFIAPI":
							System.out.println("WIFIAPI policy initialized :" + name);
							this.WIFIAPI.add(name);
							break;
						case "CONTENTAPI":
							System.out.println("CONTENTAPI policy initialized :" + name);
							this.CONTENTAPI.add(name);
							break;
						case "LOCAPI":
							System.out.println("LOCAPI policy initialized :" + name);
							this.LOCAPI.add(name);
							break;

						case "SENSRCSINK":
							System.out.println("SENSRCSINK policy initialized :" + name);
							this.SENSRCSINK.add(name);
							break;

						case "SUSPPARA":
							System.out.println("SUSPPARA policy initialized :" + name);
							this.SUSPPARA.add(name);
							break;

						case "WLAPI":
							System.out.println("WLAPI policy initialized :" + name);
							this.WLAPI.add(name);
							break;

						}
					}
				}
			}
		}
		reader.close();
	}

	/**
	 * configure Kafka consumer
	 */
	private Properties createConsumerProps() {
		Properties props = new Properties();

		// props.put("group.id", groupId);
		/*
		 * props.put("zookeeper.connect", this.zookeeper);
		 * props.put("zookeeper.session.timeout.ms", "400");
		 * props.put("zookeeper.sync.time.ms", "200");
		 * props.put("auto.commit.interval.ms", "10000");
		 * //https://cwiki.apache.org/confluence/display/KAFKA/Consumer+Group+
		 * Example
		 */

		props.put("group.id", groupId);
		props.put("bootstrap.servers", this.consumerServer);

		props.put("session.timeout.ms", "30000");
		props.put("auto.commit.interval.ms", "10000");

		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringDeserializer");
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				com.bbn.tc.schema.serialization.kafka.KafkaAvroGenericDeserializer.class);
		props.put(AvroConfig.SCHEMA_READER_FILE, schemaFilename);
		props.put(AvroConfig.SCHEMA_WRITER_FILE, schemaFilename);
		props.put(AvroConfig.SCHEMA_SERDE_IS_SPECIFIC, true);
		props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		return props;

	}
	
	private String encodePredicate(Event enode, String pred){
		//String predObjectHex = "predObject;;";
		String predType = "";
		boolean matched = false;			
		String[] subs = pred.split("/");
		
		if (subs.length > 1 && this.filePolicyList.containsKey(subs[1])) {
					//System.out.println(subs[1]);
					for (String i : this.filePolicyList.get(subs[1])) {						
						String[] regObjects = i.split(":;:");						
						if (pred.matches(regObjects[0])){
							matched = true;
							//System.out.println(pred1 + ' ' + regObjects[regObjects.length-1]);
							predType += regObjects[regObjects.length-1];
							break;							
						}
					}
		}
		
		if (!matched && subs.length > 1 && this.filePolicyListGen.containsKey(subs[1])) {
			//System.out.println(subs[1]);
			for (String i : this.filePolicyListGen.get(subs[1])) {						
				String[] regObjects = i.split(":;:");						
				if (pred.matches(regObjects[0])){
					matched = true;
					//System.out.println(pred1 + ' ' + regObjects[regObjects.length-1]);
					predType += regObjects[regObjects.length-1];
					break;							
				}
			}
		}
		
		if (!matched){						
			//predType += pred+ ":nomatch_t";
			predType += "nomatch_t";
		}		
		return predType;
	}

	/**
	 * take different actions for the corresponding datum Type
	 * @throws FontFormatException 
	 */
	private void processDatum17Dump(Object datum) throws FontFormatException {
		//System.out.println(datum.toString());
		String className = datum.getClass().getName().substring(23);
		//System.out.println("Processing:" + className);
		
		switch (className) {
		case "ENDOFMESSAGES":
			//System.out.println("THE END..................");
			break;
		case "Subject":
			Subject subject = (Subject) datum;
			String subjectStore = "";
			UUID subUUID = subject.getUuid();
			String subHexUUID = Util.getHexUUIDStr(subUUID);

			String subjDetailStr = Util.convertPrintCUUID(subUUID, subject, className, false);
			String parentUUID = "";
			if (subject.getParentSubject()!=null)
				parentUUID = Util.getHexUUIDStr(subject.getParentSubject());
			else {
				parentUUID = "null";
			}		
			subjectStore+= ","  + parentUUID;
		
			//String [] fields = subjDetailStr.split("::");
			
			if (subject.getCmdLine()!=null);
				subjectStore+= ","  + subject.getCmdLine();
		
			String cidStr = subject.getCid().toString();
			String startTime = subject.getStartTimestampNanos().toString();
			String ppidStr = "";
			String propString = cidStr;
			HashMap<CharSequence, CharSequence> props = (HashMap) subject.getProperties();		
			for (CharSequence i : props.keySet()) {
				String item = i.toString();
				if (item.contains("name") || item.contains("cwd") || item.contains("ppid")){
					
					if (item.contains("ppid")) ppidStr = props.get(i).toString();
					if (props.get(i)!=null){
						//fireFoxUUID.add(subHexUUID);
						propString += "," + props.get(i).toString();
					}					
				}	
				if (item.contains("time") && startTime.length()<5)
					startTime = props.get(i).toString().replace(".", "");						
				
			}
			
			
			//String subHexUUID = fields[1];
			//String appname = subject.getCmdLine().toString();			
		
			//if (subject.getType().toString() == "SUBJECT_PROCESS")				
			//System.out.println(subjDetailStr);
			
			//System.out.println(subjDetailStr);
		
			//System.out.println(propString);
			subjectStore+= ","  + propString;
			if (startTime.length()>15)
				subjectStore+=',' + startTime.substring(0, startTime.length()-6);
			else 
				subjectStore+=',' + startTime;
			 
			System.out.println(subject.toString());
			System.out.println(subjectStore);

			
			this.allSubjectStrs.put(subHexUUID, subjectStore);
			
			if (!this.cid2UUIDMap.containsKey(cidStr))
				this.cid2UUIDMap.put(cidStr, subHexUUID);
			else {
				if (this.cid2UUIDMap.get(cidStr)!=subHexUUID) {
					//System.out.println(subHexUUID +' '+ this.cid2UUIDMap.get(cidStr));
					this.cid2UUIDMap.put(cidStr, this.cid2UUIDMap.get(cidStr) + ';' + subHexUUID);
					/*
					System.out.println(subHexUUID +' '+ this.cid2UUIDMap.get(cidStr));
					System.out.println(this.allSubjectStrs.get(subHexUUID));	
					System.out.println(this.allSubjectStrs.get(this.cid2UUIDMap.get(cidStr)));
					*/
				}
			}
		
			
			
			if (!this.forkGraph.containsVertex(cidStr))
				this.forkGraph.addVertex(cidStr);			
			if (!this.forkGraph.containsVertex(ppidStr))
				this.forkGraph.addVertex(ppidStr);
			if (!this.forkGraph.containsEdge(cidStr, ppidStr))
				this.forkGraph.addEdge(cidStr, ppidStr);
			 
			String report_cid = this.report_dir_path + '/' + cidStr;
			File report_dir = new File(report_cid);
			if (!report_dir.exists())
				report_dir.mkdirs();	
			
			/*
			if (!this.forkGraphUUID.containsVertex(subHexUUID))
				this.forkGraphUUID.addVertex(subHexUUID);			
			if (!this.forkGraphUUID.containsVertex(parentUUID))
				this.forkGraphUUID.addVertex(parentUUID);
			if (!this.forkGraphUUID.containsEdge(subHexUUID, parentUUID))
				this.forkGraphUUID.addEdge(subHexUUID, parentUUID);
			*/
			
			
			
			/*
			if (provenanceTagNode.getPrevTagId() != null) { // Sequence
				UUID srcTagId = provenanceTagNode.getPrevTagId();
				String srcTagIDhex = Util.getHexUUIDStr(srcTagId);
				if (!this.provGraph.containsVertex(srcTagIDhex))
					this.provGraph.addVertex(srcTagIDhex);
				if (!this.provGraph.containsEdge(targetTagIdHex, srcTagIDhex))
					provGraph.addEdge(srcTagIDhex, targetTagIdHex);
			}

			List<UUID> tags = provenanceTagNode.getTagIds(); // all TAG_OP_UNION
			if (tags != null) {
				// System.out.println(pDetailStr);
				for (UUID srcTagId : tags) {
					String cSrcTagIdHex = Util.getHexUUIDStr(srcTagId);
					if (!this.provGraph.containsVertex(cSrcTagIdHex))
						this.provGraph.addVertex(cSrcTagIdHex);
					if (!this.provGraph.containsEdge(targetTagIdHex, cSrcTagIdHex))
						provGraph.addEdge(cSrcTagIdHex, targetTagIdHex);
				}
			}*/
			
			
			break;

			
			
		case "Event":
			
			
			Event enode = new Event();
			enode = (Event) datum;
			UUID eUUID = enode.getUuid();
			boolean addSuspEvent = false;
			
			String euuidHex = Util.getHexUUIDStr(eUUID);

			long timestamp = enode.getTimestampNanos();
			if (currTimeStamp < timestamp)
				currTimeStamp = timestamp;

			UUID sUUID = enode.getSubject();
			String suuidHex = Util.getHexUUIDStr(sUUID);
			
			String predObjectHex = "predObject;;";
			
			String predType = "";
			if (enode.getPredicateObject() != null ) {
					predObjectHex += Util.getHexUUIDStr(enode.getPredicateObject()) + ";;";
					//predType = "hasUUID1";
					String type = "";
					String pred1 = "";
					if (enode.getPredicateObjectPath() != null){//enode.getPredicateObjectPath() != "<unknown>"
						pred1 = enode.getPredicateObjectPath().toString();
						predObjectHex = predObjectHex + pred1 + ";;";
						type = encodePredicate(enode, pred1);						
					} 
					//System.out.println("type: " + type);
					//System.out.println("pred1: " + pred1);
					if (type =="") {
						if (pred1.contains("/")) {
							predType += ";nomatch_t1"; 					
							System.out.println(pred1);
						} else 		
							predType += ";notype_t1";
					} else {	
						predType += ";" + type;
					}
							
					//predType += ";" + type + "1";
			} else {
				predType = "noUUID1";
			}		 			
			
			
			if (enode.getPredicateObject2() != null ) {
				predObjectHex += Util.getHexUUIDStr(enode.getPredicateObject2()) + ";;";
				//predType += ";hasUUID2";
				String type = "";
				String pred2 = "";
				if (enode.getPredicateObject2Path() != null && enode.getPredicateObject2Path() != "<unknown>"){
					pred2 = enode.getPredicateObject2Path().toString();
					predObjectHex = predObjectHex + pred2 + ";;";
					type = encodePredicate(enode, pred2);					
				}
				if (type =="") {
					if (pred2.contains("/")) {
						predType += ";nomatch_t2";
					} else 		
						predType += ";notype_t2";
				} else {	
					predType += ";" + type;
				}
			} else {
				predType += ";noUUID2";
			}	
			
			
			
			/*
			boolean matched = false;
			
			if (enode.getPredicateObject() != null) {
				predObjectHex += Util.getHexUUIDStr(enode.getPredicateObject()) + ";;";				
				if (enode.getPredicateObjectPath() != null) {
					String pred1 = enode.getPredicateObjectPath().toString();
					predObjectHex = predObjectHex + pred1 + ";;";
					String[] subs = pred1.split("/");					
					if (subs.length > 0 && this.filePolicyList.containsKey(subs[1])) {
						//System.out.println(subs[1]);
						for (String i : this.filePolicyList.get(subs[1])) {						
							String[] regObjects = i.split(":;:");						
							if (pred1.matches(regObjects[0])){
								matched = true;
								System.out.println(pred1 + ' ' + regObjects[regObjects.length-1]);
								predType += regObjects[regObjects.length-1];
								break;							
							}
						}
						if (!matched){
							System.out.println(pred1 + " NOMATCH");
						}
					}
				}
			}*/
						
			String eventType = enode.getType().toString();
			String apicall = "";
			if (enode.get("name")!=null)
				apicall = enode.get("name").toString();
			Long eventTime = enode.getTimestampNanos();
			Long eSeq = enode.getSequence();
			Integer eTid = enode.getThreadId();
			CharSequence ppt = enode.getProgramPoint();

			
			/*						
			String assEventStr = Util.assemblyEventStr(predObjectHex, suuidHex, euuidHex, apicall, eventType, eventTime,
					eSeq, eTid, ppt);
			
			String eventParaStr = "";
			ArrayList<Value> paraList = new ArrayList<Value>();
			if (enode.getParameters() != null) {
				paraList.addAll(enode.getParameters());
			}
			eventParaStr = Util.printAllEventParamDetails(paraList, suuidHex);
			*/
			
			//System.out.println(assEventStr + ":;:" + eventParaStr);
			//events.add(assEventStr + ":;:" + eventParaStr);
			if (!eventType.contains("EVENT_UNIT")){ //eventType.contains("EVENT_FORK") || eventType.contains("EVENT_EXECUTE") || eventType.contains("EVENT_CLONE")) {
				List<String> events = KafkaAnalyzer.allSubjEventUUIDs.get(suuidHex);
				if (events == null) {
					events = new ArrayList<String>();
				//System.out.println(suuidHex + "events" + events);
					KafkaAnalyzer.allSubjEventUUIDs.put(suuidHex, events);
				}
				//"," + suuidHex +
				String eventTrace = eventTime.toString().substring(0, 13) + "," + eventType + ";" + predType + "," + suuidHex.substring(suuidHex.length()-6, suuidHex.length()) + "," + eTid + "," + eSeq;
				//this.eventWriter.println(eventTrace);// + "," + predObjectHex);				
				events.add(eventTrace);
			}
			
			

			
			//String tid = enode.getThreadId().toString();
			/*
			if (this.cid2UUIDMap.containsKey(tid)) {
				if (this.cid2UUIDMap.get(tid).contains(suuidHex))
					System.out.println(enode.getTimestampNanos());
				else 
					System.out.println("missing suuidHex");
			}
			else {
				System.out.println("missing cid" + enode.toString());
			} */
			
			
			
			/*
			if (fireFoxUUID.contains(suuidHex))
				System.out.println("FireFoxEventObject: "+assEventStr);// + " " + enode.toString());
			else 
				System.out.println("EventObject: "+assEventStr);// + " " + enode.toString());
			 */
			//String eventParaStr = Util.printAllEventParamDetails(paraList, suuidHex);
			break;
			
		case "ProvenanceTagNode":
			ProvenanceTagNode provenanceTagNode = (ProvenanceTagNode) datum;
			UUID targetTagId = provenanceTagNode.getTagId();

			String pDetailStr = Util.convertPrintCUUID(provenanceTagNode.getTagId(), provenanceTagNode, className, false);
			//System.out.println("ProvenanceTagNode: "+pDetailStr + provenanceTagNode.toString());		
			break;

		case "FileObject":
			FileObject fObject = (FileObject) datum;
			UUID fUUID = fObject.getUuid();
			String fDetailStr = Util.convertPrintCUUID(fUUID, fObject, className, false);
			String fuuidHex = Util.getHexUUIDStr(fUUID);
		
			String typeStr = fObject.get("type").toString();
			//System.out.println("FileObject: "+fDetailStr + fObject.toString());
			break;

		case "NetFlowObject":
			NetFlowObject netflow = (NetFlowObject) datum;
			UUID netUUID = netflow.getUuid();
			String netflowDetailStr = Util.convertPrintCUUID(netUUID, netflow, className, false);
			String netflowUUID = netflowDetailStr.split("::")[1];
			//System.out.println("NetFlowObject: "+netflowDetailStr + netflow.toString());
						
			break;

		case "SrcSinkObject":
			SrcSinkObject srcsinkObject = (SrcSinkObject) datum;
			UUID ssUUID = srcsinkObject.getUuid();
			String srcSinkDetailStr = Util.convertPrintCUUID(ssUUID, srcsinkObject, className, false);
			//this.malSRC.add(ssUUID.toString());
			//System.out.println("SrcSinkObject: "+srcSinkDetailStr);
		
			break;

		case "Principal":
			Principal principalObject = (Principal) datum;
			UUID pcpUUID = principalObject.getUuid();
			String pcpDetailStr = Util.convertPrintCUUID(pcpUUID, principalObject, className, false);
			//System.out.println("Principal: "+pcpDetailStr + " " + principalObject.toString());

			// System.out.println(pcpDetailStr);

			break;
			
		case "TimeMarker":
			TimeMarker tmObject = (TimeMarker) datum;
			// TimeMarker records are used to delineate time periods in a data stream to help consumers know their current read position in the data stream.
			//UUID tmUUID = tmObject.getTsNanos().toString() getUuid();
			//String pcpDetailStr = Util.convertPrintCUUID(tmUUID, tmObject, className, false);
			//System.out.println("TimeMarker: " + tmObject.toString());
			break;
		
		case "UnitDependency":
			//UnitDependency: {"unit": [48, 48, 49, 98, 99, 49, 52, 54, 49, 102, 48, 56, 49, 51, 48, 99], "dependentUnit": [55, 98, 98, 56, 53, 57, 48, 55, 97, 101, 100, 54, 51, 53, 49, 51]}
			UnitDependency unitDep = (UnitDependency) datum;			
			//System.out.println("UnitDependency: " + unitDep.toString());
			String uuidUnitDep = Util.getHexUUIDStr(unitDep.getUnit());
			String uuidDepUnitDep = Util.getHexUUIDStr(unitDep.getDependentUnit());
			//System.out.println("UnitDependency: " +uuidUnitDep + " dependentUnt: " + uuidDepUnitDep);
			//UUID ssUUID = srcsinkObject.getUuid();			
			
			break;
		
		case "MemoryObject":
			//memObject: {"uuid": [97, 98, 99, 48, 53, 51, 51, 48, 55, 50, 57, 102, 53, 101, 53, 49], "baseObject": {"permission": null, "epoch": null, "properties": {"tgid": "14379"}}, "memoryAddress": 139684987281408, "pageNumber": null, "pageOffset": null, "size": 4096}
			MemoryObject memObject = (MemoryObject) datum;			
			//System.out.println("MemoryObject: " + memObject.toString());
			break; 
	
		case "UnnamedPipeObject":
			//UnnamedPipeObject: {"uuid": [55, 100, 99, 56, 100, 51, 56, 101, 55, 56, 54, 101, 97, 100, 56, 48], "baseObject": {"permission": null, "epoch": 2, "properties": {"pid": "7517"}}, "sourceFileDescriptor": 3, "sinkFileDescriptor": 4}
			UnnamedPipeObject unPipeObject = (UnnamedPipeObject) datum;			
			//System.out.println("UnnamedPipeObject: " + unPipeObject.toString());
			break;
	
		case "RegistryKeyObject":
			//RegistryKeyObject: {"uuid": [-49, 12, -21, 5, 102, 23, 77, -124, -120, 29, 77, 103, 63, -19, -98, -109], "baseObject": {"permission": null, "epoch": null, "properties": null}, "key": "\\REGISTRY\\MACHINE\\SOFTWARE\\Classes\\CLSID\\{25336920-03F9-11CF-8FD0-00AA00686F13}\\ProgID", "value": {"size": -1, "type": "VALUE_TYPE_SRC", "valueDataType": "VALUE_DATA_TYPE_BYTE", "isNull": true, "name": null, "runtimeDataType": null, "valueBytes": null, "tag": null, "components": null}, "size": null}
			RegistryKeyObject regkeyObject = (RegistryKeyObject) datum;
			//System.out.println("RegistryKeyObject: " + regkeyObject.toString());
			break;
			
			
		default:
			System.out.println("CaseType Not Processed " +
			  datum.getClass().toString());
			break; // com.bbn.tc.schema.avro.cdm18.Principal
					// EDGE_SUBJECT_HASLOCALPRINCIPAL
		}
		
	}
	

	
	
	
	
	

	private void processDatum17(Object datum) throws FontFormatException {

		String className = datum.getClass().getName().substring(23);
		String type = datum.getClass().toString();

		if (!this.typecnt.containsKey((type))) {
			this.typecnt.put(type, 1);
		} else
			this.typecnt.replace(type, this.typecnt.get(type) + 1);

		String subHexUUID;
		UUID subUUID;

		UUID eUUID;
		UUID fUUID;
		// String netDetailStr;
		UUID netUUID;
		// String srcsinkUUID;
		UUID ssUUID;

		// Principle
		String pcpDetailStr;
		UUID pcpUUID;

		String eventDetailStr;
		String eventParamValue;
		// String eventIOType;
		String suspeventDetailStr;
		String srcSinkDetailStr;
		String subjDetailStr;
		// List<Value> parms = new ArrayList<Value>();
		// CUUID cuuid;
		// Event event;
		// SrcSinkObject srcSinkObject;
		// Object eventdatum = null;

		String pDetailStr;
		String fDetailStr;

		switch (className) {
		case "ENDOFMESSAGES":
			System.out.println("THE END..................");
			break;
		case "Subject":
			Subject subject = (Subject) datum;
			subUUID = subject.getUuid();
			subjDetailStr = Util.convertPrintCUUID(subUUID, subject, className, false);
			subHexUUID = subjDetailStr.split("::")[1];

			// matchPrintMal(subjDetailStr);
			// whether in the white app list
			String appname = subject.getCmdLine().toString();
			KafkaAnalyzer.processName.add(appname);// add all processName
			// System.out.println("processName::appname: " + appname + "
			// Details:: " + subjDetailStr);

			this.allSubjectStrs.put(subHexUUID, subjDetailStr);
			if (this.TRUSTAPP.contains(appname)) {
				// System.out.println("White::appname: " + appname + "
				// subjectDetails:: " + subjDetailStr);
				this.whtSubjectStrs.put(subHexUUID, subjDetailStr);
				// this.whtSubjectNodes.put(subUUID, subjDetailStr);
				KafkaAnalyzer.subAllWriter.println("Trust::appname: " + appname + " trustDetails:: " + subjDetailStr);
				// System.out.println(subject.getCmdLine() + "in the
				// whitelist
				// app");
			} else if (this.CONNECTAPP.contains(appname)) {
				this.cnnctSubjectStrs.put(subHexUUID, subjDetailStr);
				// this.whtSubjectNodes.put(subUUID, subjDetailStr);
				KafkaAnalyzer.subAllWriter
						.println("CONNECTAPP::appname: " + appname + "Connected trustDetails:: " + subjDetailStr);

			} else if (!this.TRUSTUUID.contains(subHexUUID)) {
				this.suspSubjectStrs.put(subHexUUID, subjDetailStr);
				// this.suspSubjectNodes.put(subUUID, subjDetailStr);
				KafkaAnalyzer.subAllWriter
						.println("Suspicious::appname: " + appname + " subjectDetails:: " + subjDetailStr);
			}
			// subject.getClass().toString() + ":" + subject.getPid().toString()
			// + ":" + subject.getPpid()
			// + ":" + subject.getStartTimestampMicros().toString() + ":" + ;
			break;

		case "ProvenanceTagNode":
			// ArrayList<Object> srcTagIdList;
			// provenance UNION SEQ tagID->tagID and the fileObject->tagID,
			// tagID->networkflow,
			ProvenanceTagNode provenanceTagNode = (ProvenanceTagNode) datum;
			UUID targetTagId = provenanceTagNode.getTagId();

			// System.out.println(provenanceTagNode);
			pDetailStr = Util.convertPrintCUUID(provenanceTagNode.getTagId(), provenanceTagNode, className, false);

			if (!provenanceTagNode.getTagId().toString().contains(",")) {
				System.out.println(pDetailStr);
			}
			// System.out.println(pDetailStr);
			/*
			 * if (provenanceTagNode.getFlowObject()!=null)
			 * this.matchPrintMal(provenanceTagNode.getFlowObject().toString(),
			 * pDetailStr);
			 */
			String targetTagIdHex = Util.getHexUUIDStr(targetTagId);
			if (!KafkaAnalyzer.provUUID2ObjUUID.containsKey(targetTagIdHex)) {
				String flowObjectUUID = Util.getHexUUIDStr(provenanceTagNode.getFlowObject());
				// System.out.println("MAPPING::" + + ":" + targetTagIdHex);
				KafkaAnalyzer.provUUID2ObjUUID.put(targetTagIdHex, flowObjectUUID);
			}

			// if (this.suspSrcTags.contains(s))
			if (!this.provGraph.containsVertex(targetTagIdHex))
				this.provGraph.addVertex(targetTagIdHex);
			if (provenanceTagNode.getPrevTagId() != null) { // Sequence
				UUID srcTagId = provenanceTagNode.getPrevTagId();
				String srcTagIDhex = Util.getHexUUIDStr(srcTagId);
				if (!this.provGraph.containsVertex(srcTagIDhex))
					this.provGraph.addVertex(srcTagIDhex);
				if (!this.provGraph.containsEdge(targetTagIdHex, srcTagIDhex))
					provGraph.addEdge(srcTagIDhex, targetTagIdHex);
			}

			List<UUID> tags = provenanceTagNode.getTagIds(); // all TAG_OP_UNION
			if (tags != null) {
				// System.out.println(pDetailStr);
				for (UUID srcTagId : tags) {
					String cSrcTagIdHex = Util.getHexUUIDStr(srcTagId);
					if (!this.provGraph.containsVertex(cSrcTagIdHex))
						this.provGraph.addVertex(cSrcTagIdHex);
					if (!this.provGraph.containsEdge(targetTagIdHex, cSrcTagIdHex))
						provGraph.addEdge(cSrcTagIdHex, targetTagIdHex);
				}
			}
			// if (provenanceTagNode.getPrevTagId() != null &&
			// provenanceTagNode.getOpcode() != null) {
			// System.out.println(pDetailStr);
			// }
			break;

		case "FileObject":
			FileObject fObject = (FileObject) datum;
			fUUID = fObject.getUuid();
			fDetailStr = Util.convertPrintCUUID(fUUID, fObject, className, false);
			String fuuidHex = Util.getHexUUIDStr(fUUID);
			/*
			 * String pefInfo = fileObject.getBaseObject().toString(); SHORT p =
			 * fileObject.getBaseObject().getPermission();//
			 * 
			 * if (pefInfo!=null) System.out.println(' ' +pefInfo);
			 */
			// /storage/emulated/
			// /data/local/tmp/MainActivity-debug.apk
			// /data/app/vmdl591819199.tmp/base.apk
			// /sys/kernel/debug
			// /proc/
			// ....
			// this.malSRC.add(fUUID.toString());

			String typeStr = fObject.get("type").toString();
			// System.out.println("fDetailStr"+fDetailStr);
			HashMap<CharSequence, CharSequence> props = (HashMap) fObject.getBaseObject().getProperties();
			for (CharSequence i : props.keySet()) {
				if (i.toString().contains("path") && !Util.matchSuspStr(props.get(i).toString(), this.MEDIADIR)
						&& (Util.matchSuspStr(props.get(i).toString(), UNTRUSTDIR)
								|| Util.matchSuspStr(props.get(i).toString(), UNTRUSTDIRCODE)))
					sensitiveFile.put(fuuidHex, fDetailStr);
			}

			// Util.matchSuspStr(fObject.)

			// two types
			// typeStr!="FILE_OBJECT_DIR"
			// &&
			// typeStr!="FILE_OBJECT_FILE"
			// System.out.println(typeStr + ' ' +
			// fObject.getBaseObject().getProperties().toString());
			// System.out.println(typeStr + ' ' + fDetailStr);
			// FileObject::b7b075f01b382b7325be89b42e5c5f44::
			// {"uuid": [-73, -80, 117, -16, 27, 56, 43, 115, 37, -66, -119,
			// -76, 46, 92, 95, 68],
			// "baseObject": {"permission": [1, -92], "epoch": null,
			// "properties": {"path":
			// "/system/app/SoundRecorder/SoundRecorder.apk"}},
			// "type": "FILE_OBJECT_FILE", "fileDescriptor": null,
			// "localPrincipal": null, "size": null, "peInfo": null, "hashes":
			// null}
			break;

		case "NetFlowObject":
			NetFlowObject netflow = (NetFlowObject) datum;
			netUUID = netflow.getUuid();
			String netflowDetailStr = Util.convertPrintCUUID(netUUID, netflow, className, false);
			String netflowUUID = netflowDetailStr.split("::")[1];

			// System.out.println(netflowDetailStr);
			// if (Util.matchSuspStr(srcSinkDetailStr, this.SENSRCSINK)){
			// System.out.println(srcsinkObject.getBaseObject().toString());
			if (!netflowDetailStr.contains("unknown"))
				sensitiveNetflow.put(netflowUUID, netflowDetailStr);
			// }

			// NetFlowObject::00000000000000000000000000000081::
			// {"uuid": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -127],
			// "baseObject": {"permission": null, "epoch": null, "properties":
			// null},
			// "localAddress": "::", "localPort": -1, "remoteAddress":
			// "<UNKNOWN>", "remotePort": -1,
			// "ipProtocol": 0, "fileDescriptor": null}
			// this.malSINK.add(netUUID.toString());
			// this.matchPrintMal(netflowUUID, netflowDetailStr);
			// System.out.println(netflowDetailStr);
			break;

		case "SrcSinkObject":
			SrcSinkObject srcsinkObject = (SrcSinkObject) datum;
			ssUUID = srcsinkObject.getUuid();
			srcSinkDetailStr = Util.convertPrintCUUID(ssUUID, srcsinkObject, className, false);
			this.malSRC.add(ssUUID.toString());

			// System.out.println(srcsinkObject.toString());
			if (Util.matchSuspStr(srcSinkDetailStr, this.SENSRCSINK)) {
				// System.out.println(srcsinkObject.getBaseObject().toString());
				sensitiveSrcSink.put(Util.getHexUUIDStr(ssUUID),
						srcsinkObject.getType().toString() + srcsinkObject.getBaseObject().toString());
			}

			/*
			 * if
			 * (srcsinkObject.getType().toString().contains("SRCSINK_DATABASE"))
			 * if (srcsinkObject.getBaseObject() !=null)
			 * System.out.println(srcsinkObject.getBaseObject().toString());
			 */
			// SrcSinkObject::00000000000000000000000000007f4d:: // like the
			// fileobjects...
			// {"uuid": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 127, 77],
			// "baseObject": {"permission": null, "epoch": null, "properties":
			// null},
			// "type": "SRCSINK_PERMISSIONS", "fileDescriptor": null}

			// SRCSINK_ACTIVITY_MANAGEMENT SRCSINK_ALARM_SERVICE
			// SRCSINK_AUDIO_IO SRCSINK_BACKUP_MANAGER
			// SRCSINK_BINDER SRCSINK_BLUETOOTH
			// SRCSINK_BROADCAST_RECEIVER_MANAGEMENT SRCSINK_CAMERA
			// SRCSINK_CONTENT_PROVIDER SRCSINK_CONTENT_PROVIDER_MANAGEMENT
			// SRCSINK_DATABASE
			// SRCSINK_DEVICE_ADMIN SRCSINK_DEVICE_SEARCH SRCSINK_DEVICE_USER
			// SRCSINK_DISPLAY SRCSINK_FILE_SYSTEM
			// SRCSINK_FILE_SYSTEM_MANAGEMENT
			// SRCSINK_FINGERPRINT SRCSINK_IDLE_DOCK_SCREEN
			// SRCSINK_INSTALLED_PACKAGES
			// SRCSINK_JSSE_TRUST_MANAGER SRCSINK_KEYCHAIN SRCSINK_KEYGUARD
			// SRCSINK_MEDIA_LOCAL_MANAGEMENT SRCSINK_MEDIA_LOCAL_PLAYBACK
			// SRCSINK_MEDIA_REMOTE_PLAYBACK SRCSINK_NETWORK_MANAGEMENT
			// SRCSINK_NFC SRCSINK_NOTIFICATION SRCSINK_PERMISSIONS
			// SRCSINK_POSIX SRCSINK_POWER_MANAGEMENT SRCSINK_PRINT_SERVICE
			// SRCSINK_PROCESS_MANAGEMENT SRCSINK_RECEIVER_MANAGEMENT
			// SRCSINK_RPC
			// SRCSINK_SCREEN_AUDIO_CAPTURE SRCSINK_SERVICE_MANAGEMENT
			// SRCSINK_SMS_MMS
			// SRCSINK_SPEECH_INTERACTION SRCSINK_STATUS_BAR SRCSINK_TELEPHONY
			// SRCSINK_THREADING SRCSINK_UI SRCSINK_USAGE_STATS SRCSINK_USB
			// SRCSINK_USER_ACCOUNTS_MANAGEMENT SRCSINK_VIBRATOR
			// SRCSINK_WALLPAPER_MANAGER
			// SRCSINK_WEB_BROWSER SRCSINK_WIDGETS

			// MATCH (n:SrcSinkObject) WHERE n.type='SRCSINK_POSIX' RETURN n
			// LIMIT 2
			// MATCH (n:SrcSinkObject) WHERE
			// n.uuid='00000000-0000-0000-0000-0000000001fd' RETURN n LIMIT 25

			break;

		case "Principal":
			Principal principalObject = (Principal) datum;
			pcpUUID = principalObject.getUuid();
			pcpDetailStr = Util.convertPrintCUUID(pcpUUID, principalObject, className, false);
			// System.out.println(pcpDetailStr);

			break;

		case "Event": // suspicious event match suspicious predicate objects
						// (/emulator/..), match suspicious event types...

			Event enode = new Event();
			enode = (Event) datum;
			eUUID = enode.getUuid();
			boolean addSuspEvent = false;

			String euuidHex = Util.getHexUUIDStr(eUUID);

			/*
			 * String eDetailStr = Util.convertPrintCUUID(eUUID, enode,
			 * className, false); String eUUIDHex = eDetailStr.split("::")[1];
			 */
			long timestamp = enode.getTimestampNanos();
			if (currTimeStamp < timestamp)
				currTimeStamp = timestamp;

			UUID sUUID = enode.getSubject();
			String suuidHex = Util.getHexUUIDStr(sUUID);

			String predObjectHex = "predObject;;";
			if (enode.getPredicateObject() != null) {
				predObjectHex += Util.getHexUUIDStr(enode.getPredicateObject()) + ";;";
				if (enode.getPredicateObjectPath() != null)
					predObjectHex = predObjectHex + enode.getPredicateObjectPath().toString() + ";;";
			}
			if (enode.getPredicateObject2() != null) {
				predObjectHex += Util.getHexUUIDStr(enode.getPredicateObject2()) + ";;";
				if (enode.getPredicateObject2Path() != null)
					predObjectHex += enode.getPredicateObject2Path().toString() + ";;";
			}

			String eventType = enode.getType().toString();
			String apicall = enode.get("name").toString();
			Long eventTime = enode.getTimestampNanos();
			Long eSeq = enode.getSequence();
			Integer eTid = enode.getThreadId();
			CharSequence ppt = enode.getProgramPoint();

			// String assEventStr = "";
			String assEventStr = Util.assemblyEventStr(predObjectHex, suuidHex, euuidHex, apicall, eventType, eventTime,
					eSeq, eTid, ppt);
			// System.out.println("assEventStr" + assEventStr);

			// this.potentialSuspEvent = false;
			// String eventParaStr = Util.printAllEventParamDetails(enode);

			ArrayList<Value> paraList = new ArrayList<Value>();
			if (enode.getParameters() != null) {
				paraList.addAll(enode.getParameters());
			}

			String eventParaStr = "";
			eventParaStr = Util.printAllEventParamDetails(paraList, suuidHex);

			if (eventParaStr.contains("allNormalBinderIPC")) { // && &&
																// eventParaStr.length()
																// > 1
																// eventParaStr.contains("MatchProcName")){
																// &&
																// potentialSuspEvent
				// if
				// (eventParaStr.contains("00000000000000000000000000000000000000000000000000000000"))
				// this.eventAllWriter.println("BinderIPC-suspEvent:" +
				// assEventStr + " eventParaStr:" + eventParaStr + " Event" +
				// enode.toString());
				KafkaAnalyzer.eventAllWriter.println("Susp-NormalBinder-ipc:" + assEventStr + ":;:eventParaStr:"
						+ eventParaStr + ":;:EventUUID:" + euuidHex);
				addSuspEvent = true;
			}

			if (assEventStr.contains("native") || eventParaStr.contains("native")) { // &&
																						// &&
																						// eventParaStr.length()
																						// >
																						// 1
				KafkaAnalyzer.eventAllWriter.println(
						"Susp-native:" + assEventStr + ":;:eventParaStr:" + eventParaStr + ":;:EventUUID:" + euuidHex);
				addSuspEvent = true;
			}

			if (eventParaStr.contains("android.permission")) { // && &&
																// eventParaStr.length()
																// > 1
				KafkaAnalyzer.eventAllWriter.println("Susp-permission:" + assEventStr + ":;:eventParaStr:"
						+ eventParaStr + ":;:EventUUID:" + euuidHex);
				addSuspEvent = true;
			}

			if (eventType.contains("EVENT_LOADLIBRARY")) {
				KafkaAnalyzer.eventAllWriter.println(
						"Susp-loadlib:" + assEventStr + ":;:eventParaStr:" + eventParaStr + ":;:EventUUID:" + euuidHex);
				addSuspEvent = true;
			}

			if (KafkaAnalyzer.dumpSUUID.contains(suuidHex))
				eventAllWriter.println(
						suuidHex + ' ' + assEventStr + ":;:eventParaStr:" + eventParaStr + ":;:EventUUID:" + euuidHex);

			if (eventParaStr.contains("potentialSuspEvent") && !this.whtSubjectStrs.containsKey(suuidHex)) {
				addSuspEvent = true; // add suspicious event based on parameter
										// values;
			}

			if (Util.matchSuspStr(apicall, this.CONTENTAPI)) {
				// System.out.println(eventType + suuidHex + " CONTENTAPI" +
				// apicall + printAllEventParamDetails(enode));
				addSuspEvent = true;
				Util.updateSubScoreVector(this.subjectScoreVector, suuidHex, 1);
			}
			if (Util.matchSuspStr(apicall, this.WIFIAPI)) {
				// System.out.println(eventType + suuidHex + " WIFIAPI" +
				// apicall + printAllEventParamDetails(enode));
				addSuspEvent = true;
				Util.updateSubScoreVector(this.subjectScoreVector, suuidHex, 2);
			}

			if (Util.matchSuspStr(apicall, this.LOCAPI)) {
				addSuspEvent = true;
				Util.updateSubScoreVector(this.subjectScoreVector, suuidHex, 4);
			}

			if (Util.matchSuspStr(apicall, this.PREEXFILAPI)) {
				addSuspEvent = true;
				Util.updateSubScoreVector(this.subjectScoreVector, suuidHex, 8);
			}

			// System.out.println(suuidHex + " LOCAPI" + apicall +
			// printAllEventParamDetails(enode));
			if (Util.matchSuspStr(apicall, this.FILEAPI)) {
				addSuspEvent = true;
				// String paraAll = printAllEventParamDetails(enode);
				String matchStr = "";
				CharSequence predicateOne = enode.getPredicateObjectPath();
				CharSequence predicateTwo = enode.getPredicateObject2Path();
				if (predicateOne != null) {
					matchStr = predicateOne.toString();
				}
				if (predicateTwo != null) {
					matchStr += predicateTwo.toString();
				}
				int updateValue = 0;
				if (eventType.contains("WRITE") || eventType.contains("CREATE_OBJECT")) {
					if (Util.matchSuspStr(matchStr, UNTRUSTDIR) || Util.matchSuspStr(matchStr, UNTRUSTDIRCODE)) {
						updateValue = 64;
						if (!(this.allSubjectStrs.containsKey(suuidHex)
								&& this.allSubjectStrs.get(suuidHex).contains("media"))) {
							KafkaAnalyzer.eventAllWriter.println(
									suuidHex + ":Suspicious [ConfidentialSrc ==> UntrustedFileSink] Pattern Matched "
											+ matchStr);
							KafkaAnalyzer.eventAllWriter
									.println("Suspicious Provenance Graph and Event Traces Updated!!\n");
						}
						// System.out.println(suuidHex + "::SENDTO_EXFILAPI::" +
						// eventType + "::" + enode.getTimestampNanos() + "::" +
						// apicall + "::" + printAllEventParamDetails(enode));
						// System.out.println("::add"+this.suspEventNodeStrs.get(eUUIDHex).toString());
						// System.out.println("::add"+this.suspEventNodes.get(eUUIDHex).toString());
						// System.out.println(eventType + ' ' + matchStr +
						// suuidHex + " FILEAPI:" + apicall);
					}
				} else { //
					if (Util.matchSuspStr(matchStr, UNTRUSTDIR) && !Util.matchSuspStr(matchStr, MEDIADIR)) {
						updateValue = 16;
						if (!(this.allSubjectStrs.containsKey(suuidHex)
								&& this.allSubjectStrs.get(suuidHex).contains("media"))) {
							KafkaAnalyzer.eventAllWriter.println(suuidHex
									+ ":Suspicious [ConfidentialFile ==> UntrustedProc] Pattern Matched" + matchStr);
							KafkaAnalyzer.eventAllWriter
									.println("Suspicious Provenance Graph and Event Traces Updated!!\n");
						}

						// System.out.println(eventType + ' ' + matchStr +
						// suuidHex + " FILEAPI:" + apicall);
					}
				}
				Util.updateSubScoreVector(this.subjectScoreVector, suuidHex, updateValue);
			}

			if (Util.matchSuspStr(apicall, this.EXFILAPI)) {
				addSuspEvent = true;
				int updateValue = 0;
				if (eventType.contains("SENDTO") || eventType.contains("SENDMSG") || eventType.contains("WRITE")) {
					updateValue = 128;
					// System.out.println("SENDTO:" + enode + ' '+ assEventStr +
					// " eventParaStr:" + eventParaStr);
					KafkaAnalyzer.eventAllWriter.println(
							suuidHex + ":Suspicious [ConfidentialSrc ==> NetworkSink] Pattern Matched" + apicall);
					KafkaAnalyzer.eventAllWriter.println("Suspicious Provenance Graph and Event Traces Updated!!\n");
				} else {
					updateValue = 32;
					KafkaAnalyzer.eventAllWriter
							.println(suuidHex + ":Suspicious [NetworkSrc ==> UntrustedProc] Pattern Matched" + apicall);
					KafkaAnalyzer.eventAllWriter.println("Suspicious Provenance Graph and Event Traces Updated!!\n");

					// String paraAll = printAllEventParamDetails(enode);
					// System.out.println(eventType + suuidHex + " EXFILAPI" +
					// apicall + paraAll);
				}
				// System.out.println(eventType +
				// printAllEventParamDetails(enode) + suuidHex + " EXFILAPI" +
				// apicall);
				// System.out.println(eventType + suuidHex + " EXFILAPI" +
				// apicall + );
				Util.updateSubScoreVector(this.subjectScoreVector, suuidHex, updateValue);
			}

			// System.out.println(suuidHex + " EXFILAPI" + apicall +
			// printAllEventParamDetails(enode));
			// if (enode.get get("tag")!=null)
			// System.out.println(enode.get("name").toString() + ' ' +
			// enode.get("tag").toString());

			// System.out.println(suuidHex + "::predObjectHex::" + predObjectHex
			// + "::" + eventType + "::" + enode.getTimestampNanos() + "::" +
			// apicall + "::" + printAllEventParamDetails(enode));

			if (addSuspEvent) {
				if (!KafkaAnalyzer.suspEventSet.contains(euuidHex)) {
					KafkaAnalyzer.suspEventSet.add(euuidHex);
					KafkaAnalyzer.suspEventStrs.put(euuidHex, assEventStr);
					if (!Util.matchSuspStr(apicall, WLAPI)) {
						List<String> events = KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex);
						if (events == null) {
							events = new ArrayList<String>();
							KafkaAnalyzer.suspSubjEventUUIDs.put(suuidHex, events);
						}
						// eUUIDs.add(eUUIDHex + "::" + eSeq +"::" + apicall);
						events.add(assEventStr + ":;:" + eventParaStr);
					}
					// System.out.println("potentialSuspEvent:" + assEventStr +
					// ' ' + eventParaStr);
					// ArrayList<Value> paraList = new ArrayList<Value>();
					if (enode.getParameters() != null) {
						// paraList.addAll(enode.getParameters());
						Util.addEventInOutProvsDetails(paraList, eventType);
					}
				}
				/*
				 * else { System.out.println("::NotAddDupliKey"+
				 * enode.toString()); }
				 */
				// System.out.println(":::added");
				// this.suspEventStrs.put(eUUIDHex, suuidHex +
				// "::predObjectHex::" + predObjectHex + "::" +
				// enode.getTimestampNanos() + "::" + apicall + "::" +
				// printAllEventParamDetails(enode));
				// this.suspEventNodes.put(eUUIDHex, enode);
				// System.out.println(this.suspEventNodes.get(eUUIDHex).toString());
				// this.suspEventStrs.put(eUUIDHex, eDetailStr);
			}
			// }

			break;

		default:
			/*
			 * String type = datum.getClass().toString(); if
			 * (!typecnt.containsKey((type))) { typecnt.put(type, 1); } else
			 * typecnt.replace(type, typecnt.get(type) + 1);
			 */
			// System.out.println("CaseType Not Processed " +
			// datum.getClass().toString());
			break; // com.bbn.tc.schema.avro.cdm18.Principal
					// EDGE_SUBJECT_HASLOCALPRINCIPAL
		}
	}

	/*
	 * switch (eventType) { case "EVENT_SIGNAL": break; case "EVENT_FNCTL":
	 * break; case "EVENT_MOUNT": break; case "EVENT_MPROTECT": break; case
	 * "EVENT_FORK": break; case "EVENT_LOADLIBRARY": // Runtime.nativeLoad
	 * break; case "EVENT_RECVMSG": break; case "EVENT_SENDMSG": break; case
	 * "EVENT_DUP": break; case "EVENT_TRUNCATE": break; case
	 * "EVENT_CHANGE_PRINCIPAL": break; case "EVENT_MMAP": break; case
	 * "EVENT_EXECUTE": break; case "EVENT_MODIFY_PROCESS": break; case
	 * "EVENT_EXIT": break;
	 * 
	 * 
	 * case "EVENT_READ":
	 * 
	 * //System.out.println(eventType + eDetailStr); // grep EVENT_READ
	 * cs2017.txt | grep -v BINDER | grep -v unknown // | grep -v
	 * SQLiteConnection // | grep -v CursorWindow | grep
	 * LocalSocketImpl.readba_native // -v | grep -v Posix.read // grep -v
	 * libcore.io.Posix.sysconf libcore.io.Posix.getenv // | grep -v
	 * LocalSocketImpl.read_native | grep -v // NativeCrypto.SSL_read break;
	 * case "EVENT_WRITE": //System.out.println(eventType + eDetailStr); break;
	 * 
	 * case "EVENT_CHECK_FILE_ATTRIBUTES": break; case
	 * "EVENT_MODIFY_FILE_ATTRIBUTES": break; case "EVENT_LSEEK":
	 * //System.out.println(eventType + eDetailStr); break; case "EVENT_RENAME":
	 * //System.out.println(eventType + eDetailStr); break; case "EVENT_LINK":
	 * break; case "EVENT_UNLINK": //System.out.println(eventType + eDetailStr);
	 * // for (Value v : enode.getParameters()) { // v.valueBytes(); // } break;
	 * case "EVENT_OPEN": //System.out.println(eventType + eDetailStr); // Posix
	 * open break; case "EVENT_CLOSE": //System.out.println(eventType +
	 * eDetailStr); // Posix close break; case "EVENT_CREAT_OBJECT":
	 * //System.out.println(eventType + eDetailStr); break;
	 * 
	 * case "EVENT_BIND": //System.out.println(suuidHex + "::EVENT_BIND::" +
	 * eventType + "::" + enode.getTimestampNanos() + "::" + apicall + "::" +
	 * printAllEventParamDetails(enode));
	 * 
	 * 
	 * break; case "EVENT_CONNECT": //System.out.println(suuidHex +
	 * "::EVENT_CONNECT::" + eventType + "::" + enode.getTimestampNanos() + "::"
	 * + apicall + "::" + printAllEventParamDetails(enode));
	 * 
	 * break; case "EVENT_RECVFROM": // decide the flow information for
	 * netflowObject
	 * 
	 * //System.out.println(suuidHex + "::EVENT_RECVFROM::" + eventType + "::" +
	 * enode.getTimestampNanos() + "::" + apicall + "::" +
	 * printAllEventParamDetails(enode)); break; case "EVENT_SENDTO":
	 * //addSuspEvent = true;
	 * 
	 * break; case "EVENT_OTHER": //System.out.println(suuidHex +
	 * "::EVENT_OTHER::" + eventType + "::" + enode.getTimestampNanos() + "::" +
	 * apicall + "::" + printAllEventParamDetails(enode));
	 * 
	 * break;
	 * 
	 * }
	 */

	/**
	 * constructor with default settings
	 */
	public KafkaAnalyzer(String analysisType) {
		this("config.properties", "whiteapp.list", analysisType);
	}

	/**
	 * load configuration and init Kafka settings
	 */
	public KafkaAnalyzer(String configFile, String whiteapplist, String anaType) {
		// load settings, if error, exit.
		if (anaType.contains("f") || anaType.contains("k") || anaType.contains("dump")) {
			try {
				String log_random_id = new SimpleDateFormat("MM-dd-HH-mm-ss").format(new Date());
				this.report_dir_path = "/data/result/" + log_random_id;
				File report_dir = new File(this.report_dir_path);
				report_dir.mkdirs();
				File suspEventFile = new File(this.report_dir_path + "/suspEventTrac.txt");
				File suspSubFile = new File(this.report_dir_path + "/suspSubjTrac.txt");
				
				File eventFile = new File(this.report_dir_path + "/EventTrac.txt");
				File subFile = new File(this.report_dir_path + "/SubjTrac.txt");
				File subUUIDFile = new File(this.report_dir_path + "/SubjUUIDDetailTrac.txt");
				File cidGraphFile = new File(this.report_dir_path + "/cidGraph.txt");

				
				try {
					this.eventAllWriter = new PrintWriter(new FileOutputStream(suspEventFile.getAbsolutePath()));
					this.subAllWriter = new PrintWriter(new FileOutputStream(suspSubFile.getAbsolutePath()));
					
					this.subWriter = new PrintWriter(new FileOutputStream(subFile.getAbsolutePath()));
					this.eventWriter = new PrintWriter(new FileOutputStream(eventFile.getAbsolutePath()));
					this.subUUIDDetailWriter = new PrintWriter(new FileOutputStream(subUUIDFile.getAbsolutePath()));
					this.cidGraphWriter = new PrintWriter(new FileOutputStream(cidGraphFile.getAbsolutePath()));



				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				readConfiguration(configFile);
				readWhiteapplist(whiteapplist);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	/**
	 * start analysis from Kafka
	 */
	public void startQuery(String queryType, String graphN, String key) {
		Util.queryFCCE(queryType, graphN, key);
	}

	/**
	 * start analysis from Kafka
	 */
	public void start(String multiT, String recordSize, String uuidpath) {
		Schema schema = null;
		try {
			long startTime = System.nanoTime();

			// service = Executors.newFixedThreadPool(3);

			// String dir = "/home/darpa/riskDroid2/riskdroid2.0/";
			String schemaFilename = "TCCDMDatum.avsc";
			// String dir = "/data/";
			schema = new Schema.Parser().parse(new File(this.schemaFilename));

			try (BufferedReader br = new BufferedReader(new FileReader(uuidpath))) {
				String sCurrentLine;
				while ((sCurrentLine = br.readLine()) != null) {
					KafkaAnalyzer.dumpSUUID += sCurrentLine + "::";
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			/*
			 * consumer = kafka.consumer.Consumer.createJavaConsumerConnector(
			 * createConsumerProps()); //createConsumerProps());
			 * //DatumReader<TCCDMDatum> datumReader = new
			 * SpecificDatumReader<TCCDMDatum>(schema); TopicPartition
			 * partition0 = new TopicPartition(this.readTopic, 0);
			 */

			consumer = new KafkaConsumer(createConsumerProps());
			producer = new Producer(producerServer, groupId, writeTopic);

			TopicPartition partition0 = new TopicPartition(this.readTopic, 0);
			consumer.assign(Arrays.asList(partition0));
			Set<TopicPartition> partitions = consumer.assignment();
			for (TopicPartition pInfo : partitions) {
				consumer.seekToBeginning(pInfo);
			}

			Map<String, Integer> topicCount = new HashMap<>();
			topicCount.put(this.readTopic, 1);

			TCCDMDatum transaction = null;
			boolean multithread = false;
			if (multiT.contains("m"))
				multithread = true;
			double recCNT = 0; // loop every records
			// ExecutorService service = Executors.newFixedThreadPool(4);
			while (!this.exiton0) {
				// Polling...
				ConsumerRecords<String, GenericContainer> records = consumer.poll(1000000);
				// Handle new records
				Iterator recIter = records.iterator();
				// loop every records
				// Object datum = transaction.getDatum();
				// if (multithread) {
				// System.out.println("records.count()" + records.count());
				CountDownLatch latch = new CountDownLatch(7);
				// }
				// long procCNT = 0;
				ArrayList<Object> datumList = new ArrayList<Object>();

				while (recIter.hasNext()) {
					ConsumerRecord<String, GenericContainer> record = (ConsumerRecord<String, GenericContainer>) recIter
							.next();
					transaction = (TCCDMDatum) record.value();
					if (multithread)
						datumList.add(transaction.getDatum());
					else
						processDatum17(transaction.getDatum());
					recCNT += 1;
					if (recCNT % 100000 == 0) {
						System.out.printf("Takes: " + (System.nanoTime() - startTime) / 1000000000
								+ "(s) to process: %.1f records...\n", recCNT); // 32262852498
						if (multithread) {
							ProcessDatum17 r;
							try {
								r = new ProcessDatum17(latch, "Thread:" + recCNT, datumList, KafkaAnalyzer.typecnt);
								r.start(); // Thread.sleep(10000);
								KafkaAnalyzer.mutex.acquire();
								// System.out.println("Cleaning " + recCNT);
								datumList.clear();
								KafkaAnalyzer.mutex.release();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}

					if (recCNT % 500000 == 0) {
						// Util.queryFCCE("queryelembyproperty");
						// Util.queryFCCE("queryelembyuuidB");
						Util.matchSuspEvents();
						Util.reportSuspSubjEvents(this.report_dir_path);
						Util.reportSuspSubj();
						Util.reportTime(startTime, recCNT);
					}
					/*
					 * if (multithread) { if (!datumList.isEmpty()) {
					 * ProcessDatum17 r; try { r = new ProcessDatum17(latch,
					 * "Thread:" + recCNT, datumList, KafkaAnalyzer.typecnt);
					 * r.start(); KafkaAnalyzer.mutex.acquire();
					 * System.out.println("Cleaning " + recCNT);
					 * datumList.clear(); KafkaAnalyzer.mutex.release(); } catch
					 * (InterruptedException e) { e.printStackTrace(); } } } if
					 * (multithread){ try { Thread.sleep(5000); } catch
					 * (InterruptedException e) { e.printStackTrace(); } }
					 */

					if (recCNT == Integer.valueOf(recordSize)) { // less than
																	// 3000000
																	// ||
																	// (records.count()<10)
						for (String type : KafkaAnalyzer.typecnt.keySet()) {
							System.out.println(type + ':' + KafkaAnalyzer.typecnt.get(type).toString());
						}
						String log_random_id_pred = new SimpleDateFormat("MM-dd-HH-mm-ss").format(new Date());
						Util.finalReport(report_dir_path, log_random_id_pred);
						Util.reportTime(startTime, recCNT);
					}
				}

				if (recCNT % 15000000 == 0) {
					// Util.reportTraces(this.report_dir_path);
					KafkaAnalyzer.suspEventStrs.clear();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	private void updateFilePolicyfile(boolean general, String topKey, String regex, String type){
		
		if (general) {
			if (this.filePolicyListGen.containsKey(topKey)){
				this.filePolicyListGen.get(topKey).add(regex + type);
				//System.out.println("addPolicyGen:" + regex + type);
				}
			else {
				ArrayList<String> policyList = new ArrayList<String>();
				policyList.add(regex + type);
				//System.out.println("addPolicyGen:" + regex + type);
				this.filePolicyListGen.put(topKey, policyList);
			}
				
		}
		else {
			if (this.filePolicyList.containsKey(topKey)){
				this.filePolicyList.get(topKey).add(regex + type);
				//System.out.println("addPolicy:" + regex + type);
				}
			else{
				ArrayList<String> policyList = new ArrayList<String>();
				policyList.add(regex + type);
				//System.out.println("addPolicy:" + regex + type);
				this.filePolicyList.put(topKey, policyList);
			}
		}
	}
	
	/*
	 * read cdm data from avro file
	 */
	public void startFromAvroFile(String avroFilePath, String multiT, String uuidpath, boolean dump, String policyFile)
			throws IOException {

		// read reocrds from a avro file
		long startTime = System.nanoTime();
		// String dir = "/home/darpa/riskDroid2/riskdroid2.0/";
		String schemaFilename = "TCCDMDatum.avsc";

		// String dir = "/data/";
		/*
		 * String log_random_id = new
		 * SimpleDateFormat("MM-dd-HH-mm-ss").format(new Date()); String
		 * report_dir_path = "/data/result/" + log_random_id; File report_dir =
		 * new File(report_dir_path); report_dir.mkdirs();
		 */
		// String schemaFilename =
		// "/Users/hhuang/Documents/avro_demo/avro_demo/schema/TCCDMDatum.avsc";
		// String schemaFilename =
		// "/Users/hhuang/Documents/avro_demo/avro_demo/schema/TCCDMDatum15.avsc";
		Schema schema = new Schema.Parser().parse(new File(schemaFilename));

		try (BufferedReader br = new BufferedReader(new FileReader(uuidpath))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				KafkaAnalyzer.dumpSUUID += sCurrentLine + "::";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try (BufferedReader br = new BufferedReader(new FileReader(policyFile))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {			  	
				String[] fds = sCurrentLine.split("\t");
				//if ()
				if (fds.length>1 && fds[0].contains("/")) {
					String regex = fds[0] + ":;:";
					String type ="";
					for (int i = 1; i< fds.length; i++) {
						if (fds[i].contains(":")) {
							type = fds[i];
							break;
						}
					}
					
					String [] subs = regex.split("/");
					String topKey = "";
					if (subs.length>0)
						topKey = subs[1];
					
					boolean generalType = false;
					if (sCurrentLine.contains("#"))  //  /usr/.*	system_u:object_r:usr_t#:s0
						generalType = true;											
					updateFilePolicyfile(generalType, topKey, regex, type);
					
				}
				else 
					System.out.println("not split:" + sCurrentLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {

			DatumReader<TCCDMDatum> datumReader = new SpecificDatumReader<TCCDMDatum>(schema);
			DataFileReader<TCCDMDatum> dataFileReader = new DataFileReader(new File(avroFilePath), datumReader);
			TCCDMDatum transaction = null;
			double recCNT = 0; // loop every records
			boolean multithread = false;

			if (multiT.contains("m"))
				multithread = true;

			// if (multithread) {
			// ExecutorService service = Executors.newFixedThreadPool(7);
			CountDownLatch latch = new CountDownLatch(7);
			// }

			long procCNT = 0;
			ArrayList<Object> datumList = new ArrayList<Object>();
			if (dump) {
				recCNT = 0;
				while (dataFileReader.hasNext()) {
					recCNT += 1;
					transaction = dataFileReader.next(transaction);
					processDatum17Dump(transaction.getDatum());
					
					if (recCNT % 5000 == 0) {
						// Util.queryFCCE("queryelembyproperty");
						// Util.queryFCCE("queryelembyuuidB");
						//Util.matchSuspEvents();
						Util.reportAllSubjEvents(this.report_dir_path);
						//Util.reportSuspSubj();
						//Util.reportTime(startTime, recCNT);
					}
				}				

				/*
				for (Map.Entry<String, String> cidMap : this.cid2UUIDMap.entrySet()) {
					this.subWriter.println(cidMap.getKey() + ',' + cidMap.getValue());
				}
				this.subWriter.flush();
				this.subWriter.close();
				*/
				
				for (Map.Entry<String, String> subuuidMap : this.allSubjectStrs.entrySet()) {
					this.subUUIDDetailWriter.println(subuuidMap.getKey() + ',' + subuuidMap.getValue());
				}
				this.subUUIDDetailWriter.flush();
				this.subUUIDDetailWriter.close();

				
				for (DefaultEdge e : this.forkGraph.edgeSet()) {
					this.cidGraphWriter.println(e.toString());
				}
				this.cidGraphWriter.flush();
				this.cidGraphWriter.close();
				
				this.eventWriter.flush();
				this.eventWriter.close();
				

				
			} else {
				while (dataFileReader.hasNext()) {
					recCNT += 1;
					transaction = dataFileReader.next(transaction);
					// System.out.println("Processed :" + transaction);
					// processDatum17(transaction.getDatum());
					if (multithread)
						datumList.add(transaction.getDatum());
					else
						processDatum17(transaction.getDatum());
					// procCNT += 1;

					/*
					 * String type =
					 * transaction.getDatum().getClass().toString(); if
					 * (!this.typecnt.containsKey((type))) {
					 * this.typecnt.put(type, 1); } else
					 * this.typecnt.replace(type, this.typecnt.get(type) + 1);
					 */
					// r5.close();
					if (recCNT % 100000 == 0) {
						System.out.printf("Takes: " + (System.nanoTime() - startTime) / 1000000000
								+ "(s) to process: %.1f records...\n", recCNT);
						if (multithread) {
							ProcessDatum17 r;
							try {
								r = new ProcessDatum17(latch, "Thread:" + recCNT, datumList, KafkaAnalyzer.typecnt);
								// service.submit(r);
								r.start();
								// Thread.sleep(10000);
								KafkaAnalyzer.mutex.acquire();
								System.out.println("Cleaning " + recCNT);
								datumList.clear();
								KafkaAnalyzer.mutex.release();

							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}

					if (recCNT % 50000 == 0) {
						// String keyType = "queryelembyproperty";
						// String queryk = "EVENT_SENDTO";
						// Util.queryFCCE("queryelembyproperty");
						// Util.queryFCCE("queryelembyuuidB");

						// System.out.println("HERE................");

						Util.matchSuspEvents();
						Util.reportSuspSubjEvents(this.report_dir_path);

						Util.reportSuspSubj();
						Util.reportTime(startTime, recCNT);
						// this will remove the suspEvents
					}
					if (recCNT % 400000 == 0) {
						// Util.reportTraces(this.report_dir_path);
						KafkaAnalyzer.suspEventStrs.clear();
					}
				}

				if (multithread) {
					if (!datumList.isEmpty()) {
						ProcessDatum17 r;
						try {
							r = new ProcessDatum17(latch, "Thread:" + recCNT, datumList, KafkaAnalyzer.typecnt);
							// service.submit(r);
							// r = new ProcessDatum17("Thread" + recCNT,
							// datumList,
							// KafkaAnalyzer.typecnt);
							r.start();

							KafkaAnalyzer.mutex.acquire();
							System.out.println("Cleaning " + recCNT);
							datumList.clear();
							KafkaAnalyzer.mutex.release();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

				if (multithread) {
					try {
						Thread.sleep(15000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				for (String type : KafkaAnalyzer.typecnt.keySet()) {
					System.out.println(type + ':' + KafkaAnalyzer.typecnt.get(type).toString());
				}

				String log_random_id_pred = new SimpleDateFormat("MM-dd-HH-mm-ss").format(new Date());
				Util.finalReport(report_dir_path, log_random_id_pred);
				Util.reportTime(startTime, recCNT);
				System.exit(0);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FontFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// System.out.println("Processed " + count + " CDM records.");
		}

	}

}
