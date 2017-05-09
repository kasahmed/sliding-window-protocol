package protocol;

import java.nio.charset.Charset;
import java.util.ArrayList;

import datastructure.CustomQueue;
import frame.Frame;
import layers.NetworkLayer;
import server.Networking;

public class Protocol6 implements Runnable{

	final int MAX_SEQ = 3;
	final int BUFF_SIZE = ((MAX_SEQ + 1) / 2);
	
	
	int nextFrameToSend;
	int frameExpected;
	int oldestFrame = MAX_SEQ + 1;
	boolean noNak = true;
	Networking physicalLayer;
	NetworkLayer networkLayer;
	Thread[] timers = new Thread[MAX_SEQ + 1];
	Thread ackTimer;
	
	CustomQueue<Integer> timeouts = new CustomQueue<Integer>();
	
	public Protocol6(Networking physicalLayer)
	{
		this.physicalLayer = physicalLayer;
		
		this.run();
	}
	
	private void sendData(String fk, int frameNum, int frameExpected, BuffData[] data)
	{
		Frame s;
		
		int fe = (this.frameExpected + MAX_SEQ) % (MAX_SEQ + 1);
		
		s = new Frame(fk, frameNum, fe, data[frameNum % BUFF_SIZE].getBuff());
		
		if(fk.equals(Frame.NAK) )
			noNak = false;
		
		if((int)(Math.random() * 100) <= 100)
		{
			physicalLayer.setOutputStream(s);
			System.out.println("Sent Frame: " + s);
		}
		else
		{
			System.out.println("dropped Frame: " + s);
		}
		
		if(fk.equals(Frame.DATA))
			setTimer(frameNum);
		//stop_ack_timer?
		
	}
	
	private void sendDataNoFail(String fk, int frameNum, int frameExpected, byte[] data)
	{
		Frame s;
		
		int fe = (this.frameExpected + MAX_SEQ) % (MAX_SEQ + 1);
	
		s = new Frame(fk, frameNum, fe/*(frameExpected + MAX_SEQ) % (MAX_SEQ + 1)*/, data);
		
		if(fk.equals(Frame.NAK) )
			noNak = false;
		

		
		
		if((int)(Math.random() * 100) <= 100)
		{
			physicalLayer.setOutputStream(s);
			System.out.println("resending Sent Frame: " + s);
			//System.out.println("Sent Frame: " + s);
		}
		else
		{
			System.out.println("dropped Frame: " + s);
		}
		
		if(fk.equals(Frame.DATA))
			setTimer(frameNum);
		//stop_ack_timer?
		
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		networkLayer = new NetworkLayer();
		
		nextFrameToSend = 0;
		frameExpected = 0;
		oldestFrame = 0;
		int nBuffered = 0;
		int ackExpected = 0;
		int tooFar = BUFF_SIZE;
		
		
		BuffData[] outBuff = new BuffData[BUFF_SIZE];
		BuffData[] inBuff = new BuffData[BUFF_SIZE];
		Boolean[] arrived = new Boolean[BUFF_SIZE];
		
		for(int i = 0; i < arrived.length; i++)
			arrived[i] = false;
		
		System.out.println("Starting Protocol6 (Client)");
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
					sendData(Frame.DATA, nextFrameToSend, frameExpected, outBuff);
					nextFrameToSend = inc(nextFrameToSend);
					System.out.println("Next frame to send: " + nextFrameToSend);
					
					break;
				case 2: //Frame arrival
					Frame r = (Frame)physicalLayer.getPacket();
					
					System.out.println("Recieved: " + r);
					if(r.getKind().equals(Frame.DATA))
					{
						/*
						if( (r.getSeq() != frameExpected) && noNak)
							sendData(Frame.NAK, r.getSeq(), frameExpected, outBuff[r.getSeq()].getBuff());
						else
							System.out.println("Start Ack Timer arrived:" + arrived[r.getSeq() % BUFF_SIZE]);*/
						
						/*
						if(inBetween(frameExpected, r.getSeq(), tooFar) && (arrived[r.getSeq() % BUFF_SIZE] == false) )
						{
							arrived[r.getSeq() % BUFF_SIZE] = true;
							inBuff[r.getSeq() % BUFF_SIZE] = new BuffData(r.getBuff());
							while(arrived[frameExpected % BUFF_SIZE])
							{
								//Send to network layer
								noNak = true;
								arrived[frameExpected % BUFF_SIZE] = false;
								frameExpected = inc(frameExpected);
								tooFar = inc(tooFar);
								//start ack timer
							}
							
						}
						*/
						
						
						
					}
					
					if( (r.getKind().equals(Frame.NAK) ) && inBetween(ackExpected, (r.getAck() + 1) % (MAX_SEQ+1), nextFrameToSend))
					{
						System.out.println("resending frame with ack: " + (r.getAck() + 1) % (MAX_SEQ+1));
						//stopTimer(r.getSeq() % BUFF_SIZE);
						sendDataNoFail(Frame.DATA, (r.getAck() + 1) % (MAX_SEQ+1), frameExpected, r.getBuff());
					}
					
					while(inBetween(ackExpected, r.getAck(), nextFrameToSend))
					{
						//System.out.println("stopping timer");
						nBuffered -= 1;
						stopTimer(ackExpected);
						ackExpected = inc(ackExpected);
						//oldestFrame = inc(oldestFrame);
					}
					
					break;
				case 4: //timeout
					try {
						oldestFrame = timeouts.dequeue();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					sendData(Frame.DATA, oldestFrame, frameExpected, outBuff);
					break;
				
			}
			
			if(nBuffered < BUFF_SIZE)
				networkLayer.enableLayer();
			else
				networkLayer.disableLayer();
			
		}
		
		
	}
	
	private boolean inBetween(int a, int b, int c)
	{
		return ((a <= b) && (b < c)) || ((c < a) && (a <= b)) || ( (b < c) && (c < a));
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
		//int seq = realSeq % BUFF_SIZE;
		//System.out.println("Setting timer for: " + seq);
		Thread currThread = Thread.currentThread();
		Thread t =  new Thread(new Runnable(){

			private Thread curr = currThread;
			private int s = seq;
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(2000);
					timeouts.enqueue(s);
					curr.interrupt();
					
					//System.out.println("timeout: " + seq);
					
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					//System.out.println("Interrupted: " + seq);
				}
				
			}
			
		});
		
		if(timers[seq] == null)
		{
			System.out.println("Not overriding");
			timers[seq ] = t;
		}
		else
		{
			System.out.println("Overriding other timer");
			
			
			timers[seq].interrupt();
			timers[seq ] = null;
			timers[seq ] = t;
			
			
		}
		
		timers[seq].start();
	}
	
	public void startAckTimer()
	{
		//???
	}
	
	public void stopTimer(int seq)
	{
		//int seq = s % BUFF_SIZE;
		if(timers.length < seq)
		{
			System.out.println("seq: " + seq + " not found");
			return;
		}
		if(timers[seq] == null)
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
