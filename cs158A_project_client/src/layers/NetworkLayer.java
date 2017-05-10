package layers;

import datastructure.CustomQueue;

public class NetworkLayer extends Thread
{
	final String MESSAGE = "This is going to be a big string"; //message to send
	int frameSize = 2; //in bit
	private CustomQueue<byte[]> queue = new CustomQueue<byte[]>();
	boolean isDisabled = false;
	boolean finishedRunning = false;
	
	public NetworkLayer()
	{
		
	}
	
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		byte[] data = MESSAGE.getBytes();
		int byteIndex = 0;
		
		//Creates a byte[] depending on the frame size
		while(byteIndex < data.length)
		{
			int temp = frameSize;
			if(byteIndex + temp < data.length)
			{
				byte[] dataToSend = new byte[temp];
				for(int i = 0; i <dataToSend.length; i++)
				{
					dataToSend[i] = data[byteIndex++];
				}
				queue.enqueue(dataToSend);
				
				
			}
			else
			{
				byte[] dataToSend = new byte[data.length - byteIndex];
				for(int i = 0; i < dataToSend.length;i++)
				{
					dataToSend[i] = data[byteIndex++];
				}
				queue.enqueue(dataToSend);
				
			}
		}
		
		finishedRunning = true;
	}

	
	/**
	 * Checks to see if the the network layer
	 * has an item that needs to be sent
	 * @return true if there is an item in the queue.
	 * False if no item is in the queue or layer is
	 * currently disabled. 
	 */
	public boolean hasItem()
	{
		return !queue.isEmpty() && !isDisabled;
	}
	
	/**
	 * Gets data from queue
	 * @return Returns byte[] containing data that needs to be sent
	 * or null if an error occurs. 
	 */
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