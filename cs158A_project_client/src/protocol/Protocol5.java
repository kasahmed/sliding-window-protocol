package protocol;


import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

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
	Thread[] timers = new Thread[MAX_SEQ + 1];
	public Protocol5(Networking physicalLayer)
	{
		this.physicalLayer = physicalLayer;
		
		this.run();
	}
	
	private void sendData(int frameNum, int frameExpected, byte[] data)
	{
		
		Frame s = new Frame(frameNum, (frameExpected + MAX_SEQ) % (MAX_SEQ + 1), data);

		
		if((int)(Math.random() * 100) <= 10)
		{
			physicalLayer.setOutputStream(s);
			//System.out.println("Sent Frame: " + s);
		}
		else
		{
			//System.out.println("dropped Frame: " + s);
		}
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
		ArrayList<byte[]> buffer = new ArrayList<byte[]>();
		
		
		System.out.println("Starting Protocol5 (Client)");
		while(true)
		{
			int event = getEvent();
			
			if(event == -1)
			{
				for(int i = 0; i  < timers.length; i++)
					stopTimer(i);
				break;
			}
			switch(event)
			{
				case 1 : //Network layer has frame
					
					byte[] data = networkLayer.getData();
					if(buffer.size() > nextFrameToSend)
						buffer.remove(nextFrameToSend);
					buffer.add(nextFrameToSend, data);
					sendData(nextFrameToSend, frameExpected, data);
					nextFrameToSend = inc(nextFrameToSend);
					nBuffered += 1;
					break;
				case 2: //Frame arrival
					Frame r = (Frame)physicalLayer.getPacket();
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
					//printBuffer(buffer);
					for(int i = 0; i < nBuffered; i++)
					{
						sendData(nextFrameToSend, frameExpected, buffer.get(nextFrameToSend));
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
	
	public void setTimer(int seq)
	{
		//System.out.println("Setting timer for: " + seq);
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
					//System.out.println("timeout: " + seq);
					
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					//System.out.println("Stopped: " + seq);
				}
				
			}
			
		});
		
		if(timers[seq] == null)
			timers[seq] = t;
		else
		{
			//System.out.println("Overriding other timer");
			
			timers[seq].interrupt();
			timers[seq] = null;
			timers[seq] = t;
			
		}
		
		timers[seq].start();
	}
	
	public void stopTimer(int seq)
	{
		if(timers.length < seq)
			return;
		timers[seq].interrupt();
		timers[seq] = null;
	}
	
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
	
	private void printBuffer(ArrayList<byte[]> buff)
	{
		for(int i = 0; i <  buff.size(); i++)
		{
			String message2 = new String(buff.get(i),  Charset.forName("UTF-8"));
			System.out.println("buffer" + i + ": " + message2);
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
			this.start();
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

