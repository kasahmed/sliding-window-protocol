import java.io.IOException;
import java.net.ServerSocket;

import server.DefaultSocketServer;

public class Server {

	final static int PORT = 4444;
	
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        // TODO code application logic here
        new Server().startServer();
        
    }
    
    public void startServer()
    {
        ServerSocket serverSocket= null;
        
        try
        {
            serverSocket = new ServerSocket(PORT);
        }
        catch(IOException e)
        {
            System.err.println("Could not listen on port: " + PORT + ".");
            System.exit(1);
        }
        
        
        try
        {
            while(true)
            {
                DefaultSocketServer server = new DefaultSocketServer(PORT, serverSocket.accept());
                server.start();
            }
        }
        catch(IOException e)
        {
            System.err.println("Accept failed.");
            System.exit(2);
        }
    }

}
