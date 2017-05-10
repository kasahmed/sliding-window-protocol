package protocol;


import java.nio.charset.Charset;
import java.util.ArrayList;
import frame.Frame;
import layers.NetworkLayer;
import server.Networking;

public class Protocol5 implements Runnable
{
	
	int MAX_SEQ = 1;
	int DROP_RATE;
	int nextFrameToSend;
	int frameExpected;
	int nBuffered = 0;
	Networking physicalLayer;
	NetworkLayer networkLayer;
	Thread[] timers;
	BuffData[] buffer;
	
	long executionTime = 0;
	long startTime = 0;
	long totalTime = 0;
	
	public Protocol5(Networking physicalLayer, int maxSeq, int dropRate)
	{
		this.MAX_SEQ = maxSeq;
		this.DROP_RATE = dropRate;
		timers = new Thread[MAX_SEQ + 1];
		this.physicalLayer = physicalLayer;
		
		this.run();
	}
	
	private void sendData(int frameNum, int frameExpected, byte[] data)
	{
		
		Frame s = new Frame(Frame.DATA, frameNum, (frameExpected + MAX_SEQ) % (MAX_SEQ + 1), data);

		
		if((int)(Math.random() * 100) <= (100 - DROP_RATE))
		{
			physicalLayer.setOutputStream(s);
			System.out.println("Sent Frame: " + s);
			
		}
		else
		{
			System.out.println("dropped Frame: " + s);
		}
		setTimer(frameNum);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		nextFrameToSend = 0;
		frameExpected = 0;
		networkLayer = new NetworkLayer();
		
		
		
		nBuffered = 0;
		int ackExpected = 0;
		
		BuffData[] buffer = new BuffData[MAX_SEQ + 1];
		
		System.out.println("Starting Protocol5 (Client)");
		
		startTime = System.currentTimeMillis();
		long endTime = 0;
		
		networkLayer.start();
		while(true)
		{
			long waitStart = System.currentTimeMillis();
			int event = getEvent();
			totalTime += System.currentTimeMillis() - waitStart;
			if(event == -1)
			{
				endTime = System.currentTimeMillis();
				for(int i = 0; i  < timers.length; i++)
					stopTimer(i);
				break;
			}
			
			
			switch(event)
			{
				case 1 : //Network layer has frame
					
					byte[] data = networkLayer.getData();
					buffer[nextFrameToSend] = new BuffData(data);
					
					sendData(nextFrameToSend, frameExpected, data);
					nextFrameToSend = inc(nextFrameToSend);
					nBuffered += 1;
					break;
				case 2: //Frame arrival
					
					Frame r = (Frame)physicalLayer.getPacket();
					
					if( !((int)(Math.random() * 100) <= (100 - DROP_RATE)))
					{
						System.out.println("Ack dropped: " + r);
						break;
					}
					else
						System.out.println("Ack Recieved: " + r);
					
					if(r.getSeq() == frameExpected)
					{
						frameExpected = inc(frameExpected);
						new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								System.out.println("Recieved: " + r.toString());
							}
							
						}.run();
						
					}
					
					while(inBetween(ackExpected, r.getAck(), nextFrameToSend))
					{
						nBuffered -= 1;
						stopTimer(ackExpected);
						ackExpected = inc(ackExpected);
					}
					break;
				case 4: //timeout
					nextFrameToSend = ackExpected;
					
					for(int i = 0; i < nBuffered; i++)
					{
						sendData(nextFrameToSend, frameExpected, buffer[nextFrameToSend].getBuff());
						nextFrameToSend = inc(nextFrameToSend);
					}
					break;
			}
			
			if(nBuffered < MAX_SEQ)
				networkLayer.enableLayer();
			else
				networkLayer.disableLayer();
			
		}
		this.executionTime += (endTime - startTime);
		System.out.println("Time took to complete protocol in milliseconds: " + executionTime + " Time in idle: " + totalTime);
		
		
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
	
	/**
	 * Creates and starts the timer for a specific frame
	 * @param seq The seq number you want to set timer for. 
	 */
	public void setTimer(int seq)
	{
		
		Thread currThread = Thread.currentThread();
		Thread t =  new Thread(new Runnable(){

			private Thread curr = currThread;
			private int s = seq;
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(50);
					curr.interrupt();
					
					
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					
				}
				
			}
			
		});
		
		if(timers[seq] == null)
			timers[seq] = t;
		else
		{
			
			
			timers[seq].interrupt();
			timers[seq] = null;
			timers[seq] = t;
			
		}
		
		timers[seq].start();
	}
	
	/**
	 * Stops a timer for a specific frame
	 * @param seq frame number you want to remove timer for
	 */
	public void stopTimer(int seq)
	{
		if(timers.length < seq)
			return;
		if(timers[seq] != null)
		{
			timers[seq].interrupt();
			timers[seq] = null;
		}
		
	}
	
	/**
	 * Returns an event depending on the flags that are raised. 
	 * @return 1 if the network layer has data. 2 if physical layer has
	 * data. 4 if a frame got timed out. -1 indicating that the protocol should
	 * terminate itself. 
	 */
	private int getEvent()
	{
		
		while(true)
		{
			
			if(networkLayer.hasItem()) //Network layer has data
				return 1;
			else if(physicalLayer.hasPacket()) // got something from receiver
				return 2;
			else if(Thread.currentThread().interrupted()) //timeout
				return 4;
			else if(!physicalLayer.isConnected()) //checks status of connection
				return -1;
			else if(networkLayer.isFinished() && nBuffered == 0) //protocol finished
				return -1;
			
		}
		
	}
	
	private void printBuffer(ArrayList<byte[]> buff)
	{
		for(int i = 0; i <  buff.size(); i++)
		{
			String message2 = new String(buff.get(i),  Charset.forName("UTF-8"));
			System.out.println("buffer" + i + ": " + message2);
		}
	}
	
	
	
	private class BuffData
	{
		private byte[] data;
		
		public BuffData(byte[] data)
		{
			this.data = data;
		}
		
		public byte[] getBuff()
		{
			return data;
		}
		
		
	}
	
	
}

