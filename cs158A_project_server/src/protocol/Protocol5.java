package protocol;

import datastructure.CustomQueue;
import frame.Frame;
import server.Networking;

public class Protocol5 implements Runnable{

	int MAX_SEQ = 1;
	int nextFrameToSend;
	int frameExpected;
	Networking physicalLayer;
	NetworkLayer networkLayer;
	Thread[] timers;
	
	public Protocol5(Networking physicalLayer, int maxSeq)
	{
		this.physicalLayer = physicalLayer;
		this.MAX_SEQ = maxSeq;
		timers = new Thread[MAX_SEQ + 1];
		this.run();
	}
	
	private void sendData(int frameNum, int frameExpected, byte[] data)
	{
		
		Frame s = new Frame(Frame.ACK, frameNum, (frameExpected + MAX_SEQ) % (MAX_SEQ + 1), data);
		physicalLayer.setOutputStream(s);
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
		
		BuffData[] buffer = new BuffData[MAX_SEQ + 1];
		
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
					buffer[nextFrameToSend] = new BuffData(data);
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
						stopTimer(ackExpected);
						ackExpected = inc(ackExpected);
						//release timer
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
					Thread.sleep(1000);
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
			if(networkLayer.hasItem())
				return 1;
			else if(physicalLayer.hasPacket())
				return 2;
			else if(Thread.currentThread().interrupted())
				return 4;
			else if(!physicalLayer.isConnected())
				return -1;
			
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
