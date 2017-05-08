

import server.DefaultSocketServer;

public class Client {
	
	public Client(String ip, int port)
	{
		startClient(ip, port);
	}

	public static void main(String args[]) {
		if(args.length < 1)
		{
			System.err.println("args has too few parameters\nNeeds:\n0 -IP\n1 port (int)");
			return;
		}
		
		int port = 0;
		try
		{
			port = Integer.parseInt(args[1]);
		}
		catch(NumberFormatException e)
		{
				e.printStackTrace();
		}
		
		String ipAddress = args[0];
		System.out.println("IP: " + ipAddress + " port: " + port);
		new Client(ipAddress, port);
	
		
		
	}
	
	private void startClient(String ip, int port)
	{
		new DefaultSocketServer(ip, port).start();
		
	}
}
