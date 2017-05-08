package protocol;

import datastructure.CustomQueue;
import frame.Frame;
import server.Networking;

public class Protocol5 implements Runnable{

	final String MESSAGE = "Hello World";
	int messageIndex = 0;
	
	final int MAX_TRANSMISSIONS = 30;
	int transmissions= 0;
	final int MAX_SEQ = 4;
	int nextFrameToSend;
	int frameExpected;
	Networking physicalLayer;
	NetworkLayer networkLayer;
	
	public Protocol5(Networking physicalLayer)
	{
		this.physicalLayer = physicalLayer;
		
		this.run();
	}
	
	private void sendData(int frameNum, int frameExpected, byte[] data)
	{
		
		Frame s = new Frame(frameNum, (frameExpected + MAX_SEQ) % (MAX_SEQ + 1), data);
		physicalLayer.setOutputStream(s);
		//System.out.println("Sent Frame: " + s);
		setTimer(frameNum);
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		nextFrameToSend = 0;
		frameExpected = 0;
		networkLayer = new NetworkLayer();
		int nBuffered = 0;
		int ackExpected = 0;
		
		System.out.println("Starting Protocol5 (Server)");
		while(true)
		{
			int event = getEvent();
			
			if(event == -1)
				break;
			
			switch(event)
			{
				case 1 :
					
					byte[] data = networkLayer.getData();
					if(data == null)
						break;
					sendData(nextFrameToSend, frameExpected, data);
					nextFrameToSend = inc(nextFrameToSend);
					nBuffered += 1;
					break;
				case 2:
					Frame r = (Frame)physicalLayer.getPacket();
					if(r.getSeq() == frameExpected)
					{
						networkLayer.sendData(r.getBuff());
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
						ackExpected = inc(ackExpected);
						//release timer
					}
					break;
			}
			
			if(nBuffered < MAX_SEQ)
				networkLayer.enableLayer();
			else
				networkLayer.disableLayer();
			
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
	
	private int getEvent()
	{
		
		while(true)
		{
			
			if(networkLayer.hasItem())
				return 1;
			else if(physicalLayer.hasPacket())
				return 2;
			else if(!physicalLayer.isConnected())
				return -1;
			
		}
		
	}
	
	
	private class NetworkLayer extends Thread
	{
		final String MESSAGE = "Hello World";
		int frameSize = 1; //in bits
		private CustomQueue<byte[]> queue = new CustomQueue<byte[]>();
		boolean isDisabled = false;
		
		public NetworkLayer()
		{
			
		}
		
	
		@Override
		public void run() {
			// TODO Auto-generated method stub
			//super.run();
			
			byte[] data = MESSAGE.getBytes();
			int byteIndex = 0;
			
			while(byteIndex < data.length)
			{
				
				if(byteIndex + 1 < data.length)
				{
					
					byte[] dataToSend = {data[byteIndex++], data[byteIndex++]};
					queue.enqueue(dataToSend);
					
				}
				else
				{
					byte[] dataToSend = {data[byteIndex++]};
					queue.enqueue(dataToSend);
				}
			}
		}

		

		public boolean hasItem()
		{
			return !queue.hasItem() && !isDisabled;
		}
		
		public byte[] getData()
		{
			try 
			{
				byte[] dataToSend = queue.dequeue();
				
				return dataToSend;
			} 
			catch (InterruptedException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
		public void sendData(byte[] data)
		{
			queue.enqueue(data);
		}
		public void disableLayer()
		{
			isDisabled = true;
		}
		
		public void enableLayer()
		{
			isDisabled = false;
		}
		
		
		
	}
}
