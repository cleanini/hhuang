package ibm.cs;


import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import com.bbn.tc.schema.serialization.AvroGenericSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericContainer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.log4j.Logger;

import com.bbn.tc.schema.serialization.AvroConfig;
import com.bbn.tc.schema.serialization.Utils;
import com.bbn.tc.schema.utils.RecordGenerator;



public class Producer {
	protected final KafkaProducer<String, String> producer;
	protected final String topic;
	
	protected int count;
	
	
	public AtomicBoolean shutdown = new AtomicBoolean(false);

	public Producer(String serverAddress, String producerID, String topic
			) {
		this.topic = topic;
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddress);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, producerID);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringSerializer");
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		// props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
		// com.bbn.tc.schema.serialization.kafka.KafkaAvroGenericSerializer.class);
		//props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,String.class);                                //????? how to generate data in the right format
//		props.put(AvroConfig.SCHEMA_WRITER_FILE,SchemaFilename);
		// Use the graph serializer if a graph schema was passed in else use strings
		
		producer = new KafkaProducer<>(props);
	}
	
	void run(String js)
	{
			String key = ""+System.currentTimeMillis();
			ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, key, js);
			
			Callback cb = null;					
			try {
				producer.send(record).get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			if (!(cb instanceof DemoCallBack)) {
//					cb.onCompletion(null, null);
//					// stats callback ignores all arguments, so nulls are fine
//			}
	}
	
	public void setShutdown(){
		this.shutdown.set(true);
	}

	class DemoCallBack implements Callback {

		private long startTime;
		private String key;

		public DemoCallBack(long startTime, String key, GenericContainer record) {
			this.startTime = startTime;
			this.key = key;
		}

		/**
		 * A callback method the user can implement to provide asynchronous handling of request completion. This method will
		 * be called when the record sent to the server has been acknowledged. Exactly one of the arguments will be
		 * non-null.
		 *
		 * @param metadata  The metadata for the record that was sent (i.e. the partition and offset). Null if an error
		 *                  occurred.
		 * @param exception The exception thrown during processing of this record. Null if no error occurred.
		 */
		public void onCompletion(RecordMetadata metadata, Exception exception) {

			long elapsedTime = System.currentTimeMillis() - startTime;
			if (metadata != null) {
				System.out.println(
						"message(" + key + ") sent to partition(" + metadata.partition() +
						"), " +
						"offset(" + metadata.offset() + ") in " + elapsedTime + " ms");
			} else {
				exception.printStackTrace();
			}
		}
	}
}
