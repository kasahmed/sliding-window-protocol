package protocol;

import server.Networking;
import frame.Frame;

public class Protocol4 implements Runnable{

	final int MAX_TRANSMISSIONS = 10;
	int transmissions= 0;
	final int MAX_SEQ = 1;
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
		
		//physicalLayer.setOutputStream(new Frame(nextFrameToSend, 1 - frameExpected));
		//set timer
		System.out.println("Starting Protocl4 (Server");
		while(true)
		{
			Frame r = (Frame)physicalLayer.getInputStream();
			
			//check if frame is damaged
			
			if(r.getSeq() == frameExpected)
			{
				frameExpected = inc(frameExpected);
			}
			
			if(r.getAck() == nextFrameToSend)
			{
				nextFrameToSend = inc(nextFrameToSend);
				//release timer
			}
			
			new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					System.out.println(transmissions + ". " + r.toString());
				}
				
			}.run();
			
			
			/*
			if(transmissions > MAX_TRANSMISSIONS)
				break;
			*/
			transmissions += 1;
			physicalLayer.setOutputStream(new Frame(nextFrameToSend,  1 - frameExpected));
			//set timer
		}
	}
	
	
	public int inc(int value)
	{
		value++;
		
		if(value > MAX_SEQ)
			return 0;
		return value;
	}
	
	
	
	
}
