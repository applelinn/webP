import java.net.*;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;

public class WebProxy {
	/** Port for the proxy */
	private static int port;

	/** Socket for client connections */
	private static ServerSocket socket;
	

	public static void main(String args[]) { //throws IOException{
		/** Read command line arguments and start proxy */

		/** Read port number as command-line argument **/
		try
		{
			port = Integer.parseInt(args[0]);
		}
		catch (Exception e) // (NumberFormatException e)
		{
			System.out.println("Invalid inputs!");
			System.exit(0);
		}

		/** Create a server socket, bind it to a port and start listening **/
		try {
			socket = new ServerSocket(port);
		} catch (IOException e1) {
			System.out.println("Error opening port!");
		}

		/** Main loop. Listen for incoming connections **/
		Socket client = null;

		while (true) {

			String firstLine, method, URL, version;
			Scanner inClient = null;

			try {
				client = socket.accept();
				System.out.println("'Received a connection from: " + client);

				/** Read client's HTTP request **/
				inClient = new Scanner(client.getInputStream());

				firstLine = inClient.nextLine();
				String[] temp = firstLine.split(" ");
				method = temp[0];
				URL = temp[1];
				version = temp[2];

			} 
			catch (IOException e) {
				System.out.println("Error reading request from client: " + e);
				/* Definitely cannot continue, so skip to next
				 * iteration of while loop. */
				continue;
			}

			String[] URI = URL.split("/");
			String host = URI[2];
			int serverPort = 80;
			if(host.contains(":"))
			{
				URI = host.split(":");
				host = URI[0];
				serverPort = Integer.parseInt(URI[1]);
			}

			/** Check cache if file exists **/
			String fileName;
			fileName = URL.replaceAll(":", " ");
			fileName = fileName.replaceAll("/", " ");
			fileName = fileName.replace('.', ',');
			File f = new File(fileName); 
			if (method.compareTo("POST") != 0 && f.exists()) 
			{
				try
				{
					// Read the file 
					FileInputStream frF = new FileInputStream(f);
					//		BufferedInputStream frFb = new BufferedInputStream (frF);
					Scanner frS = new Scanner (f);
					
					File tempFile = new File("temp");
					tempFile.delete();
					tempFile.createNewFile();
					PrintWriter toTempFile = new PrintWriter(tempFile);
					PrintWriter frCache = new PrintWriter(client.getOutputStream());

					//  generate appropriate respond headers ie change the cache file
					// the date!
					String tempCache = frS.nextLine();
					while(!tempCache.toLowerCase().contains("date:"))
					{
						toTempFile.println(tempCache);
						tempCache = frS.nextLine();
						
					}

					//get the date here
					String dateHeader[] = tempCache.split(":");
					String date = dateHeader[1]+ ":" + dateHeader[2] + ":" +dateHeader[3];
					
					//send to the server header
					PrintWriter toServer = null;
					/** connect to server and relay client's request **/
					Socket server = null;
					try 
					{
						server = new Socket(host, serverPort);
					} 
					catch (Exception e) 
					{
						// catch the 502 error here
						PrintWriter printError = new PrintWriter(client.getOutputStream());
						printError.println("HTTP/1.0 502 Bad Gateway");
						//					printError.println("Date: Fri, 25 Sep 2015 11:05:00 GMT");
						printError.println("Connection: close");
						printError.println();
						printError.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
						printError.println("<html><head>");
						printError.println("<title>502 Bad Gateway</title>");
						printError.println("</head><body>");
						printError.println("<h1>Bad Gateway</h1>");
						printError.println("<p>Could not connect to server.</p>");
						printError.println("</body></html>");
						printError.println();
						printError.flush();
						
						continue;
					}

						toServer = new PrintWriter(server.getOutputStream());
		//test2				toServer = new PrintWriter(new File ("test2"));

					String requestMsg = method + " " + URL + " " + version + "\n";
					toServer.print(requestMsg);

					//print headers stored in scanner inClient
					String tmp1 = inClient.nextLine();
					while(tmp1.length() != 0)
					{
						{
							toServer.println(tmp1);
							tmp1 = inClient.nextLine();
						}
					}
					toServer.println("If-Modified-Since:" + date);
					toServer.println();
					toServer.flush();

					//receive response from server
					Scanner	frServer = new Scanner(server.getInputStream());
					String tempFS = frServer.nextLine();
					boolean isCache = false;
					if(tempFS.contains("304"))
					{
						isCache = true;
						
					}
					
					if(isCache)
					{
						//send the stream as usual		
						
						//copy stuff fr tempFile into client stream
						Scanner frTempF = new Scanner (tempFile);
						while(frTempF.hasNextLine())
						{
							frCache.println(frTempF.nextLine());
						}
						frTempF.close();
						//		Calendar cal = Calendar.getInstance();
						SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
						sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
						tempCache = "Date: " + sdf.format(new Date());
						
						frCache.println(tempCache);
						//feed into client to the end
						while(frS.hasNextLine())
						{
							frCache.println(frS.nextLine());
						}
						frCache.flush();
						frCache.close();
					}
					else
					{
						//send new stream to client n cache new part
						f.createNewFile();
						f.delete();
						f.createNewFile();
						FileOutputStream fos = new FileOutputStream (f);
						PrintWriter toFile = new PrintWriter(fos);
						//send scanner into client into n file toFile
						toFile.println(tempFS);
						frCache.println(tempFS);
						while(frServer.hasNextLine())
						{
							tempFS = frServer.nextLine();
							toFile.println(tempFS);
							frCache.println(tempFS);
						}
						frCache.flush();
						frCache.close();
						toFile.flush();
						toFile.close();
					}

					client.close();	
					frF.close();
					frS.close();
					server.close();
					frServer.close();
					toTempFile.close();
					

					continue;
				} catch (IOException e)
				{
					System.out.println("Cache IO error");
				} 
			}
		
		else {
			PrintWriter toServer = null;
			InputStream inServer = null;

			//		Socket server = null;
			try {
				/** connect to server and relay client's request **/
				Socket server = null;
				try {
					server = new Socket(host, serverPort);
				} catch (Exception e) {
					// catch the 502 error here
					PrintWriter printError = new PrintWriter(client.getOutputStream());
					printError.println("HTTP/1.0 502 Bad Gateway");
					//					printError.println("Date: Fri, 25 Sep 2015 11:05:00 GMT");
					printError.println("Connection: close");
					printError.println();
					printError.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
					printError.println("<html><head>");
					printError.println("<title>502 Bad Gateway</title>");
					printError.println("</head><body>");
					printError.println("<h1>Bad Gateway</h1>");
					printError.println("<p>Could not connect to server.</p>");
					printError.println("</body></html>");
					printError.println();
					printError.flush();
					continue;
				}

				toServer = new PrintWriter(server.getOutputStream());
				//test2		toServer = new PrintWriter(new File ("test2"));

				String requestMsg = method + " " + URL + " " + version + "\n";
				toServer.print(requestMsg);

				//print headers stored in scanner inClient
				String tmp1 = inClient.nextLine();
				while(tmp1.length() != 0)
				{
					{
						toServer.println(tmp1);
						tmp1 = inClient.nextLine();
					}
				}
				toServer.println();

				if(method.compareTo("POST")==0)
				{
					client.shutdownInput();
					tmp1 = inClient.next();
					toServer.println(tmp1);
				}
				toServer.flush();

				/** Get response from server **/
				inServer = new BufferedInputStream(server.getInputStream());
				
				//send file to user n file
				BufferedOutputStream into = new BufferedOutputStream(client.getOutputStream());
				f.createNewFile();
				FileOutputStream fos = new FileOutputStream (f);
				BufferedOutputStream toFile = new BufferedOutputStream(fos);
				byte[] b = new byte[40960];
				int length;
				length = inServer.read(b);
				while (length > 0) {
					into.write(b, 0, length);
					toFile.write(b,0,length); //send to file
					length = inServer.read(b);
				}
				into.flush();
				toFile.flush();

				inServer.close();

				server.close();
				client.close();
				into.close();
				toFile.close();
			}
			catch (IOException e) {

			} finally {
				//close stuff
				inClient.close();

			}
		}

	}
	}
}
