package ibm.cs;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {

	public static void main(String[] args) throws IOException {
		KafkaAnalyzer analyzer = new KafkaAnalyzer(args[0]);
		

		int runLoc = 0;
		boolean dump = false;
		if (args.length < 5)
			System.exit(0);
		String kafkaFile = args[0];
		String path = args[1];
		String multiT = args[2]; 			
		String recordSize = args[3];
		String uuidpath = args[4];
		String policyFile = args[5];
		
		if (runLoc == 1) {
			
			//String uuidpath = "/home/darpa/suspUUID.txt";
			if (kafkaFile.contains("f")) {				
				analyzer.startFromAvroFile(path, multiT, uuidpath, dump, policyFile);
			} else if (kafkaFile.contains("dump")) {
				dump = true;
				analyzer.startFromAvroFile(path, multiT, uuidpath, dump, policyFile);
			} else if (kafkaFile.contains("k")) {
				analyzer.start(multiT, recordSize, path);
			 } else if (kafkaFile.contains("bovia")|| kafkaFile.contains("pandex")) {				
				String queryT = args[1];
				String graphN = args[0];
				String queryK =  args[2];
				analyzer.startQuery(queryT, graphN, queryK);			 
			} else { 				
				System.out.println("Usage: java -jar riskDroid2.jar k/f/ /data/ s/m");
				System.out.println("Usage: java -jar riskDroid2.jar cadets-bovia-1 queryelembyproperty/queryelembyuuidB uuids/EVENT_SENDTO"); 
			}
		} else {
			// analyzer.start();
			multiT = "s";
			int testbasicOps = 8;
			uuidpath = "/Users/hhuang/Documents/suspUUID.txt";
			
			if (testbasicOps == 0)				
				analyzer.startFromAvroFile("/Users/hhuang/Documents/avro_demo/avro_demo/dataCDM/basic-ops.cdm", multiT, uuidpath, dump, policyFile);
			else if (testbasicOps == 1)
				analyzer.startFromAvroFile("/Users/hhuang/Documents/avro_demo/avro_demo/dataCDM/boot-and-gather.cdm",
						multiT, uuidpath, dump, policyFile);
			else if (testbasicOps == 2) {
				analyzer.startFromAvroFile(
						"/Users/hhuang/Downloads/2017-04-20/gather-project/boot-and-gatherproject.cdm", multiT, uuidpath, dump, policyFile); // native				
			} else if (testbasicOps == 3) {// AdumpAPT
				analyzer.startFromAvroFile("/Users/hhuang/Downloads/prov-output.cdm", multiT, uuidpath, dump, policyFile);				
			} else if (testbasicOps == 4) { 
				analyzer.startFromAvroFile("/Volumes/seagate8t/DP-TC/ta1-clearscope-cdm17.bin", multiT, uuidpath, dump, policyFile);
				analyzer.startFromAvroFile("/Volumes/seagate8t/DP-TC/ta1-clearscope-cdm17.bin.1", multiT, uuidpath, dump, policyFile);
				analyzer.startFromAvroFile("/Volumes/seagate8t/DP-TC/ta1-clearscope-cdm17.bin.2", multiT, uuidpath, dump, policyFile);
				analyzer.startFromAvroFile("/Volumes/seagate8t/DP-TC/ta1-clearscope-cdm17.bin.3", multiT, uuidpath, dump, policyFile);
				analyzer.startFromAvroFile("/Volumes/seagate8t/DP-TC/ta1-clearscope-cdm17.bin.4", multiT, uuidpath, dump, policyFile);
					// analyzer.startFromAvroFile("/Volumes/seagate8t/DP-TC/prov-output.cdm");
				// analyzer.startFromAvroFile("/Volumes/seagate8t/DP-TC/2017-02-21-adump.cdm");
				// analyzer.startFromAvroFile("/Volumes/seagate8t/DP-TC/2017-02-21.cdmt");
			} else if (testbasicOps == 5) {
				analyzer.startFromAvroFile("/Users/hhuang/Downloads/ta1-clearscope-pandex-cdm17.bin.5", multiT, uuidpath, dump, policyFile);
				//analyzer.startFromAvroFile("/Users/hhuang/Downloads/ta1-clearscope-pandex-cdm17.bin.5", multiT, uuidpath);				
				//analyzer.startFromAvroFile("/Users/hhuang/Downloads/ta1-clearscope-pandex-cdm17.bin.6", multiT, uuidpath);				
			} else if (testbasicOps == 6) {
				analyzer.startFromAvroFile("/Users/hhuang/Downloads/2017-12-19-ClearScope-GatherProject-CDM18/GatherProject.cdm", multiT, uuidpath, dump, policyFile);
				//analyzer.startFromAvroFile("/Users/hhuang/Downloads/ta1-clearscope-pandex-cdm17.bin.5", multiT, uuidpath);				
				//analyzer.startFromAvroFile("/Users/hhuang/Downloads/ta1-clearscope-pandex-cdm17.bin.6", multiT, uuidpath);				
			}  else if (testbasicOps == 7) {
				analyzer.startFromAvroFile("/data/ta1-cadets-bovia-cdm17.bin", multiT, uuidpath, dump, policyFile);
				//analyzer.startFromAvroFile("/Users/hhuang/Downloads/ta1-clearscope-pandex-cdm17.bin.5", multiT, uuidpath);				
				//analyzer.startFromAvroFile("/Users/hhuang/Downloads/ta1-clearscope-pandex-cdm17.bin.6", multiT, uuidpath);				
			}  else if (testbasicOps == 8) {
				analyzer.startFromAvroFile("/data/trace-avro/ta1-trace-cdm17-benign-0424.bin", multiT, uuidpath, dump, policyFile);
				//analyzer.startFromAvroFile("/Users/hhuang/Downloads/ta1-clearscope-pandex-cdm17.bin.5", multiT, uuidpath);				
				//analyzer.startFromAvroFile("/Users/hhuang/Downloads/ta1-clearscope-pandex-cdm17.bin.6", multiT, uuidpath);				
			} 
			
			
		}
		// analyzer.startFromAvroFile("/Users/hhuang/Documents/avro_demo/avro_demo/dataCDM/background-email.avro");
	}

}
