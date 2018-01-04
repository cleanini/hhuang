package ibm.cs;

import java.awt.FontFormatException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.jgrapht.graph.DefaultEdge;

import com.bbn.tc.schema.avro.cdm18.Event;
import com.bbn.tc.schema.avro.cdm18.TagRunLengthTuple;
import com.bbn.tc.schema.avro.cdm18.UUID;
import com.bbn.tc.schema.avro.cdm18.Value;
import com.bbn.tc.schema.avro.cdm18.ValueDataType;
import java.io.BufferedWriter;

public class Util {
	
	  public static class RelationshipEdge<V> extends DefaultEdge {
	        private V v1;
	        private V v2;
	        private String label;

	        public RelationshipEdge(V v1, V v2, String label) {
	            this.v1 = v1;
	            this.v2 = v2;
	            this.label = label;
	        }

	        public V getV1() {
	            return v1;
	        }

	        public V getV2() {
	            return v2;
	        }

	        public String toString() {
	            return label;
	        }
	    }

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static String printAllEventParamDetails(ArrayList<Value> params, String suuidHex) throws FontFormatException {

		HashSet<String> matchedSubName = new HashSet<String>();
		// List<Value> params = enode.getParameters();
		String eventParamValue = "";
		boolean potentialSuspEvent = false;
		if (!params.isEmpty()) {
			eventParamValue = "eventParam::";
			try {
				for (Value v : params) {
					String paraString = "";

					if (!v.getIsNull()) {// && v.getSize() > 0
						ValueDataType dataType = v.getValueDataType();

						// parms.get(j).getValueBytes().
						ByteBuffer data;

						data = (ByteBuffer) v.getValueBytes();
						switch (dataType) {
						case VALUE_DATA_TYPE_INT:
							// System.out.print("INT: ");
							while (data.hasRemaining()) {
								// System.out.print(data.getInt());
								paraString += data.getInt();
							}
							paraString += ":INT";
							if (paraString.length() > 15)
								potentialSuspEvent = true;
							// System.out.println(paraString);
							break;

						case VALUE_DATA_TYPE_CHAR:
							// paraString += ("CHAR: ");
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
								// System.out.println("CHAR:" + paraString);
								// //([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.
								if (paraString.matches("([0-9a-fA-F][0-9a-fA-F]:){5}([0-9a-fA-F][0-9a-fA-F])")
										|| paraString.matches(
												"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9].){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9])")
										|| Util.matchSuspStr(paraString, KafkaAnalyzer.SUSPPARA)) {
									KafkaAnalyzer.eventAllWriter.println("Susp-Match mac:" + paraString);
									String oldStr = ""; 
									if (KafkaAnalyzer.subjectSuspStrMap.containsKey(suuidHex)) {
										oldStr = KafkaAnalyzer.subjectSuspStrMap.get(suuidHex);
									}
									if (!oldStr.contains(paraString))
										KafkaAnalyzer.subjectSuspStrMap.put(suuidHex, oldStr+":;:"+paraString);
									
									potentialSuspEvent = true;
								}
								if (paraString.contains("/lib/arm")) {
									String[] words = paraString.split("/");
									String appname = "";
									// System.out.println("processName:" +
									// paraString);

									for (int i = 0; i < words.length; i++) {
										if (words[i].contains("lib") && i < words.length - 1
												&& words[i + 1].contains("arm")) {
											
											//KafkaAnalyzer.hasLibArmSubjectNames.add(paraString);
											//KafkaAnalyzer.eventAllWriter.println("Susp-hasLibArm:" + paraString);

											appname = words[i - 1];
										}
									}
									// synchronized
									// (KafkaAnalyzer.mutexAllSubjectStrs) {
									for (String suuid : KafkaAnalyzer.allSubjectStrs.keySet()) {
										if (!KafkaAnalyzer.hasLibArmSubject.contains(suuid) && appname != ""
												&& KafkaAnalyzer.allSubjectStrs.get(suuid)
														.contains(appname.split("-")[0]))
											KafkaAnalyzer.hasLibArmSubject.add(suuid);
											String oldLibs = ""; 
											if (KafkaAnalyzer.hasLibArmSubjectNamesMap.containsKey(suuid)) {
												oldLibs = KafkaAnalyzer.hasLibArmSubjectNamesMap.get(suuid);
												//KafkaAnalyzer.hasLibArmSubjectNamesMap.put(suuid, oldLibs+"::"+appname);
											}
											if (!oldLibs.contains(appname))
												KafkaAnalyzer.hasLibArmSubjectNamesMap.put(suuid, oldLibs+":;:"+appname);											
											//KafkaAnalyzer.eventAllWriter.println("Susp-hasLibArmUUID:" + suuid);
									}
									// }
								}
								// System.out.println("BEFOREprocessName" +
								// paraString);
								if (KafkaAnalyzer.processName.contains(paraString)
										&& !matchedSubName.contains(paraString)) {
									matchedSubName.add(paraString);
									// System.out.println("processName" +
									// paraString);

								}
								paraString += (":CHAR");

							} catch (UnsupportedEncodingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;

						case VALUE_DATA_TYPE_BOOL:
							paraString += "BOOL: ";
							// System.out.println(paraString);
							break;

						case VALUE_DATA_TYPE_BYTE:
							while (data.hasRemaining() && paraString.length() < 3000) {// process
																						// effiicency
								paraString += data.get();
							}
							paraString += ":BYTE";
							// if (paraString.length()>500)
							// this.potentialSuspEvent = true;
							// System.out.println(paraString.length() +' '+
							// paraString);
							break;

						case VALUE_DATA_TYPE_DOUBLE:
							while (data.hasRemaining()) {
								paraString += (data.getDouble());
							}
							potentialSuspEvent = true;
							paraString += (":DOUBLE");

							break;

						case VALUE_DATA_TYPE_FLOAT:
							while (data.hasRemaining()) {
								paraString += (data.getFloat());

							}
							// this.potentialSuspEvent = true;
							paraString += (":FLOAT");
							break;

						case VALUE_DATA_TYPE_LONG:
							while (data.hasRemaining()) { // data.hasRemaining()
								paraString += (data.getLong());
							}
							paraString += (":LONG");
							// System.out.println(paraString);
							break;

						case VALUE_DATA_TYPE_SHORT:
							while (data.hasRemaining()) {
								paraString += data.getShort();
							}
							paraString += (":SHORT");

							break;

						case VALUE_DATA_TYPE_COMPLEX: // not properly handled
														// yet
							// v.getRuntimeDataType()
							if (data != null) {
								while (data.hasRemaining()) {
									paraString += data.get();
								}
							}
							// System.out.println(":COMPLEX " + data);
							paraString += (":COMPLEX");

							break;

						default:
							System.out.println("NO handled dataType" + dataType);
							break;
						}
					}
					// String type = printEventParamDetails(v);
					eventParamValue = eventParamValue + ' ' + paraString + ' ';
				}

				boolean suspBinderName = false;
				String tempNames = "";
				if (matchedSubName.size() == 2) {
					Iterator<String> nameItr = matchedSubName.iterator();
					while (nameItr.hasNext()) {
						String subname = nameItr.next();
						// System.out.println("BEFORE:subname:" + subname);
						if (!suspBinderName) {
							for (String suuid : KafkaAnalyzer.suspSubjectStrs.keySet()) { // suspBinder BinderIPC																															
								if (KafkaAnalyzer.suspSubjectStrs.get(suuid).contains(subname)) 										
									suspBinderName = true;
							}
							for (String suuid : KafkaAnalyzer.cnnctSubjectStrs.keySet()) {
								if (KafkaAnalyzer.cnnctSubjectStrs.get(suuid).contains(subname)) 										
									suspBinderName = true;
							}
							
						}
						if (!tempNames.contains(subname))
							tempNames = tempNames + subname + "::";
						// System.out.println("AFTER:subname:" + subname);

					}
					eventParamValue = eventParamValue + ' ' + tempNames + "allNormalBinderIPC"; // recall call normal Binders
				}
				if (suspBinderName && matchedSubName.size() == 2) { // process susp BinderCall
					String uuidpair = "";
					// System.out.println("tempname:" + tempNames);
					String[] names = tempNames.split("::");
					for (String nm : names) {
						// System.out.println("BEFORE:uuidpair:" + uuidpair);
						for (String suuid : KafkaAnalyzer.allSubjectStrs.keySet()) {
							if (KafkaAnalyzer.allSubjectStrs.get(suuid).contains(nm) && !uuidpair.contains(suuid)) {
								// System.out.println("BEFORE:uuidpair:" +
								// uuidpair);
								uuidpair = uuidpair + suuid + "::";
							}
						}
					}
					if (!KafkaAnalyzer.allSuspBinderSubject.containsKey(uuidpair)) {
						// System.out.println("AFTER:uuidpair:" + uuidpair);
						KafkaAnalyzer.allSuspBinderSubject.put(uuidpair, tempNames);
					}					
				}

				if (potentialSuspEvent)
					eventParamValue = eventParamValue + ' ' + "potentialSuspEvent";

			} catch (BufferUnderflowException bue) {
				throw new FontFormatException(bue.toString());
			} catch (IllegalArgumentException i) {
				System.out.println("IllegalArgumentException:" + eventParamValue);
			} catch (NullPointerException i) {
				System.out.println("NullPointerException:" + eventParamValue + ' ' + params);
				i.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return eventParamValue;
	}

	public static String assemblyEventStr(String predObjectHex, String suuidHex, String euuidHex, String apicall, String eventType, 
			Long eventTime, Long eSeq, Integer eTid, CharSequence ppt) {
		return predObjectHex + ":;:" + suuidHex + ":;:" + eventType + ":;:" + euuidHex + ":;:" + apicall + ":;:"
				+ eSeq.toString() + ":;:" + eventTime.toString() + ":;:" + eTid + ":;:"
				+ ppt // + ":;:" +
				+ ":;:";
	}

	public static void printProvGraph(PrintWriter report_writer, PrintWriter provObjReportWriter) {
		if (KafkaAnalyzer.provGraph.edgeSet() != null) {
			for (DefaultEdge e : KafkaAnalyzer.provGraph.edgeSet()) {
				String[] node = e.toString().split(" : ");
				String nd0 = node[0].substring(1, node[0].length());
				String nd1 = node[1].substring(0, node[0].length() - 1);
				// System.out.println("Reporting Provs edge::" + e);
				if (provObjReportWriter != null) {
					report_writer.print("ProvsEdge::" + "(" + nd1 + " : " +  nd0 +  ")\n");
					report_writer.println("ProvsNode::" + nd0);
					report_writer.println("ProvsNode::" + nd1);

					
					if (KafkaAnalyzer.provUUID2ObjUUID.containsKey(nd0))
						provObjReportWriter.println("ProvsNode::" + KafkaAnalyzer.provUUID2ObjUUID.get(nd0));
					else 
						System.out.println("provUUID2ObjUUID: No" + nd0);
					
					if (KafkaAnalyzer.provUUID2ObjUUID.containsKey(nd1))
						provObjReportWriter.println("ProvsNode::" + KafkaAnalyzer.provUUID2ObjUUID.get(nd1));
					else 
						System.out.println("provUUID2ObjUUID: No" + nd1);
					
										
					if (KafkaAnalyzer.provUUID2ObjUUID.containsKey(nd0) && KafkaAnalyzer.provUUID2ObjUUID.containsKey(nd1))
						provObjReportWriter.println("ProvsEdge::(" + KafkaAnalyzer.provUUID2ObjUUID.get(nd1) +  " : " +  KafkaAnalyzer.provUUID2ObjUUID.get(nd0) + ")");
					
				} else
					report_writer.print("ProvsEdge::" + "(" + nd1 + " : " +  nd0 +  ")\n");
			}
			
			for (String v : KafkaAnalyzer.provGraph.vertexSet()) {
				report_writer.println("ProvsNode::" + v);				
			}
			
			for (String v : KafkaAnalyzer.provGraph.vertexSet()) {
				provObjReportWriter.println("ProvsNode::" + KafkaAnalyzer.provUUID2ObjUUID.get(v));				
			}
			
			provObjReportWriter.flush();
			provObjReportWriter.close();
			report_writer.flush();
			report_writer.close();
			
		}
		/*
		if (KafkaAnalyzer.provGraph.vertexSet() != null) {
			for (String v : KafkaAnalyzer.provGraph.vertexSet()) {
				// System.out.println("Reporting Provs edge::" + e);
				if (report_writer != null)
					report_writer.print("ProvsNode::" + v + "\n");
				else
					System.out.println("ProvsNode::" + v);
			}
		}*/
		// System.out.println("Reporting Provs edge Total Number:: " +
		// this.provGraph.edgeSet().size() + "\n");
	}

	public static void printSuspProvGraph(PrintWriter suspProvReportWriter, PrintWriter suspObjReportWriter) {
		/*
		 * if (this.provGraph.edgeSet() != null) { for (DefaultEdge e :
		 * this.provGraph.edgeSet()) { //
		 * System.out.println("Reporting Provs edge::" + e); if (report_writer
		 * != null) report_writer.print("ProvsEdge::" + e + "\n"); else{
		 * System.out.println("ProvsEdge::"+e); } } }
		 */
		//System.out.println("Reporting Provs EDGE::");
		//if (KafkaAnalyzer.provGraph.vertexSet().) {
			for (String v : KafkaAnalyzer.provGraph.vertexSet()) {
				if (suspProvReportWriter != null) {
					if (KafkaAnalyzer.suspSinkTags.contains(v) || KafkaAnalyzer.suspSrcTags.contains(v)
							|| KafkaAnalyzer.suspCtrlTags.contains(v))
					{
						suspProvReportWriter.println("ProvsNode::" + v);
						if (KafkaAnalyzer.provUUID2ObjUUID.containsKey(v))
							suspObjReportWriter.println("ProvsNode::" + KafkaAnalyzer.provUUID2ObjUUID.get(v));
						
						Set<DefaultEdge> edges = KafkaAnalyzer.provGraph.edgesOf(v);
						for (DefaultEdge e : edges) {
							String[] node = e.toString().split(" : ");
							String nd0 = node[0].substring(1, node[0].length());
							String nd1 = node[1].substring(0, node[0].length() - 1);
							// MATCH (n:provTag) where
							// n.provTagID='00000000000000000000000000007c20'
							// RETURN n					
							suspProvReportWriter.println("ProvsEdge::" + "(" + nd1 + " : " +  nd0 +  ")\n");
							

							if (!nd0.contains(v)) {
								suspProvReportWriter.println("ProvsNode::" + nd0); // src
								if (KafkaAnalyzer.provUUID2ObjUUID.containsKey(nd0))
									suspObjReportWriter.println("ProvsNode::" + KafkaAnalyzer.provUUID2ObjUUID.get(nd0)); 
							}
							if (!nd1.contains(v)) {
								suspProvReportWriter.println("ProvsNode::" + nd1); // sink
								if (KafkaAnalyzer.provUUID2ObjUUID.containsKey(nd1))
									suspObjReportWriter.println("ProvsNode::" + KafkaAnalyzer.provUUID2ObjUUID.get(nd1));
							}
							if (KafkaAnalyzer.provUUID2ObjUUID.containsKey(nd0) && KafkaAnalyzer.provUUID2ObjUUID.containsKey(nd1))
								suspObjReportWriter.println("ProvsEdge::(" + KafkaAnalyzer.provUUID2ObjUUID.get(nd1) +  " : " +  KafkaAnalyzer.provUUID2ObjUUID.get(nd0) + ")");
							// + ' ' +
							// e.toString().split(":")[1].split(")")[0]);
						}
					}
				} else {
					if (KafkaAnalyzer.suspSinkTags.contains(v) || KafkaAnalyzer.suspSrcTags.contains(v)
							|| KafkaAnalyzer.suspCtrlTags.contains(v)) {
						System.out.println("ProvsNode::" + v);
						Set<DefaultEdge> edges = KafkaAnalyzer.provGraph.edgesOf(v);
						for (DefaultEdge e : edges) {
							// MATCH (n:provTag) where
							// n.provTagID='00000000000000000000000000007c20'
							// RETURN n
							System.out.println("ProvsEdge::" + e);
							String[] node = e.toString().split(" : ");
							String nd0 = node[0].substring(1, node[0].length());
							String nd1 = node[1].substring(0, node[0].length() - 1);

							if (!nd0.contains(v))
								System.out.println("ProvsNode::" + nd0);
							if (!nd1.contains(v))
								System.out.println("ProvsNode::" + nd1);
							// + ' ' +
							// e.toString().split(":")[1].split(")")[0]);
						}
					}
				}
			}
			//if (suspProvReportWriter != null) {
			suspObjReportWriter.flush();
			suspObjReportWriter.close();
			
			suspProvReportWriter.flush();
			suspProvReportWriter.close();
			//}
		//}
		// System.out.println("Reporting Provs edge Total Number:: " +
		// this.provGraph.edgeSet().size() + "\n");
	}

	public static void addEventInOutProvsDetails(ArrayList<Value> paraList, String eventType) {
		// UUID eUUID = event.getUuid();
		// String eventUUID = Util.convertPrintCUUID(eUUID, event, "Event",
		// false);
		try {

			// String EventIOtype = "";
			// this.suspEventNodes.containsKey(eventUUID))
			for (Value v : paraList) {
				// event.toString() + " REPORT ParamDetail::"
				// + printEventParamDetails(v));
				if (!v.getIsNull()) {//&& v.getSize() > 0
					if (v.getTag() != null) {
						// valueTypes += v.getType().toString() + ' ' +
						// v.getTag() + ' ';

						for (TagRunLengthTuple tag : v.getTag()) {
							// if (tag.getNumValueElements() != null &&
							// tag.getTagId() != null &&
							// tag.getNumValueElements() != -1) {//
							// tag.getNumValueElements()
							String tagID = Util.getHexUUIDStr(tag.getTagId());
							if (!tagID.contains("00000000000000000000000000000000") && v.getType() != null) {
								switch (v.getType()) {
								case VALUE_TYPE_CONTROL:
									KafkaAnalyzer.suspCtrlTags.add(tagID);
									break;
								case VALUE_TYPE_SRC:
									KafkaAnalyzer.suspSrcTags.add(tagID);
									break;
								case VALUE_TYPE_SINK:
									KafkaAnalyzer.suspSinkTags.add(tagID);
									break;
								default:
									break;
								}
							}
						}
					}
				}
			}
		} catch (NullPointerException e) {
			System.out.println("NullPointer:" + paraList.toString());
			e.printStackTrace();
		}
	}

	public static String getHexUUIDStr(UUID uuid) {
		CUUID cuuid = new CUUID(uuid);
		return cuuid.getHexUUID();
	}

	public static void checkDup(UUID id, HashMap<UUID, Integer> cntUUID) {
		if (!cntUUID.containsKey(id))
			cntUUID.put(id, 1);
		else
			cntUUID.put(id, cntUUID.get(id) + 1);
	}

	public static boolean matchSuspStr(String matchStr, Set<String> set) {
		for (String i : set) {
			if (matchStr.contains(i))
				return true;
		}
		return false;
	}

	public static String convertPrintCUUID(UUID uuid, Object convertedRecord, String className, boolean print) {
		CUUID cuuid = new CUUID(uuid);
		String detailStr = className + "::" + cuuid.getHexUUID() + "::" + convertedRecord.toString();
		// print = false;
		if (print)
			System.out.println("new " + detailStr);
		return detailStr;
	}

	public static void updateSubScoreVector(Map<String, Integer> subjectScoreVector, String suuidHex, Integer weight) {
		if (subjectScoreVector.containsKey(suuidHex))
			subjectScoreVector.replace(suuidHex, subjectScoreVector.get(suuidHex) | weight);
		else
			subjectScoreVector.put(suuidHex, weight);
	}

	public static void queryFCCE(String type, String graphN, String queryk){							
		//long startFCCETime = System.nanoTime();
		
		String graphName = graphN;//"cs20170221";	
		//String queryk = "";
		
		String keyType = "";
		
		switch (type){ 
			case "queryelembyuuidB":
			case "queryelembyuuid":
				 keyType = "queryelembyuuid";					
			case "queryelembyproperty": 
				// GET /queryelembyproperty/cadets/type/EVENT_CONNECT

				 keyType = type;
				 //queryk = "EVENT_SENDTO";
			break;
				 
			case "querypropbytype": 
				//  GET /querypropbytype/cadets/_filename?where=value=~%2F.so.%2F HTTP/1.0
				keyType = type;
				break;
			
			case "queryeventbytime":
				keyType = type;
				break;
			case "queryeventbyentity": 
				keyType = type;
				break;
			default:
				System.out.println("NO handled queryType " + keyType);
				break;
			
			
				 /*
				 Iterator<String> itrStr = KafkaAnalyzer.suspEventSet.iterator();
					if (itrStr.hasNext()) queryk = itrStr.next();
					int cnt = 0;
					while(itrStr.hasNext() && cnt<50) {
						queryk += "," + itrStr.next();
						cnt+=1;
					}*/
					
		}
		
		//System.out.println("HERE........queryk" + queryk);
		talkFCCERestAPI r = new talkFCCERestAPI(keyType, queryk, 
				graphName, "TESTFCCE", System.nanoTime());
		r.start();
		//KafkaAnalyzer.service.submit(r);		
	} 
	
	public static void matchSuspEvents() {
		// matched suspSS suspNetFlow, suspFile
		synchronized (KafkaAnalyzer.mutexsuspEventStrs) {
			for (String e : KafkaAnalyzer.suspEventStrs.keySet()) {
				String[] fds = KafkaAnalyzer.suspEventStrs.get(e).split(":;:");
				if (fds.length > 0 && fds[0].contains(";;")) {
					String[] preds = fds[0].split(";;");
					for (String pred : preds) {
						// System.out.println(pred);
						if (KafkaAnalyzer.sensitiveNetflow.containsKey(pred)) {
							KafkaAnalyzer.eventAllWriter.println("Susp-NetFlowMatched:" + pred + ":;:FlowObj " + KafkaAnalyzer.sensitiveNetflow.get(pred) + 
									":;:Event " + KafkaAnalyzer.suspEventStrs.get(e));
						}
						if (KafkaAnalyzer.sensitiveSrcSink.containsKey(pred)) {
							KafkaAnalyzer.eventAllWriter.println(
									"Susp-SrcSinkMatched:" + pred + ":;:SrcSinkObj " + KafkaAnalyzer.sensitiveSrcSink.get(pred)
											+ ":;:Event " + KafkaAnalyzer.suspEventStrs.get(e));
						}
						
						if (KafkaAnalyzer.sensitiveFile.containsKey(pred)) {
							KafkaAnalyzer.eventAllWriter.println("Susp-FileObjectMatched:" + pred + ":;:FileObj "
									+ KafkaAnalyzer.sensitiveFile.get(pred) + ":;:Event " + KafkaAnalyzer.suspEventStrs.get(e));
						}
						//matched-susp-File:6d67d78ef131e7ee9e33512bd855b985 
						//FileObject::6d67d78ef131e7ee9e33512bd855b985::
						//{"uuid": [109, 103, -41, -114, -15, 49, -25, -18, -98, 51, 81, 43, -40, 85, -71, -123], "baseObject": {"permission": [1, -92], "epoch": null, "properties": {"path": "/data/app/com.adups.fota.sysoper-1/base.apk"}}, "type": "FILE_OBJECT_FILE", "fileDescriptor": null, "localPrincipal": null, "size": null, "peInfo": null, "hashes": null} 
						//predObject;;6d67d78ef131e7ee9e33512bd855b985;;/data/app/com.adups.fota.sysoper-1/base.apk;;:;:0000000000000000000000000000d6d9:;:EVENT_READ:;:int libcore.io.Posix.read(java.io.FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) [line: 459]:;:1488040601944000000:;:8404:;:352:;:long com.msg.analytics.b.a(java.lang.String) [line: -1]:;:
						
					}
				}
			}
		}
	}

	public static void reportSuspSubj() {
		// matched suspicious process with Behavior Vector

		// listSuspVector.sort((left, right) -> left - right);

		KafkaAnalyzer.subAllWriter.println("====================Report suspcious process with behavior vector=======================\n");

		Map<String, Integer> sortedSubjVec = Util.sortByValue(KafkaAnalyzer.subjectScoreVector);

		for (String suuidHex : sortedSubjVec.keySet()) {
			Integer scoreVec = sortedSubjVec.get(suuidHex);
			if (scoreVec != null) {
				if (scoreVec.intValue() > 0) { // has file or network activities
					String vectorRep = "Process:" + suuidHex + " suspcious behavior vector:["
							+ Integer.toBinaryString(scoreVec.intValue()) + ']';
					System.out.println("Process:" + suuidHex + " suspcious behavior vector:["
							+ Integer.toBinaryString(scoreVec.intValue()) + ']');
					if (KafkaAnalyzer.suspSubjectStrs.containsKey(suuidHex)) {
						KafkaAnalyzer.suspSubUUID.add(suuidHex);
						KafkaAnalyzer.subAllWriter.println(vectorRep + " Untrusted application process MATCHED:"
								+ KafkaAnalyzer.suspSubjectStrs.get(suuidHex).split("cmdLine")[1] + '\n');
						System.out.println(vectorRep + " Untrusted application process MATCHED:"
								+ KafkaAnalyzer.suspSubjectStrs.get(suuidHex).split("cmdLine")[1] + '\n');
					} else if (KafkaAnalyzer.cnnctSubjectStrs.containsKey(suuidHex)){
						KafkaAnalyzer.subAllWriter.println(vectorRep + " Trusted connect application process REPORTED:"
								+ KafkaAnalyzer.cnnctSubjectStrs.get(suuidHex).split("cmdLine")[1] + '\n');
						System.out.println(vectorRep + " Trusted connect application process REPORTED:"
								+ KafkaAnalyzer.cnnctSubjectStrs.get(suuidHex).split("cmdLine")[1] + '\n');
					} else if (KafkaAnalyzer.whtSubjectStrs.containsKey(suuidHex)){
						KafkaAnalyzer.subAllWriter.println(vectorRep + " Matched trusted apps"
						+ KafkaAnalyzer.whtSubjectStrs.get(suuidHex).split("cmdLine")[1] + '\n');// this.whtSubjectStrs.get(suuidHex).split("cmdLine")[1]
						// suspSubUUID.add(suuidHex);						
					} else 
						System.out.println(suuidHex + " No suspcious process matched" + '\n');
				}
			}
		}
		KafkaAnalyzer.subAllWriter.println(
				"Process suspcious behavior vector category:[wNetwork, wFILE, rNetwork, rFILE, preEXFIL, Location, ContentDB, WIFI]\n");
		KafkaAnalyzer.subAllWriter.flush();
	}

	
	public static void reportAllSubjEvents(String dir) {
		System.out
				.println("====================Report ALL process Events=======================\n");
		//Map<String, Integer> sortedSubjVec = Util.sortByValue(KafkaAnalyzer.subjectScoreVector);
		
		for (Map.Entry<String, String> cidMap : KafkaAnalyzer.cid2UUIDMap.entrySet()) {
			//System.out.println(cidMap.getKey() + ',' + );
			String [] suuids = cidMap.getValue().split(";");
			
			for (String suuid : suuids) {			
				File eventTracReport = new File(
					dir + '/' + cidMap.getKey() + '/' + cidMap.getKey() + '-' + suuid + ".txt");
				try {
				// System.out.println("created FILE\n");
					KafkaAnalyzer.eventTracWriter = 
							new PrintWriter(new BufferedWriter(new FileWriter(eventTracReport.getAbsolutePath(), true)));
					List<String> sList = KafkaAnalyzer.allSubjEventUUIDs.get(suuid);
					
					ArrayList<String> eventStringList = new ArrayList<String>();
					Optional.ofNullable(sList).ifPresent(eventStringList::addAll);
					reportRankedTracesAll(KafkaAnalyzer.eventTracWriter, suuid, eventStringList);
					//KafkaAnalyzer.eventTracWriter.close();
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
				    System.err.println(e);
				}
							
		}}
		
		//ExecutorService service = Executors.newFixedThreadPool(4);
		/*
		for (String suuidHex : sortedSubjVec.keySet()) {
			Integer scoreVec = sortedSubjVec.get(suuidHex);
			if (scoreVec != null) {
				if (scoreVec.intValue() > 0) { // has file or network activities
					if (KafkaAnalyzer.suspSubjEventUUIDs.containsKey(suuidHex) && (KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex).size()>5)) { //scoreVec.intValue() > 16 ||
					
						System.out.println(
								"Number of Events:" + suuidHex + ' '+ KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex).size() + '\n');																
						try {
							//sortTraces(CountDownLatch lt, String name, ArrayList<String> eventList, String suuidHexStr, String dir) {
							KafkaAnalyzer.mutex.acquire();
							sortTracesMulti r;
							List<String> sList = KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex);
							r = new sortTracesMulti("Thread:" + suuidHex, sList, suuidHex, dir);
							KafkaAnalyzer.mutex.release();
							KafkaAnalyzer.service.submit(r);
	
						} catch (InterruptedException e) {
							e.printStackTrace();
						}												
						KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex).clear();						
					}
				}
			}
		}*/
	}
		
		
	public static void reportSuspSubjEvents(String dir) {
		System.out
				.println("====================Report suspcious process Events=======================\n");

		Map<String, Integer> sortedSubjVec = Util.sortByValue(KafkaAnalyzer.subjectScoreVector);
		//ExecutorService service = Executors.newFixedThreadPool(4);
		for (String suuidHex : sortedSubjVec.keySet()) {
			Integer scoreVec = sortedSubjVec.get(suuidHex);
			if (scoreVec != null) {
				if (scoreVec.intValue() > 0) { // has file or network activities
					//String suspSub = itrsub.next();
					//HashMap<String, String> suspProcHash = new HashMap<String, String>();
					//List<String> list = new ArrayList<String>();
					if (KafkaAnalyzer.suspSubjEventUUIDs.containsKey(suuidHex) && (KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex).size()>5)) { //scoreVec.intValue() > 16 ||
					
						System.out.println(
								"Number of Events:" + suuidHex + ' '+ KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex).size() + '\n');																
						try {
							//sortTraces(CountDownLatch lt, String name, ArrayList<String> eventList, String suuidHexStr, String dir) {
							KafkaAnalyzer.mutex.acquire();
							sortTracesMulti r;
							List<String> sList = KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex);
							r = new sortTracesMulti("Thread:" + suuidHex, sList, suuidHex, dir);
							KafkaAnalyzer.mutex.release();
							//r.start();
							KafkaAnalyzer.service.submit(r);
							//KafkaAnalyzer.service.submit(r);
						
							// service.submit(r);
							// Thread.sleep(10000);
							//System.out.println("Cleaning ");
							//datumList.clear();

						} catch (InterruptedException e) {
							e.printStackTrace();
						}												
						KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex).clear();						
					}
				}
			}
		}
		/*
		try {
			service.awaitTermination(30000, MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	public static void reportTime(long startTime, double recCNT) {
		System.out.println("======================Report Processed Record and Time============================\n");
		long endTime = System.nanoTime();
		long timediff = (endTime - startTime) / 1000000;
		System.out.printf("Used Total:" + timediff + "(ms) to process: %.1f records." + " Avg Speed(rec/sec):"
				+ (recCNT / timediff) * 1000 + '\n', recCNT); // 32262852498 ns
																// print
																// 31617801599
	}

	public static void reportRankedTracesAll(PrintWriter eventTracWriterSub, String suuidHex, ArrayList<String> eventStringList) { // multithreads

			Iterator<String> eventItr = eventStringList.iterator();
			List<String> eventList = new ArrayList<String>();
			
			HashMap<String, String> suspProcHash = new HashMap<String, String>();
			HashMap<String, String> suspEventHash = new HashMap<String, String>();

			String eventStr = "";
			while (eventItr.hasNext()) {
			   eventStr = eventItr.next();
			  //System.out.println(eventStr);
			 //eventTracWriterSub.append(eventStr+"\n");
			 //if (eventStr.contains(suspSub)) {
			  
			  String[] eventfds = eventStr.split(",");
			  if (eventfds.length > 4) {
				suspProcHash.put(eventfds[4], eventStr);
				suspEventHash.put(eventfds[4], eventfds[1]);
				//System.out.println(eventfds[4] + ' ' + eventfds[4] + ' ' + eventfds[0]);				
				eventList.add(eventfds[4]);
			  }
				// KafkaAnalyzer.suspEventStrs.remove(euuidHex);
			}
			
			//System.out.println("Seq#, InvokedTimes, suspAPICall, provenanceUUIDSorting");
			
			eventList.sort((left, right) -> Integer.parseInt(left) - Integer.parseInt(right));
			//System.out.println("Seq#, InvokedTimes, suspAPICall, provenanceUUID === Sorted");
			// sort it
			String prev = "";
			String next = "";
			String preSeq = "1400000000000,EVENT_SEPARATE;;noUUID1;noUUID2,0,0,0,0";
			//eventTracWriterSub.print("Seq#, InvokedTimes, suspAPICall, provenanceUUID"); // suspSub.substring(25,
			//System.out.println("Seq#, InvokedTimes, suspAPICall, provenanceUUID"); 
			/*
			for (String seq : eventList) {
				eventTracWriterSub.print(seq); // suspSub.substring(25,
			}*/
			
			int cnt = 1;
			for (String seq: eventList) {
				next = suspEventHash.get(seq);

				if (!prev.contains(next)){
					//System.out.println(preSeq);
					eventTracWriterSub.print(preSeq + ',' +  cnt + '\n');
					cnt = 1;
				
					prev = next;
					preSeq = suspProcHash.get(seq);
				}
				else 
					cnt += 1;				
			}
			eventTracWriterSub.flush();
			eventTracWriterSub.close();
			
			
			if (KafkaAnalyzer.allSubjEventUUIDs.containsKey(suuidHex))
				KafkaAnalyzer.allSubjEventUUIDs.get(suuidHex).clear(); // only report periodically
			
			
			/*
			int cnt = 1;
			for (String seq : eventList) {
				next = suspProcHash.get(seq);
				if (!prev.contains(next)) {
					if (prev != "") {
						String apicall = prev.split("\\(")[0];
						String[] fds = prev.split("\\)");
						String preduuid = "";
						if (fds.length > 1)
							preduuid = fds[1];
						String[] preds = preduuid.split(";;");
						String predsPrint = ";;";
						for (int i = 1; i < preds.length; i++) {
							predsPrint += ',' + preds[i];
						}
						if (preds.length > 1)
							eventTracWriterSub.println(preSeq + ":  invoked " + cnt + " times:   " + apicall + " " + predsPrint);
						else 
							eventTracWriterSub.println(preSeq + ":  invoked " + cnt + " times:   " + apicall + " NoPredPath");
					} // suspSub.substring(25, suspSub.length()) + ':'
					prev = next;
					preSeq = seq;
					cnt = 1;
				} else
					cnt += 1;
			}

			String apicall = prev.split("\\(")[0];
			String[] preduuids = prev.split("\\)");
			String preduuid = "";
			if (preduuids.length > 1)
				preduuid = preduuids[1];
			eventTracWriterSub.println(preSeq + ":  invoked " + cnt + " times:   " + apicall + ", " + preduuid);
			*/
		
		
		
		 
	}
	
	
	
	public static void reportRankedTraces(PrintWriter eventTracWriterSub, String suuidHex, ArrayList<String> eventStringList) { // multithreads
		System.out.println("======================Report Sorted Suspicious Traces " + suuidHex + "\n");
		//Iterator<String> eventItr = KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex).iterator();
		Iterator<String> eventItr = eventStringList.iterator();
			HashMap<String, String> suspProcHash = new HashMap<String, String>();
			List<String> eventList = new ArrayList<String>();

			while (eventItr.hasNext()) {
			  String eventStr = eventItr.next();
			 
			  eventTracWriterSub.print(eventStr+"\n");
			 //if (eventStr.contains(suspSub)) {
			  String[] eventfds = eventStr.split(":;:");
			  if (eventfds.length > 4) {
				suspProcHash.put(eventfds[5], eventfds[4] + ' ' + eventfds[0]);
				//System.out.println(eventfds[5] + ' ' + eventfds[4] + ' ' + eventfds[0]);				
				eventList.add(eventfds[5]);
			  }
				// KafkaAnalyzer.suspEventStrs.remove(euuidHex);
			}
			//System.out.println("Seq#, InvokedTimes, suspAPICall, provenanceUUIDSorting");
			eventList.sort((left, right) -> Integer.parseInt(left) - Integer.parseInt(right));
			System.out.println("Seq#, InvokedTimes, suspAPICall, provenanceUUID === Sorted");
			// sort it
			String prev = "";
			String next = "";
			String preSeq = "";
			eventTracWriterSub.println("Seq#, InvokedTimes, suspAPICall, provenanceUUID"); // suspSub.substring(25,
			System.out.println("Seq#, InvokedTimes, suspAPICall, provenanceUUID"); 

			int cnt = 1;
			for (String seq : eventList) {
				next = suspProcHash.get(seq);
				if (!prev.contains(next)) {
					if (prev != "") {
						String apicall = prev.split("\\(")[0];
						String[] fds = prev.split("\\)");
						String preduuid = "";
						if (fds.length > 1)
							preduuid = fds[1];
						String[] preds = preduuid.split(";;");
						String predsPrint = ";;";
						for (int i = 1; i < preds.length; i++) {
							predsPrint += ',' + preds[i];
						}
						if (preds.length > 1)
							eventTracWriterSub.println(preSeq + ":  invoked " + cnt + " times:   " + apicall + " " + predsPrint);
						else 
							eventTracWriterSub.println(preSeq + ":  invoked " + cnt + " times:   " + apicall + " NoPredPath");
					} // suspSub.substring(25, suspSub.length()) + ':'
					prev = next;
					preSeq = seq;
					cnt = 1;
				} else
					cnt += 1;
			}

			String apicall = prev.split("\\(")[0];
			String[] preduuids = prev.split("\\)");
			String preduuid = "";
			if (preduuids.length > 1)
				preduuid = preduuids[1];
			eventTracWriterSub.println(preSeq + ":  invoked " + cnt + " times:   " + apicall + ", " + preduuid);
		
		eventTracWriterSub.flush();
		eventTracWriterSub.close();
		
		KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex).clear(); // only report periodically 
	}
	
	public static void reportTraces(String dir) { // multithreads replaced by  reportRankedTraces
		System.out.println("======================Report Suspicious Traces============================\n");
		Iterator<String> itrsub = KafkaAnalyzer.suspSubUUID.iterator();
		while (itrsub.hasNext()) {
			String suspSub = itrsub.next();
			HashMap<String, String> suspProcHash = new HashMap<String, String>();
			List<String> list = new ArrayList<String>();

			
			String log_random_id = new SimpleDateFormat("MM-dd-HH-mm-ss").format(new Date());
			File eventTracReport = new File(
					dir + '/' + suspSub.substring(15, suspSub.length()) + '-' + log_random_id + '-' + "eventTSorted.txt");
			try {
				// System.out.println("created FILE\n");
				KafkaAnalyzer.eventTracWriter = new PrintWriter(
						new FileOutputStream(eventTracReport.getAbsolutePath()));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			synchronized (KafkaAnalyzer.mutexsuspEventStrs) {
				for (String euuidHex : KafkaAnalyzer.suspEventStrs.keySet()) {
					String eventStr = KafkaAnalyzer.suspEventStrs.get(euuidHex);
					if (eventStr.contains(suspSub)) {
						String[] eventfds = eventStr.split(":;:");
						if (eventfds.length > 3) {
							suspProcHash.put(eventfds[5], eventfds[3] + ' ' + eventfds[0]);
							list.add(eventfds[5]);
						}
						// KafkaAnalyzer.suspEventStrs.remove(euuidHex);
					}
					list.sort((left, right) -> Integer.parseInt(left) - Integer.parseInt(right));
					// if (suspSubUUID.contains())
					// System.out.println("suspEvent:" + eventNode.toString());
				}
			}

			String prev = "";
			String next = "";
			String preSeq = "";
			KafkaAnalyzer.eventTracWriter.println("Seq#, InvokedTimes, suspAPICall, provenanceUUID"); // suspSub.substring(25,

			int cnt = 1;
			for (String seq : list) {
				next = suspProcHash.get(seq);
				if (!prev.contains(next)) {
					if (prev != "") {
						String apicall = prev.split("\\(")[0];
						String[] fds = prev.split("\\)");
						String preduuid = "";
						if (fds.length > 1)
							preduuid = fds[1];
						String[] preds = preduuid.split(";;");
						String predsPrint = ";;";
						for (int i = 1; i < preds.length; i++) {
							predsPrint += ',' + preds[i];
						}
						if (preds.length > 1)
							KafkaAnalyzer.eventTracWriter
									.println(preSeq + ":  invoked " + cnt + " times:   " + apicall + " " + predsPrint);
					} // suspSub.substring(25, suspSub.length()) + ':'
					prev = next;
					preSeq = seq;
					cnt = 1;
				} else
					cnt += 1;
			}

			String apicall = prev.split("\\(")[0];
			String[] preduuids = prev.split("\\)");
			String preduuid = "";
			if (preduuids.length > 1)
				preduuid = preduuids[1];
			KafkaAnalyzer.eventTracWriter
					.println(preSeq + ":  invoked " + cnt + " times:   " + apicall + ", " + preduuid);
			// eventTracWriter.println(preSeq + ": invoked "+ cnt + " times: " +
			// prev.split("\\(")[0]); //suspSub.substring(25, suspSub.length())
			// + ':'
			KafkaAnalyzer.eventTracWriter.flush();
			KafkaAnalyzer.eventTracWriter.close();
		}
		// KafkaAnalyzer.eventAllWriter.close();
	}

	public static void reportBinder(PrintWriter reportWriter, String tag, HashMap<String, String> theMap) {
		for (String uuidPair : KafkaAnalyzer.allSuspBinderSubject.keySet()) {
			reportWriter.println(tag + ":" + uuidPair + ' ' + KafkaAnalyzer.allSuspBinderSubject.get(uuidPair));
		}
		reportWriter.flush();
	}
	
	public static void reportProv2ObjMap(PrintWriter reportWriter) {
		for (String provuuid : KafkaAnalyzer.provUUID2ObjUUID.keySet()) {
			reportWriter.println("prov2objUUIDMap:" + provuuid + ':' + KafkaAnalyzer.provUUID2ObjUUID.get(provuuid));
		}
		reportWriter.flush();
	}
	
	public static void reportMap(PrintWriter reportWriter, String tag, Map<String, String> theMap) {
		for (Object key : theMap.keySet()) {
			reportWriter.println(tag + ";" + key + ";" + theMap.get(key));
		}
		reportWriter.flush();
	}
	
	

	public static void finalReport(String report_dir_path, String timename) {
		/*
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
		try {
			File provReport = new File(report_dir_path + "/" + "nodesEdges-" + timename + ".txt");
			File provObjReport = new File(report_dir_path + "/" + "nodesEdgesObj-" + timename + ".txt");
			
			File susProvReport = new File(report_dir_path + "/" + "nodesEdgesSusp-" + timename + ".txt");
			File susFlowObjReport = new File(report_dir_path + "/" + "nodesEdgesSuspObj-" + timename + ".txt");


			PrintWriter provReportWriter = new PrintWriter(new FileOutputStream(provReport.getAbsolutePath()));
			PrintWriter provObjReportWriter = new PrintWriter(new FileOutputStream(provObjReport.getAbsolutePath()));

			PrintWriter suspProvReportWriter = new PrintWriter(new FileOutputStream(susProvReport.getAbsolutePath()));
			PrintWriter suspObjReportWriter = new PrintWriter(new FileOutputStream(susFlowObjReport.getAbsolutePath()));

			File binderIPCReport = new File(report_dir_path + "/" + "binderIPC-" + timename + ".txt");
			PrintWriter binderIPCReportWriter = new PrintWriter(new FileOutputStream(binderIPCReport.getAbsolutePath()));
			
			File prov2ObjMapReport = new File(report_dir_path + "/" + "prov2ObjMap-" + timename + ".txt");
			PrintWriter prov2ObjMapReportWriter = new PrintWriter(new FileOutputStream(prov2ObjMapReport.getAbsolutePath()));
			
			File hasLibsReport = new File(report_dir_path + "/" + "hasLibs-" + timename + ".txt");
			PrintWriter hasLibsReportWriter = new PrintWriter(new FileOutputStream(hasLibsReport.getAbsolutePath()));
			
			File subSuspStrsReport = new File(report_dir_path + "/" + "suspStrs-" + timename + ".txt");
			PrintWriter subSuspStrsReportWriter = new PrintWriter(new FileOutputStream(subSuspStrsReport.getAbsolutePath()));
		
			synchronized (KafkaAnalyzer.mutexProvGraph) {
				printProvGraph(provReportWriter, provObjReportWriter);
				//System.out.println("Reporting Provs EDGE::");
				printSuspProvGraph(suspProvReportWriter, suspObjReportWriter);
				//System.out.println("Reporting Provs After EDGE::");
			}
		
		Util.matchSuspEvents();
		Util.reportSuspSubj();		
		Util.reportSuspSubjEvents(report_dir_path);
		//Util.reportTraces(report_dir_path);	
		Util.reportMap(prov2ObjMapReportWriter, "prov2objUUIDMap", KafkaAnalyzer.provUUID2ObjUUID);
		Util.reportMap(subSuspStrsReportWriter, "suspStr", KafkaAnalyzer.subjectSuspStrMap);
		Util.reportMap(binderIPCReportWriter, "BinderIPC", KafkaAnalyzer.allSuspBinderSubject);
		Util.reportMap(hasLibsReportWriter, "hasLibs", KafkaAnalyzer.hasLibArmSubjectNamesMap);
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (KafkaAnalyzer.suspEventStrs != null)
			KafkaAnalyzer.suspEventStrs.clear();
		if (KafkaAnalyzer.eventTracWriter != null)
			KafkaAnalyzer.eventTracWriter.close();
		if (KafkaAnalyzer.eventAllWriter != null)
			KafkaAnalyzer.eventAllWriter.close();
		if (KafkaAnalyzer.subAllWriter != null){
			KafkaAnalyzer.subAllWriter.flush();
			KafkaAnalyzer.subAllWriter.close();}
		
	}
}

/*
 * private HashSet<UUID> suspUUIDSet = new HashSet<UUID>(); public static void
 * addCheckUUID(UUID id) { this.suspUUIDSet.add(id); }
 * 
 */
/*
 * TreeSet<Map.Entry<String, Integer>> entriesSet = new TreeSet<>(new
 * Comparator<Map.Entry<String, Integer>>(){ public int
 * compare(Map.Entry<String, Integer> me1, Map.Entry<String, Integer> me2) {
 * return me1.getValue().compareTo(me2.getValue()); } });
 * entriesSet.addAll(this.subjectScoreVector.entrySet());
 */