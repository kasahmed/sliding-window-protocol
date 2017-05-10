package frame;

import java.io.Serializable;
import java.nio.charset.Charset;

public class Frame implements Serializable
{
	public final static String ACK = "ACK";
	public final static String DATA = "DATA";
	public final static String NAK = "NAK";
	
	private byte[] buff;
	private final int seq;
	private final int ack;
	private final String kind;
	
	public Frame(String kind, int seq, int ack, byte[] data)
	{
		this.kind = kind;
		this.seq = seq;
		this.ack = ack;
		buff = data;
		
	}
	
	public int getSeq()
	{
		return seq;
	}
	
	public int getAck()
	{
		return ack;
	}
	
	public byte[] getBuff()
	{
		return buff;
	}
	
	public String getKind()
	{
		return kind;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		
		String message = new String(buff,  Charset.forName("UTF-8"));
		return "[Frame: seq: " + seq + " ack: " +  ack + " Kind: " + kind + " Message: " + message + "]";
	}
	
	
	
}
