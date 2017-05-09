package protocol;

import java.nio.charset.Charset;
import java.util.ArrayList;

import datastructure.CustomQueue;
import frame.Frame;
import server.Networking;

public class Protocol6 implements Runnable{

	final int MAX_SEQ = 3;
	final int BUFF_SIZE = ((MAX_SEQ + 1) / 2);
	
	
	int nextFrameToSend;
	int frameExpected;
	boolean noNak = true;
	Networking physicalLayer;
	NetworkLayer networkLayer;
	Thread[] timers = new Thread[BUFF_SIZE + 1];
	Thread ackTimer;
	boolean ackTimeout = false;
	
	public Protocol6(Networking physicalLayer)
	{
		this.physicalLayer = physicalLayer;
		
		this.run();
	}
	
	private void sendData(String fk, int frameNum, int frameExpected, byte[] data)
	{
		Frame s;
		
		s = new Frame(fk, frameNum, (frameExpected + MAX_SEQ) % (MAX_SEQ + 1), data);
		
		if(fk.equals(Frame.NAK))
			noNak = false;
		
		physicalLayer.setOutputStream(s);
		//System.out.println("Sent: " + s);
		
		if(fk.equals(Frame.DATA))
			setTimer(frameNum % BUFF_SIZE);
		//stop_ack_timer?
		
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		networkLayer = new NetworkLayer();
		
		nextFrameToSend = 0;
		frameExpected = 0;
		int nBuffered = 0;
		int ackExpected = 0;
		int tooFar = BUFF_SIZE;
		
		BuffData[] outBuff = new BuffData[BUFF_SIZE];
		BuffData[] inBuff = new BuffData[BUFF_SIZE];
		Boolean[] arrived = new Boolean[BUFF_SIZE];
		
		for(int i = 0; i < arrived.length; i++)
			arrived[i] = false;
		
		System.out.println("Starting Protocol6 (Server)");
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
					nBuffered += 1;
					byte[] data = networkLayer.getData();
					outBuff[nextFrameToSend % BUFF_SIZE] = new BuffData(data);
					sendData(Frame.ACK, nextFrameToSend, frameExpected, data);
					nextFrameToSend = inc(nextFrameToSend);
					break;
				case 2: //Frame arrival
					Frame r = (Frame)physicalLayer.getPacket();
					
					if(r.getKind().equals(Frame.DATA))
					{
						
						if( (r.getSeq() != frameExpected) && noNak) {
							sendData(Frame.NAK, 0, frameExpected, r.getBuff());
							System.out.println("Error with frame r.seq: " + r.getSeq() + " Expected: " + frameExpected);
						}
							
						/*else
							System.out.println("Start Ack Timer");*/
							
						
						if(inBetween(frameExpected, r.getSeq(), tooFar) && (arrived[r.getSeq() % BUFF_SIZE]) == false)
						{
							arrived[r.getSeq() % BUFF_SIZE] = true;
							inBuff[r.getSeq() % BUFF_SIZE] = new BuffData(r.getBuff());
							System.out.println("Recieved: " + r);
							if(arrived[frameExpected % BUFF_SIZE])
							{
								//Send to network layer
								
								networkLayer.sendData(r.getBuff());
								noNak = true;
								arrived[frameExpected % BUFF_SIZE] = false;
								frameExpected = inc(frameExpected);
								tooFar = inc(tooFar);
								//start ack timer
							}
							
						}
						
					}
					
					if( (r.getKind().equals(Frame.NAK) ) && inBetween(ackExpected, (r.getAck() + 1) % (MAX_SEQ+1), nextFrameToSend))
					{
						
						sendData(Frame.DATA, r.getSeq(), frameExpected, outBuff[(r.getAck() + 1) % (MAX_SEQ+1) % BUFF_SIZE].getBuff());
					}
					
					/*
					while(inBetween(ackExpected, r.getAck(), nextFrameToSend))
					{
						nBuffered -= 1;
						stopTimer(ackExpected % BUFF_SIZE);
						ackExpected = inc(ackExpected);
					}
					*/
					break;
					
				case 4: //timeout
					
					break;
				case 5 : //ack_timeout
					
					break;
			}
			
			/*
			if(nBuffered < BUFF_SIZE)
				networkLayer.enableLayer();
			else
				networkLayer.disableLayer();
				*/
			
		}
		
		
	}
	
	private boolean inBetween(int a, int b, int c)
	{
		return ((a <= b) && (b < c)) || ((c < a) && (a <= b)) || ( (b < c) && (c < a));
		/*
		if( ((a <= b) && (b < c)) || ((c < a) && (a <= b)) || ( (b < c) && (c < a)))
			return true;
		return false;
		*/
	}
	
	public int inc(int value)
	{
		value++;
		
		if(value > BUFF_SIZE)
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
			//this.start();
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
