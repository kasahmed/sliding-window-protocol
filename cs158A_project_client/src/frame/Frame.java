package frame;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Frame implements Serializable
{
	private byte[] buff;
	private final int seq;
	private final int ack;
	
	public Frame(int seq, int ack, byte[] data)
	{
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

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		
		String message = new String(buff,  Charset.forName("UTF-8"));//Arrays.toString(buff);
		return "[Frame: seq: " + seq + " ack: " +  ack + " Message: " + message + "]";
	}
	
	
	
}
