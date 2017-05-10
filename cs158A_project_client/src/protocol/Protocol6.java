package protocol;

import java.nio.charset.Charset;
import java.util.ArrayList;

import datastructure.CustomQueue;
import frame.Frame;
import layers.NetworkLayer;
import server.Networking;

public class Protocol6 implements Runnable{

	int MAX_SEQ = 3;
	int BUFF_SIZE = ((MAX_SEQ + 1) / 2);
	
	
	int nextFrameToSend;
	int frameExpected;
	int oldestFrame = MAX_SEQ + 1;
	int nBuffered = 0;
	boolean noNak = true;
	Networking physicalLayer;
	NetworkLayer networkLayer;
	Thread[] timers;
	Thread ackTimer;
	
	CustomQueue<Integer> timeouts = new CustomQueue<Integer>();
	
	long executionTime = 0;
	long startTime = 0;
	long totalTime = 0;
	
	
	public Protocol6(Networking physicalLayer, int maxSeq)
	{
		this.MAX_SEQ = maxSeq;
		BUFF_SIZE = ((MAX_SEQ + 1) / 2);
		oldestFrame = MAX_SEQ + 1;
		timers = new Thread[MAX_SEQ + 1];
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
	
		s = new Frame(fk, frameNum, fe, data);
		
		if(fk.equals(Frame.NAK) )
			noNak = false;
		

		
		
		if((int)(Math.random() * 100) <= 100)
		{
			physicalLayer.setOutputStream(s);
			System.out.println("resending Sent Frame: " + s);
			
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
		nBuffered = 0;
		int ackExpected = 0;
		int tooFar = BUFF_SIZE;
		
		
		BuffData[] outBuff = new BuffData[BUFF_SIZE];
		BuffData[] inBuff = new BuffData[BUFF_SIZE];
		Boolean[] arrived = new Boolean[BUFF_SIZE];
		
		for(int i = 0; i < arrived.length; i++)
			arrived[i] = false;
		
		System.out.println("Starting Protocol6 (Client)");
		startTime = System.currentTimeMillis();
		networkLayer.start();
		long endTime = 0;
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
					nBuffered += 1;
					byte[] data = networkLayer.getData();
					outBuff[nextFrameToSend % BUFF_SIZE] = new BuffData(data);
					sendData(Frame.DATA, nextFrameToSend, frameExpected, outBuff);
					nextFrameToSend = inc(nextFrameToSend);
					
					break;
				case 2: //Frame arrival
					Frame r = (Frame)physicalLayer.getPacket();
					
					System.out.println("Recieved: " + r);
					if(r.getKind().equals(Frame.DATA))
					{
						
						if( (r.getSeq() != frameExpected) && noNak)
							sendData(Frame.NAK, r.getSeq(), frameExpected, outBuff/*outBuff[r.getSeq()].getBuff()*/);
						/*else
							System.out.println("Start Ack Timer arrived:" + arrived[r.getSeq() % BUFF_SIZE]);*/
						
						
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
						
						
						
						
					}
					
					if( (r.getKind().equals(Frame.NAK) ) && inBetween(ackExpected, (r.getAck() + 1) % (MAX_SEQ+1), nextFrameToSend))
					{
						System.out.println("resending frame with ack: " + (r.getAck() + 1) % (MAX_SEQ+1));
						//stopTimer(r.getSeq() % BUFF_SIZE);
						sendDataNoFail(Frame.DATA, (r.getAck() + 1) % (MAX_SEQ+1), frameExpected, r.getBuff());
					}
					
					while(inBetween(ackExpected, r.getAck(), nextFrameToSend))
					{
						nBuffered -= 1;
						stopTimer(ackExpected);
						ackExpected = inc(ackExpected);
						
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
		this.executionTime += (endTime - startTime);
		System.out.println("Time took to complete protocol in milliseconds: " + executionTime + " Time in idle: " + totalTime);
		
		
		
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
	
	/**
	 * Creates and starts the timer for a specific frame. When
	 * interrupt is set the seq number is sent to the timeout queue. 
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
					timeouts.enqueue(s);
					curr.interrupt();
					
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					
				}
				
			}
			
		});
		
		if(timers[seq] == null)
		{
			timers[seq ] = t;
		}
		else
		{
			
			
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
	
	
	/**
	 * Stops a timer for a specific frame
	 * @param seq frame number you want to remove timer for
	 */
	public void stopTimer(int seq)
	{
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
			else if(networkLayer.isFinished() && nBuffered == 0)
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
