package server;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import protocol.Protocol4;


public class DefaultSocketServer extends Thread implements SocketClientInterface, SocketClientConstants, Networking
{
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

    public void run()
    {
       if (openConnection())
       {
            handleSession();
            
            closeSession();
       }
       
    }//run
    
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
    	    e.printStackTrace();
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
    		new Protocol4(this);
    	}
    	catch(Exception e)
    	{
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
            Logger.getLogger(ServerSocket.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(ServerSocket.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(ServerSocket.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
        catch(ClassNotFoundException e)
        {
            Logger.getLogger(ServerSocket.class.getName()).log(Level.SEVERE, null, e);
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

}// class DefaultSocketClient
