package server;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import datastructure.CustomQueue;
import frame.Frame;
import protocol.Protocol4;
import protocol.Protocol5;
import protocol.Protocol6;


public class DefaultSocketServer extends Thread implements SocketClientInterface, SocketClientConstants, Networking
{
	private CustomQueue<Frame> queue = new CustomQueue<Frame>(); 

	private ObjectInputStream in;
    private ObjectOutputStream out;

    private Socket sock;
    private String strHost;
    private int iPort;
    private boolean connected = false;

    
    public DefaultSocketServer(String strHost, int iPort) 
    {       
        setPort (iPort);
        setHost (strHost);
    }//constructor

    public DefaultSocketServer(int iPort, Socket s) 
    {   
        setPort (iPort);
        sock = s;
        setHost(sock.getInetAddress().toString());
    }//constructor

    public void run()
    {
       if (openConnection())
       {
    	   connected = true;
    	   handleSession();
            closeSession();
       }
       
    }//run
    
    public boolean openConnection()
    {
    	try 
    	{
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream()); 
    	}
    	catch (Exception e)
    	{
    	    if (DEBUG) 
                postError("Unable to obtain stream to/from " + strHost);
    	    return false;
    	}
    	  return true;
    }

    /*
        Method handles server processes and sends information to client.
        Method also recives information from client and acts accordingly to 
        the clients response
    */
    public void handleSession()
    {
    	if (DEBUG) 
            System.out.println ("Handling session with " + strHost + ":" + iPort);

    	try
    	{
    		int p = (int)this.getInputStream();
    		int size = (int)this.getInputStream();
    		
    		Thread t = new Thread(new Runnable(){

    			
				@Override
				public void run() {
					// TODO Auto-generated method stub
					while(true)
					{
						if(Thread.interrupted())
							break;
						
						try {
							Frame f = (Frame)getInputStream();
							if(f == null)
							{
								connected = false;
								break;
							}
							queue.enqueue(f);
						}
						catch(Exception e)
						{
							postError(e.toString());
							break;
						}
						
					}
				}
    			
    		});
    		t.start();
    		if(p != 6)
    			new Protocol5(this, size);
    		t.interrupt();
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
 
    		return;
    	}
    	
      
    }       

    public void closeSession()
    {
       try 
       {
            out = null;
            in = null;
            postMessage("Closing socket to " + strHost);
            sock.close();
            this.stop();
       }
       catch (IOException e)
       {
            if (DEBUG) 
                postError("Error closing socket to " + strHost);
       }       
    }

    private void setHost(String strHost)
    {
            this.strHost = strHost;
    }

    private void setPort(int iPort)
    {
            this.iPort = iPort;
    }
    
    public void setOutputStream(int code, Object object)
    {
        try 
        {
            out.writeObject(code);
            out.writeObject(object);
        } 
        catch (IOException ex) 
        {
            //Logger.getLogger(ServerSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setOutputStream(Object object)
    {
        try 
        {
        	
            out.writeObject(object);
            
        } 
        catch (IOException ex) 
        {
            //Logger.getLogger(ServerSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Object getInputStream() 
    {
        try
        {
            return in.readObject();
        }
        catch(IOException e)
        {
            //Logger.getLogger(ServerSocket.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
        catch(ClassNotFoundException e)
        {
            //Logger.getLogger(ServerSocket.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
        
        
    }
    
    public int getInputStreamCode()
    {
        try
        {
            return (int)in.readObject();
        }
        catch(IOException e)
        {
            //Logger.getLogger(ServerSocket.class.getName()).log(Level.SEVERE, null, e);
            return -1;
        }
        catch(ClassNotFoundException e)
        {
            //Logger.getLogger(ServerSocket.class.getName()).log(Level.SEVERE, null, e);
            return -1;
        }
    }
    
    public void postMessage(String message)
    {
        final String TALKING = "[Server/" + strHost + "]: ";
        
        System.out.println(TALKING + message);
    }
    
    public void postError(String message)
    {
        final String TALKING = "[Server/" + strHost + "]: ";
        
        System.err.println(TALKING + message);
    }

	@Override
	public Object getPacket() {
		// TODO Auto-generated method stub
		Object packet = null;

		while(true)
		{
			try 
			{
				if(packet == null)
					packet = queue.dequeue();
				break;
			} 
			catch (InterruptedException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return packet;
		/*
		Thread curr = Thread.currentThread();
		while(true)
		{
			if(curr.interrupted())
				return null;
			
			try
			{
				Object packet = queue.dequeue();
				if(packet != null)
					return packet;
			}
			catch(InterruptedException e)
			{
				//e.printStackTrace();
				return null;
			}
			
		}
		*/
	}

	@Override
	public boolean hasPacket() {
		// TODO Auto-generated method stub
		return !queue.hasItem();
	}
	
	public boolean isConnected()
	{
		return connected;
	}

}// class DefaultSocketClient
