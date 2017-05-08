package protocol;

import server.Networking;
import frame.Frame;

public class Protocol4 implements Runnable{

	final int MAX_TRANSMISSIONS = 10;
	int transmissions= 0;
	final int MAX_SEQ = 3;
	int nextFrameToSend;
	int frameExpected;
	Networking physicalLayer;
	Frame r, s;
	
	public Protocol4(Networking physicalLayer)
	{
		this.physicalLayer = physicalLayer;
		
		this.run();
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		nextFrameToSend = 0;
		frameExpected = 0;
		
		if(physicalLayer == null)
		{
			System.err.println("No network attached, please attach a network");
			return;
		}
		
		
		System.out.println("Starting Protocl4 (Server)");
		while(true)
		{
			Frame r = (Frame)physicalLayer.getInputStream();
			
			//check if frame is damaged
			
			if(r.getAck() == nextFrameToSend)
			{
				new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						System.out.println(transmissions + ". " + r.toString());
					}
					
				}.run();
				
				nextFrameToSend = inc(nextFrameToSend);
				//release timer
			}
			
			
			
			
			
			/*
			if(transmissions > MAX_TRANSMISSIONS)
				break;
			*/
			transmissions += 1;
			physicalLayer.setOutputStream(new Frame(nextFrameToSend,  1 - frameExpected, r.getBuff()));
			//set timer
		}
	}
	
	private boolean inBetween(int a, int b, int c)
	{
		if( ((a <= b) && (b < c)) || ((c < a) && (a <= b)) || ( (b < c) && (c < a)))
			return true;
		return false;
	}
	
	public int inc(int value)
	{
		value++;
		
		if(value > MAX_SEQ)
			return 0;
		return value;
	}
	
	
	
	
}
