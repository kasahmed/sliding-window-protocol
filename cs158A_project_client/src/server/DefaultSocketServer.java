package server;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import datastructure.CustomQueue;
import frame.Frame;
import protocol.Protocol5;
import protocol.Protocol6;


public class DefaultSocketServer extends Thread implements SocketClientInterface, SocketClientConstants, Networking
{
	//queue to hold received frames
	private CustomQueue<Frame> queue = new CustomQueue<Frame>(); 
	
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private Socket sock;
    private String strHost;
    private int iPort;
    
    
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

    @Override
    public void run()
    {
       if (openConnection())
       {
            handleSession();
            
            closeSession();
       }
       
    }//run
    
    /**
     * Attempt to start the connection.
     * @return true if connection is successful, false
     * if connection has failed. 
     */
    public boolean openConnection()
    {
    	
    	try 
    	{
    	    sock = new Socket(strHost, iPort);
    	 
    	}
    	catch(IOException socketError)
    	 {
    	    if (DEBUG) 
                System.err.println("Unable to connect to " + strHost);
    	    socketError.printStackTrace();
    	    return false;
    	 }
    	try 
    	{
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream()); 
    	}
    	catch (Exception e)
    	{
    	    if (DEBUG) 
                System.err.println("Unable to obtain stream to/from " + strHost);
    	    //e.printStackTrace();
    	    return false;
    	}
    	  return true;
    }

    /**
        Method handles server processes and sends information to client.
        Method also receives information from client and acts accordingly to 
        the clients response
    */
    public void handleSession()
    {
    	if (DEBUG) 
            System.out.println ("Handling session with " + strHost + ":" + iPort);
        
    	try
    	{
    		int protocol = 0;
    		int windowSize = 1;
    		int dropRate = 0;
    		boolean gotProtocol = false;
    		Scanner scan = new Scanner(System.in);
    		
    		while(!gotProtocol)
    		{
    			
    			System.out.print("Enter the protocol you want to run(4,5,6): ");
    			String input = scan.nextLine();
    			try
    			{
    				protocol = Integer.parseInt(input);
    				if(protocol == 4 || protocol == 5 || protocol == 6)
    					gotProtocol = true;
    			}
    			catch(NumberFormatException e)
    			{
    				System.out.println("Input has to be an integer");
    			}
    		}
    		
    		if(protocol != 4)
    		{
    			boolean gotSize = false;
    			while(!gotSize)
    			{
    				System.out.print("Enter the MAX_SEQ you want: ");
        			String input = scan.nextLine();
        			try
        			{
        				windowSize = Integer.parseInt(input);
        				if(windowSize < 1)
        					System.out.println("MAX_SEQ has to be greater then 1");
        				else
        					gotSize = true;
        			}
        			catch(NumberFormatException e)
        			{
        				System.out.println("MAX_SEQ has to be an Integer");
        			}
    			}
    		}
    		
    		if(protocol != 6)
    		{
    			boolean gotRate = false;
    			while(!gotRate)
    			{
    				System.out.print("Enter the drop rate: ");
    				String input = scan.nextLine();
        			try
        			{
        				dropRate = Integer.parseInt(input);
        				if(dropRate < 0 || dropRate > 100)
        					System.out.println("MAX_SEQ has to be greater then -1 and less then 100");
        				else
        					gotRate = true;
        			}
        			catch(NumberFormatException e)
        			{
        				System.out.println("Has to be an Integer");
        			}
    			}
    		}
    		
    		this.setOutputStream(protocol);
    		this.setOutputStream(windowSize);
    		
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
								break;
							queue.enqueue(f);
						}
						catch(Exception e)
						{
							//postError(e.toString());
							break;
						}
						
					}
				}
    			
    		});
    		t.start();
    		if(protocol != 6)
    			new Protocol5(this, windowSize, dropRate);
    		else
    			new Protocol6(this, windowSize/*, dropRate*/);
    		t.interrupt();
    		
    	}
    	catch(Exception e)
    	{
    		//e.printStackTrace();
    		return;
    	}
        
    
    }       

    /**
     * Ends communication
     */
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
        catch (SocketTimeoutException e)
        {
        	System.out.println("Time_out");
        	return null;
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
				//e.printStackTrace();
			}
		}
		
		return packet;
		
	}

	@Override
	public boolean hasPacket() {
		// TODO Auto-generated method stub
		return !queue.isEmpty();
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return sock.isConnected();
	}

}// class DefaultSocketClient
