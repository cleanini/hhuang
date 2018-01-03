package ibm.cs;

import com.bbn.tc.schema.avro.cdm18.StartMarker;
import com.bbn.tc.schema.avro.cdm18.UUID;

public class CUUID implements Comparable {

	private UUID uuid;
	
	private String hexUUID=null;
	/**
	 * The CDM Instance type associated with this UUID.
	 */
	private String type; 
	
	/**
	 * Save off the timestamp associated with the vertex.
	 */
	private Long timestamp;
	
	public String getHexUUID()
	{
		if (hexUUID==null)
		{
			byte a[] = uuid.bytes();
			StringBuffer sb = new StringBuffer();
			for (int i=0;i<a.length;i++)
			{
				//sb.append(Integer.toHexString(a[i]));
				sb.append(String.format("%02x", a[i]));
			}
			hexUUID= sb.toString();
		}
		return hexUUID;
	}
	
	public UUID getUUID()
	{
		return uuid;
	}
	
	
	public CUUID(UUID uuid)
	{
		this.uuid = uuid;
		String wdc = getHexUUID();
	}
	
	public CUUID(String hexUUID, String type)
	{
		byte uuidBytes[] = new byte[16];
		int j=0;
		for (int i=0;i<hexUUID.length();i+=2)
		{
			String b2 = hexUUID.substring(i, i+1);
			uuidBytes[j] = Byte.valueOf(b2,16);
			j+=1;
		}
		this.uuid = new UUID(uuidBytes);
		this.type= type;
		this.hexUUID = hexUUID;
	}

	public CUUID(UUID uuid,String type)
	{
		this(uuid);
		this.type = type;
	}
	
	
	public int compareTo(CUUID that) {

		return hexUUID.compareTo(that.getHexUUID());
		//return super.compareTo(that);
	}
	
	
	public byte[] bytes()
	{
		return this.uuid.bytes();
	}


	public int compareTo(Object o) {
		if (o instanceof CUUID )
		{
			return compareTo((CUUID)o);
		} else {
	     	return 1;
		}
	}

	public String getType() {
		return type;
	}

    void setType(String type) {
		this.type = type;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
	
}