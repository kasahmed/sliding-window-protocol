package protocol;

import server.Networking;
import frame.Frame;

public class Protocol4 implements Runnable{
	
	final String MESSAGE = "Hello World";
	int messageIndex = 0;
	
	final int MAX_TRANSMISSIONS = 30;
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
		byte[] data = MESSAGE.getBytes();
		
		if(physicalLayer == null)
		{
			System.err.println("No network attached, please attach a network");
			return;
		}
		
		System.out.println("Starting Protocl4 (client)");
		byte[] d = { data[messageIndex]};
		physicalLayer.setOutputStream(new Frame(nextFrameToSend, 1 - frameExpected, d));
		Thread t = this.setTimer(nextFrameToSend);
		t.start();
		//set timer
		while(true)
		{
			Frame r = (Frame) physicalLayer.getPacket();//(Frame)physicalLayer.getInputStream();
			
			//check if frame is damaged
			if(r != null)
			{
				if(r.getSeq() == frameExpected)
				{
					//send to network layer
					frameExpected = inc(frameExpected);
				}
			
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
					messageIndex++;
					t.interrupt();
					//release timer
				}
				
				
			}
			
			
			
			
			if(messageIndex > data.length)
				break;
			if( (int)(Math.random()*100) <= 15)
			{
				transmissions += 1;
				byte[] temp = { data[messageIndex]};
				physicalLayer.setOutputStream(new Frame(nextFrameToSend,  1 - frameExpected, temp));
			}
			//set timer
			t = this.setTimer(nextFrameToSend);
			t.start();
			
		
		}
	}
	
	
	public int inc(int value)
	{
		value++;
		
		if(value > MAX_SEQ)
			return 0;
		return value;
	}
	
	public Thread setTimer(int seq)
	{
		return new Thread(new Runnable(){

			private Thread curr = Thread.currentThread();
			private int s = seq;
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(1000);
					curr.interrupt();
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
				
			}
			
		});
	}
	
	
	
	
}
