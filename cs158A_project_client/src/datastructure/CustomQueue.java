package datastructure;

import sun.misc.Queue;

public class CustomQueue<T> {
	
	private Queue<T> q = new Queue<T>();
	
	public synchronized  void enqueue(T item)
	{
		q.enqueue(item);
	}
	
	public synchronized T dequeue() throws InterruptedException
	{
		if(q.isEmpty())
			return null;
		return q.dequeue();
		
	}
	
	public synchronized boolean isEmpty()
	{
		return q.isEmpty();
	}

}
