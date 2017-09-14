package ibm.cs;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.bbn.tc.schema.avro.Event;
import com.bbn.tc.schema.avro.FileObject;
import com.bbn.tc.schema.avro.NetFlowObject;
import com.bbn.tc.schema.avro.Principal;
import com.bbn.tc.schema.avro.ProvenanceTagNode;
import com.bbn.tc.schema.avro.SrcSinkObject;
import com.bbn.tc.schema.avro.Subject;
import com.bbn.tc.schema.avro.UUID;
import com.bbn.tc.schema.avro.Value;

import java.util.Iterator;
import java.util.List;
import java.awt.FontFormatException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ProcessDatum17 implements Runnable {
	private Thread t;
	private String threadName;
	private ArrayList<Object> eventDatumList = new ArrayList<Object>();
	//private Set<String> processName;
	private Map<String, Integer> typeCNT; 
	private final CountDownLatch latch;
	ProcessDatum17(CountDownLatch lt, String name, ArrayList<Object> datumList, Map<String, Integer> typecnt) throws InterruptedException {
	      threadName = name;	      
	      KafkaAnalyzer.mutex.acquire();	      
		  Optional.ofNullable(datumList).ifPresent(eventDatumList::addAll);
	      KafkaAnalyzer.mutex.release();
	      System.out.println("Copied!!! " + name);

	      typeCNT = typecnt;
	      latch = lt;
	      //processName = procName;
	      //System.out.println("Creating processDatum17: " +  threadName );
	}
	
	
	
    /**
	 * take different actions for the corresponding datum Type
	 */
	 public void run() {
	      //System.out.println("Running " +  threadName );
	   try {
	      //Iterator<Object> itr = eventDatumList.iterator();
	     for (Object datum: eventDatumList){	    	  
	       String type = datum.getClass().toString();			      
	       	 if (!typeCNT.containsKey((type))) {
					typeCNT.put(type, 1);
			  } else
					typeCNT.replace(type, typeCNT.get(type) + 1);
			
			String className = datum.getClass().getName().substring(23);
	  		//String subHexUUID;
	  		//String provHexUUID; 

	  		//UUID subUUID;
	  		UUID eUUID;
	  		UUID fUUID;	//String netDetailStr;
	  		UUID netUUID;		//String srcsinkUUID;
	  		UUID ssUUID;
	  		//String pcpDetailStr; 
	  		UUID pcpUUID;

	  		
	  		String eventDetailStr;
	  		String eventParamValue; //String eventIOType;
	  		String suspeventDetailStr;
	  		//String srcSinkDetailStr;
	  		String subjDetailStr;
	  		// List<Value> parms = new ArrayList<Value>();
	  		//CUUID cuuid;
	  		//Event event;
	  		//SrcSinkObject srcSinkObject;
	  		//Object eventdatum = null;

	  		String pDetailStr;
	  		String fDetailStr;

	  		switch (className) {
	  		case "Subject":
	  			Subject subject = (Subject) datum;
	  			UUID subUUID = subject.getUuid();
	  			
	  			String subHexUUID = Util.getHexUUIDStr(subUUID);
	  			subjDetailStr = className + "::" + subHexUUID + "::" + subject.toString();
	  			
	  			//subjDetailStr = Util.convertPrintCUUID(subUUID, subject, className, false);
	  			//subHexUUID = subjDetailStr.split("::")[1];

	  			// whether in the white app list
	  			String appname = subject.getCmdLine().toString();
	  			KafkaAnalyzer.processName.add(appname);// add all processName
	  			KafkaAnalyzer.allSubjectStrs.put(subHexUUID, subjDetailStr);			
	  			if (KafkaAnalyzer.TRUSTAPP.contains(appname)) {
	  				// System.out.println("White::appname: " + appname + "
	  				// subjectDetails:: " + subjDetailStr);
	  				KafkaAnalyzer.whtSubjectStrs.put(subHexUUID, subjDetailStr);				
	  				//KafkaAnalyzer.whtSubjectNodes.put(subUUID, subjDetailStr);
	  				System.out.println("Trust::appname: " + appname + " trustDetails:: " + subjDetailStr);
	  				//System.out.println(subject.getCmdLine() + "in the
	  				// whitelist
	  				// app");
	  			} else {
	  				KafkaAnalyzer.suspSubjectStrs.put(subHexUUID, subjDetailStr);
	  				//KafkaAnalyzer.suspSubjectNodes.put(subUUID, subjDetailStr);
	  			    System.out.println("Suspicious::appname: " + appname + " subjectDetails:: " + subjDetailStr);
	  			}
	  			// subject.getClass().toString() + ":" + subject.getPid().toString()
	  			// + ":" + subject.getPpid()
	  			// + ":" + subject.getStartTimestampMicros().toString() + ":" + ;
	  			break;

	  		case "ProvenanceTagNode":
	  			// ArrayList<Object> srcTagIdList;			
	  			// provenance UNION SEQ tagID->tagID and the fileObject->tagID, tagID->networkflow, 
	  			ProvenanceTagNode provenanceTagNode = (ProvenanceTagNode) datum;
	  			UUID targetTagId = provenanceTagNode.getTagId();

	  			// System.out.println(provenanceTagNode);
	  			//pDetailStr = Util.convertPrintCUUID(provenanceTagNode.getTagId(), provenanceTagNode, className, false);

	  			String provHexUUID = Util.getHexUUIDStr(targetTagId);
	  			pDetailStr = className + "::" + provHexUUID + "::" + provenanceTagNode.toString();
	  			
	  			
	  			if (!provenanceTagNode.getTagId().toString().contains(",")) {
	  				System.out.println(pDetailStr);
	  			}
	  			//System.out.println(pDetailStr);
	  			/*
	  			if (provenanceTagNode.getFlowObject()!=null)
	  			  KafkaAnalyzer.matchPrintMal(provenanceTagNode.getFlowObject().toString(), pDetailStr);
	  			 */
	  			String sTargetTagId  = Util.getHexUUIDStr(targetTagId);
  				synchronized(KafkaAnalyzer.mutexProvGraph) {
	  			//if (KafkaAnalyzer.suspSrcTags.contains(s)) 
	  			if (!KafkaAnalyzer.provGraph.containsVertex(sTargetTagId))
	  					KafkaAnalyzer.provGraph.addVertex(sTargetTagId);
	  				
	  			if (provenanceTagNode.getPrevTagId() != null) { // Sequence
	  				UUID srcTagId = provenanceTagNode.getPrevTagId();
	  				String srcTagIDhex = Util.getHexUUIDStr(srcTagId);
	  				if (!KafkaAnalyzer.provGraph.containsVertex(srcTagIDhex))
	  						KafkaAnalyzer.provGraph.addVertex(srcTagIDhex);
	  				if (!KafkaAnalyzer.provGraph.containsEdge(sTargetTagId, srcTagIDhex))
	  						KafkaAnalyzer.provGraph.addEdge(srcTagIDhex, sTargetTagId);	  					
	  			}

	  			List<UUID> tags = provenanceTagNode.getTagIds(); // all TAG_OP_UNION
	  			if (tags != null) {
	  				// System.out.println(pDetailStr);
	  				for (UUID srcTagId : tags) {
	  					String cSrcTagIdHex = Util.getHexUUIDStr(srcTagId);
	  					if (!KafkaAnalyzer.provGraph.containsVertex(cSrcTagIdHex))
	  						KafkaAnalyzer.provGraph.addVertex(cSrcTagIdHex);
	  					if (!KafkaAnalyzer.provGraph.containsEdge(sTargetTagId, cSrcTagIdHex))	  				
	  						KafkaAnalyzer.provGraph.addEdge(cSrcTagIdHex, sTargetTagId);			
	  				}
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
	  			String fuuidHex = Util.getHexUUIDStr(fUUID);
	  			//fDetailStr = Util.convertPrintCUUID(fUUID, fObject, className, false);
	  			fDetailStr = className + "::" + fuuidHex + "::" + fObject.toString();


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
	  			//KafkaAnalyzer.malSRC.add(fUUID.toString());
	  			
	  			String typeStr = fObject.get("type").toString(); 			
	  			//System.out.println("fDetailStr"+fDetailStr);
	  			HashMap<CharSequence, CharSequence> props = (HashMap)fObject.getBaseObject().getProperties();
	  			for (CharSequence i : props.keySet())	{
	  				if (i.toString().contains("path") && !Util.matchSuspStr(props.get(i).toString(), KafkaAnalyzer.MEDIADIR) && 
	  						(Util.matchSuspStr(props.get(i).toString(), KafkaAnalyzer.UNTRUSTDIR)||Util.matchSuspStr(props.get(i).toString(), KafkaAnalyzer.UNTRUSTDIRCODE)))					
	  					KafkaAnalyzer.sensitiveFile.put(fuuidHex, fDetailStr);					
	  			}
	  			
	  			//matchSuspStr(fObject.)
	  			
	  			// two types
	  																// typeStr!="FILE_OBJECT_DIR"
	  																// &&
	  																// typeStr!="FILE_OBJECT_FILE"
	  			//System.out.println(typeStr + ' ' + fObject.getBaseObject().getProperties().toString());
	  			//System.out.println(typeStr + ' ' + fDetailStr);
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
	  			
	  			String netflowUUID = Util.getHexUUIDStr(netUUID);
	  			String netflowDetailStr = className + "::" + netflowUUID + "::" + netflow.toString();
	  			
	  			//String netflowDetailStr = Util.convertPrintCUUID(netUUID, netflow, className, false);
	  			//String netflowUUID = netflowDetailStr.split("::")[1];

	  			//System.out.println(netflowDetailStr);
	  			//if (matchSuspStr(srcSinkDetailStr, KafkaAnalyzer.SENSRCSINK)){
	  				//System.out.println(srcsinkObject.getBaseObject().toString());
	  			if (!netflowDetailStr.contains("unknown"))
	  				KafkaAnalyzer.sensitiveNetflow.put(netflowUUID, netflowDetailStr);			
	  			//}
	  			
	  			// NetFlowObject::00000000000000000000000000000081::
	  			// {"uuid": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -127],
	  			// "baseObject": {"permission": null, "epoch": null, "properties":
	  			// null},
	  			// "localAddress": "::", "localPort": -1, "remoteAddress":
	  			// "<UNKNOWN>", "remotePort": -1,
	  			// "ipProtocol": 0, "fileDescriptor": null}			
	  			//KafkaAnalyzer.malSINK.add(netUUID.toString());
	  			//KafkaAnalyzer.matchPrintMal(netflowUUID, netflowDetailStr);			
	  			//System.out.println(netflowDetailStr);			
	  			break;

	  		case "SrcSinkObject":
	  			SrcSinkObject srcsinkObject = (SrcSinkObject) datum;
	  			ssUUID = srcsinkObject.getUuid();		
	  			
	  			String ssUUIDHex = Util.getHexUUIDStr(ssUUID);
	  			String srcSinkDetailStr = className + "::" + ssUUIDHex + "::" + srcsinkObject.toString();
	  			
	  			//srcSinkDetailStr = Util.convertPrintCUUID(ssUUID, srcsinkObject, className, false);
	  			
	  			KafkaAnalyzer.malSRC.add(ssUUID.toString());

	  			//System.out.println(srcsinkObject.toString());
	  			if (Util.matchSuspStr(srcSinkDetailStr, KafkaAnalyzer.SENSRCSINK)){
	  				//System.out.println(srcsinkObject.getBaseObject().toString());
	  				KafkaAnalyzer.sensitiveSrcSink.put(Util.getHexUUIDStr(ssUUID), srcsinkObject.getType().toString() + srcsinkObject.getBaseObject().toString());			
	  			}
	  			
	  			/*
	  			if (srcsinkObject.getType().toString().contains("SRCSINK_DATABASE"))
	  				if (srcsinkObject.getBaseObject() !=null)
	  				  System.out.println(srcsinkObject.getBaseObject().toString());
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
	  			//pcpDetailStr = Util.convertPrintCUUID(pcpUUID, principalObject, className, false);	  			
	  			String pcpUUIDHex = Util.getHexUUIDStr(pcpUUID);
	  			String pcpDetailStr = className + "::" + pcpUUIDHex  + "::" + principalObject.toString();
	  			
	  			// System.out.println(pcpDetailStr);

	  			break;

	  		
	  		case "Event": // suspicious event match suspicious predicate objects (/emulator/..), match suspicious event types... 
	  			
	  			/*
	  			CharSequence predicateOne = enode.getPredicateObjectPath(); 
	  			if (predicateOne!= null){
	  				String matchStr = predicateOne.toString();
	  				if (!suspEventStrs.containsKey(eUUIDHex) && matchSuspStr(matchStr, UNTRUSTDIR))
	  					addSuspEvent = true;
	  					//System.out.println("ONE " + matchStr); 				 
	  			}
	  			CharSequence predicateTwo = enode.getPredicateObjectPath(); 
	  			if (predicateTwo!= null){
	  				String matchStr = predicateTwo.toString();
	  				if (!suspEventStrs.containsKey(eUUIDHex) && matchSuspStr(matchStr, UNTRUSTDIR))
	  					addSuspEvent = true;
	  					//System.out.println("TWO " + matchStr); 				 
	  			}*/
	  			
	  			//# proc  8 bits vector  wEXFILAPI, wFILEAPI, rEXFILAPI, rFILEAPI, N/A, LOCAPI, CONTENTAPI, WIFIAPI
	  			
	  			
	  			
	  			//System.out.println(suuidHex);

	  			//System.out.println(Util.getHexUUIDStr(enode.getSubject()) + ":" + eventType + "::" + enode.getName());

	  			/*
	   			String eventSuspLevel = "";

	  			if (KafkaAnalyzer.suspSubjectStrs.containsKey(suuidHex))
	  				eventSuspLevel = "susp";
	  			else if (KafkaAnalyzer.whtSubjectStrs.containsKey(suuidHex))
	  				eventSuspLevel = "normal";
	  			else {
	  				eventSuspLevel = "pending";
	  				KafkaAnalyzer.pendEventStrs.put(suuidHex, enode);
	  			}*/
	  			/*
	  			if (false && eventSuspLevel == "susp" && enode.get("name")!=null) {
	  				String apiStr = enode.get("name").toString();
	  				Iterator<String> itrAPI = KafkaAnalyzer.SUSPAPI.iterator();
	  				while (itrAPI.hasNext())			
	  				{
	  					if (apiStr.contains(itrAPI.next())) 
	  						System.out.println(apiStr + ' ' + KafkaAnalyzer.suspSubjectNodes.get(sUUID).toString());					
	  				}
	  			}
	  			*/
	  			 
	  			   //System.out.println(KafkaAnalyzer.suspSubjectNodes.get(sUUID)); // based
	  			// on the calling method API, we could check the suspciousness of
	  			// the event. 	
	  //if (!suuidHex.contains("00000000000000000000000000000001")){	//00000000000000000000000000007b0d
	  			Event enode = new Event(); 			
	  			enode = (Event) datum;
	  			eUUID = enode.getUuid();
	  			boolean addSuspEvent = false; 

	  			//String eDetailStr = Util.convertPrintCUUID(eUUID, enode, className, false);
	  			long timestamp = enode.getTimestampNanos();
	  			if (KafkaAnalyzer.currTimeStamp < timestamp)
	  				KafkaAnalyzer.currTimeStamp = timestamp;
	  			
	  			String eUUIDHex = Util.getHexUUIDStr(eUUID);
	  			String eDetailStr = className + "::" + eUUIDHex + "::" + enode.toString();
	  			//String  eUUIDHex = eDetailStr.split("::")[1];
	  			
	  			UUID sUUID = enode.getSubject();
	  			String suuidHex = Util.getHexUUIDStr(sUUID);
	  			
	  			String predObjectHex = "predObject;;";
	  			UUID predUUID= enode.getPredicateObject();
	  			if (predUUID!=null) {	  				
	  				predObjectHex += Util.getHexUUIDStr(predUUID) + ";;";
	  				CharSequence path = enode.getPredicateObjectPath();
	  				if (path!=null)
	  						predObjectHex = predObjectHex + path.toString() + ";;";
	  			}
	  			UUID predUUID2= enode.getPredicateObject();
	  			if (predUUID2!=null){
	  				predObjectHex += Util.getHexUUIDStr(predUUID2) + ";;";
	  				if (enode.getPredicateObject2Path()!=null)
	  					predObjectHex += enode.getPredicateObject2Path().toString() + ";;";				
	  			}			
	
	       		String eventType = enode.getType().toString();
				String apicall = enode.get("name").toString();
				Long eventTime = enode.getTimestampNanos();
				Long eSeq = enode.getSequence();
				Integer eTid = enode.getThreadId();
				CharSequence ppt = enode.getProgramPoint();
				
				String assEventStr = Util.assemblyEventStr(predObjectHex, suuidHex, eUUIDHex, apicall, eventType, eventTime, eSeq, eTid, ppt);
				// System.out.println("assEventStr" + assEventStr);
	       		
	       		if (assEventStr.contains("[native]"))
	       			System.out.println(assEventStr + " NativeEventParaStr");
	       		
	       		//potentialSuspEvent = false;
	       		
	       		ArrayList<Value> paraList = null;
	       		paraList = new ArrayList<Value>();
				if (enode != null && enode.getParameters() != null){ //&& enode.getParameters().size()>0
					  //System.out.println(enode.getParameters());					
					  Optional.ofNullable(enode.getParameters()).ifPresent(paraList::addAll);
					  //paraList.addAll(enode.getParameters());
				}
				 
	       		String eventParaStr = "";
	       		eventParaStr = Util.printAllEventParamDetails(paraList, suuidHex);
	       		
	       		if (eventParaStr.contains("BinderIPC") && eventParaStr.length()>5){ //&& eventParaStr.contains("MatchProcName")){ && potentialSuspEvent     			
	       			//if (eventParaStr.contains("00000000000000000000000000000000000000000000000000000000"))
	       				KafkaAnalyzer.eventAllWriter.println("BinderIPC-suspEvent:" + assEventStr + " eventParaStr:" + eventParaStr + " Event" + enode.toString());
	       				addSuspEvent = true;
	       		}
	       		
	       		if (eventParaStr.contains("potentialSuspEvent") && !KafkaAnalyzer.whtSubjectStrs.containsKey(suuidHex)){
	  				addSuspEvent = true; // add suspicious event based on parameter values; 
	       		}
	       		
	       		
	       		if (Util.matchSuspStr(apicall, KafkaAnalyzer.CONTENTAPI)){
	       			//System.out.println(eventType + suuidHex + " CONTENTAPI" + apicall + printAllEventParamDetails(enode));     			
	  				addSuspEvent = true;
	  				Util.updateSubScoreVector(KafkaAnalyzer.subjectScoreVector, suuidHex, 1);
	       		}
	       		if (Util.matchSuspStr(apicall, KafkaAnalyzer.WIFIAPI)) {
	       			//System.out.println(eventType + suuidHex + " WIFIAPI" + apicall + printAllEventParamDetails(enode));
	  				addSuspEvent = true;
	  				Util.updateSubScoreVector(KafkaAnalyzer.subjectScoreVector, suuidHex, 2);
	  			}
	  				
	  			if (Util.matchSuspStr(apicall, KafkaAnalyzer.LOCAPI)){
	  				addSuspEvent = true;
	  				Util.updateSubScoreVector(KafkaAnalyzer.subjectScoreVector, suuidHex, 4);				
	  			}    	
	  			
	  			if (Util.matchSuspStr(apicall, KafkaAnalyzer.PREEXFILAPI)){
	  				addSuspEvent = true;
	  				Util.updateSubScoreVector(KafkaAnalyzer.subjectScoreVector, suuidHex, 8);
	  			}
	  			
	  			//System.out.println(suuidHex + " LOCAPI" + apicall + printAllEventParamDetails(enode));
	  			if (Util.matchSuspStr(apicall, KafkaAnalyzer.FILEAPI)){
	  				addSuspEvent = true;
	  				//String paraAll = printAllEventParamDetails(enode);
	  				String matchStr = "";	
	  				CharSequence predicateOne = enode.getPredicateObjectPath();
	  				CharSequence predicateTwo = enode.getPredicateObject2Path();
	  				if (predicateOne!= null){
	  					matchStr = predicateOne.toString();
	  				}	
	  				if (predicateTwo!= null){
	  					matchStr += predicateTwo.toString();
	  				}	
	  				int updateValue = 0;
	  				if (eventType.contains("WRITE")|| eventType.contains("CREATE_OBJECT")) {										
	  					if (Util.matchSuspStr(matchStr, KafkaAnalyzer.UNTRUSTDIR) || Util.matchSuspStr(matchStr, KafkaAnalyzer.UNTRUSTDIRCODE)){
	  						updateValue = 64;
	  						if (!(KafkaAnalyzer.allSubjectStrs.containsKey(suuidHex) && KafkaAnalyzer.allSubjectStrs.get(suuidHex).contains("media"))) {
	  							System.out.println(suuidHex + ":Suspicious [ConfidentialSrc ==> UntrustedFileSink] Pattern Matched " + matchStr);
	  							System.out.println("Suspicious Provenance Graph and Event Traces Updated!!\n");
	  						}
	  						//System.out.println(suuidHex + "::SENDTO_EXFILAPI::" + eventType + "::" + enode.getTimestampNanos() + "::" + apicall + "::" + printAllEventParamDetails(enode));
	  						//System.out.println("::add"+KafkaAnalyzer.suspEventNodeStrs.get(eUUIDHex).toString());
	  		     			//System.out.println("::add"+KafkaAnalyzer.suspEventNodes.get(eUUIDHex).toString());
	  						//System.out.println("::add"+KafkaAnalyzer.suspEventStrs.get(eUUIDHex));
	  						//System.out.println(eventType + ' ' + matchStr + suuidHex + " FILEAPI:" + apicall);
	  					}
	  				} else { //
	  					if (Util.matchSuspStr(matchStr, KafkaAnalyzer.UNTRUSTDIR) && !Util.matchSuspStr(matchStr, KafkaAnalyzer.MEDIADIR)){
	  						updateValue = 16;
	  						if (!(KafkaAnalyzer.allSubjectStrs.containsKey(suuidHex) && KafkaAnalyzer.allSubjectStrs.get(suuidHex).contains("media"))) {
	  							System.out.println(suuidHex + ":Suspicious [ConfidentialFile ==> UntrustedProc] Pattern Matched" + matchStr);
	  							System.out.println("Suspicious Provenance Graph and Event Traces Updated!!\n");
	  						}
	  						
	  						//System.out.println(eventType + ' ' + matchStr + suuidHex + " FILEAPI:" + apicall);
	  					}
	  				}
	  				Util.updateSubScoreVector(KafkaAnalyzer.subjectScoreVector, suuidHex, updateValue);
	  			}    			
	  			
	  			
	  			
	  			if (Util.matchSuspStr(apicall, KafkaAnalyzer.EXFILAPI)){
	  				addSuspEvent = true;
	  				int updateValue = 0;
	  				if (eventType.contains("SENDTO")){
	  					updateValue = 128;
	  	     			//System.out.println("SENDTO:" + enode + ' '+ assEventStr + " eventParaStr:" + eventParaStr);					
	  					System.out.println(suuidHex + ":Suspicious [ConfidentialSrc ==> NetworkSink] Pattern Matched");
	  					System.out.println("Suspicious Provenance Graph and Event Traces Updated!!\n");				
	  				}
	  				else {
	  					updateValue = 32;
	  					System.out.println(suuidHex + ":Suspicious [NetworkSrc ==> UntrustedProc] Pattern Matched");
	  					System.out.println("Suspicious Provenance Graph and Event Traces Updated!!\n");
	  					
	  					//String paraAll = printAllEventParamDetails(enode);
	  					//System.out.println(eventType + suuidHex + " EXFILAPI" + apicall + paraAll);
	  				}
	  				//System.out.println(eventType + printAllEventParamDetails(enode) + suuidHex + " EXFILAPI" + apicall);
	  					//System.out.println(eventType + suuidHex + " EXFILAPI" + apicall + );
	  				Util.updateSubScoreVector(KafkaAnalyzer.subjectScoreVector, suuidHex, updateValue);
	  			}    			
	  				//System.out.println(suuidHex + " EXFILAPI" + apicall + printAllEventParamDetails(enode));    	     		
	  			//if (enode.get get("tag")!=null)
	  				//System.out.println(enode.get("name").toString() + ' ' + enode.get("tag").toString());
	   
	  			//System.out.println(suuidHex + "::predObjectHex::" + predObjectHex + "::" + eventType + "::" + enode.getTimestampNanos() + "::" + apicall + "::" + printAllEventParamDetails(enode));
	  						
	  			if (addSuspEvent) {
	  			  synchronized(KafkaAnalyzer.mutexsuspEventStrs){
	  				if (!KafkaAnalyzer.suspEventStrs.containsKey(eUUIDHex)) {						
	  					KafkaAnalyzer.suspEventStrs.put(eUUIDHex, assEventStr);
	  	     			//System.out.println("potentialSuspEvent:" + assEventStr + ' ' + eventParaStr);
	  					//ArrayList<Value> paraList = new ArrayList<Value>(); 
						/*
	  	     			if (paraList != null && paraList.size()>0){
							//paraList.addAll(enode.getParameters());
							Util.addEventInOutProvsDetails(paraList, eventType);
						}*/
	  				}
	  			 }
	  				
	  			 synchronized(KafkaAnalyzer.mutexsuspSubjEventUUIDs){
	  				List<String> eUUIDs = KafkaAnalyzer.suspSubjEventUUIDs.get(suuidHex);
					if (eUUIDs==null) {
						eUUIDs = new ArrayList<String>();
						KafkaAnalyzer.suspSubjEventUUIDs.put(suuidHex, eUUIDs);
					}
					eUUIDs.add(eUUIDHex + "::" + enode.getSequence() +"::" + apicall);
					
					//System.out.println("potentialSuspEvent:" + assEventStr + ' ' + eventParaStr);
					// ArrayList<Value> paraList = new ArrayList<Value>();
					if (enode.getParameters() != null) {
						// paraList.addAll(enode.getParameters());
						Util.addEventInOutProvsDetails(paraList, eventType);
					} 
	  			  }	  				
	  			}
	  	      	break;
	  		default:
	  			/*
	  			String type = datum.getClass().toString();			
	  			if (!typecnt.containsKey((type))) {
	  				typecnt.put(type, 1);
	  			} else
	  				typecnt.replace(type, typecnt.get(type) + 1);
	  			 */
	  			// System.out.println("CaseType Not Processed " +
	  			// datum.getClass().toString());
	  			break; // com.bbn.tc.schema.avro.Principal

	  		}	
	  	
	     }
	   } catch (FontFormatException e) {
	         System.out.println("FontFormatException Thread " +  threadName + " interrupted.");
	   }  finally{
	  		latch.countDown();
	  		System.out.println("Thread " +  threadName + " exiting.");
	   }  
	       
	     
	      
	   }

	   public void start () throws FontFormatException{
	      System.out.println("Starting " +  threadName );
	      if (t == null) {
	         t = new Thread (this, threadName);	         
	         t.start();	     
	         /*
	         try {
				t.join();
			 } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
	      }
	   }
}
