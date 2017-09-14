package ibm.cs;

import java.awt.FontFormatException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class sortTracesMulti implements Runnable {
	private Thread t;
	private String threadName;
	private String suuidHex;

	private String dirName;

	private ArrayList<String> eventStringList = new ArrayList<String>();
	// private final CountDownLatch latch;

	sortTracesMulti(String name, List<String> eventList, String suuidHexStr, String dir) {
		threadName = name;
		// KafkaAnalyzer.mutex.acquire();
		Optional.ofNullable(eventList).ifPresent(eventStringList::addAll);
		//System.out.println("Copied!!! " + name);
		suuidHex = suuidHexStr;
		dirName = dir;
	}

	public void run() {
		// System.out.println("Running " + threadName );
		try {
			
			if (!(KafkaAnalyzer.whtSubjectStrs.containsKey(suuidHex))) // only print suspicious Subj KafkaAnalyzer.cnnctSubjectStrs.containsKey(suuidHex)	
			{
				String log_random_id = new SimpleDateFormat("MM-dd-HH-mm-ss").format(new Date());
				File eventTracReport = new File(dirName + '/' + suuidHex.substring(15, suuidHex.length()) + '-'
					+ log_random_id + '-' + "eventSortedTrace.txt");

			// System.out.println("created FILE\n");
				PrintWriter eventTracWriterSub = new PrintWriter(new FileOutputStream(eventTracReport.getAbsolutePath()));
				Util.reportRankedTraces(eventTracWriterSub, suuidHex, eventStringList);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// }
			// catch (FontFormatException e) {
			// System.out.println("FontFormatException Thread " + threadName + "
			// interrupted.");
		} finally {
			// latch.countDown();
			System.out.println("Thread " + threadName + " exiting.");
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
