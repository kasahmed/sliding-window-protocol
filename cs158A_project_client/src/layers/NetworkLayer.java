package layers;

import datastructure.CustomQueue;

public class NetworkLayer extends Thread
{
	final String MESSAGE = "This is going to be a big string";//"Hello World";
	int frameSize = 1; //in bits
	private CustomQueue<byte[]> queue = new CustomQueue<byte[]>();
	boolean isDisabled = false;
	boolean finishedRunning = false;
	
	public NetworkLayer()
	{
		this.start();
	}
	
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
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
		
		finishedRunning = true;
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
	
	public boolean isFinished()
	{
		return (finishedRunning) && !isDisabled;
	}
	
	
	
}