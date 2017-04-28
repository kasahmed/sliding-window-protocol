package frame;

import java.io.Serializable;

public class Frame implements Serializable
{
	private final int seq;
	private final int ack;
	
	public Frame(int seq, int ack)
	{
		this.seq = seq;
		this.ack = ack;
	}
	
	public int getSeq()
	{
		return seq;
	}
	
	public int getAck()
	{
		return ack;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "[Frame: seq: " + seq + " ack: " +  ack + "]" ;
	}
	
}
