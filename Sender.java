import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

public class Sender {

    private final static int PORT = 1300;

    public static void main(String[] args) {
    	ByteBuffer headerBB;
    	File in=null;
		int packetSize = Integer.parseInt(JOptionPane.showInputDialog(null, "Enter Packet Size: "));
		int timeOut = Integer.parseInt(JOptionPane.showInputDialog(null, "Enter Timeout: "));
		double bad = Double.parseDouble(JOptionPane.showInputDialog(null, "Enter Droprate: "));
		String requestStatus="";
		int ackerr;
		int ackno;
		String ackStatus ="";

    	//filechooser as substitue for cmd
    	JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
		int returnValue = jfc.showSaveDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			in = jfc.getSelectedFile();
		}

		long inLength = in.length();

        try (DatagramSocket socket = new DatagramSocket(14)) {
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	DataOutputStream dos = new DataOutputStream(baos);

        	byte[] data;
        	byte[] img = Files.readAllBytes(in.toPath());
        	byte[] buf = new byte[packetSize+20];
            InetAddress host = InetAddress.getByName("localhost");
            int offset = 0;
            int packetLength = (int) Math.ceil(inLength/12.0);
            DatagramPacket request;

{
            	int counter=0;
        		while(offset + packetSize < in.length()) {
        		buf=null;
        		counter++;
        		offset = packetSize*(counter-1);

        		//get data from image file, if last packet then just get remaining data instead of full packet
        		if(inLength <= offset + packetSize-1)
        			{data = Arrays.copyOfRange(img, offset, (int) inLength);}
        		else
        			{data = Arrays.copyOfRange(img, offset, offset+packetSize);}

        		//add header
    			dos.writeInt(counter);
    			dos.writeInt(offset);
    			dos.writeLong(inLength);

 
        		requestStatus = dropCheck(bad);
        		
        		if(requestStatus.equals("DROP")) {
                    dos.writeInt(-1);
         
        		}
        		else if (requestStatus.equals("SENT"))
            		dos.writeInt(0);
        		else if(requestStatus.equals("ERRR"))
        			dos.writeInt(1);
        		
        		dos.write(data);
        		buf = baos.toByteArray();
      

        		//create packet and send request
        		if(inLength <= offset + packetSize-1)
            		request = new DatagramPacket(buf, (int) inLength-offset+20,host, PORT);
        		else
            		request = new DatagramPacket(buf, packetSize+20, host, PORT); // packetSize+20 breaks code

        		long time = System.nanoTime();
                socket.send(request);
                time = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - time);

                System.out.println("SENDing " + counter +  " " + offset + ":" + (offset+data.length-1) + " "+ time +" " + requestStatus);
                
        		socket.setSoTimeout(timeOut);

        		while(true) {
        			try{
        				DatagramPacket ack = new DatagramPacket(new byte[8], 8);
            			socket.receive(ack);

            			//parsing ack
                        headerBB = ByteBuffer.wrap(ack.getData());

                        ackno = headerBB.getInt();
                        ackerr = headerBB.getInt();

                        if (ackerr == 1) {
                        	ackStatus = "ErrAck";
                        }
                        else {
                        	ackStatus = "MoveWnd";
            
                        }

                        if(ackerr != -1) {
	            			System.out.println("AckRcvd " + ackno + " " + ackStatus);
	        				if(ackStatus.equals("MoveWnd")){
	        					break;
	        				}
                        }
        			}
        			//logging timeout
        			catch (SocketTimeoutException e) {
        				System.out.println("TimeOut: " + counter);
        			}
        			
        			//recreating datagram packet for resend
        			baos.reset();
        			dos.writeInt(counter);
        			dos.writeInt(offset);
        			dos.writeLong(inLength);

            		requestStatus = dropCheck(bad);
            		
            		if(requestStatus.equals("DROP")) {
                        System.out.println("SENDing " + counter +  " " + offset + ":" + (offset+data.length-1) + " " + requestStatus);
                        dos.writeInt(-1);
                        //continue;
            		}
            		else if (requestStatus.equals("SENT"))
                		dos.writeInt(0);
            		else if(requestStatus.equals("ERRR"))
            			dos.writeInt(1);
            		
            		dos.write(data);
            		buf = baos.toByteArray();

            		//recreate packet and resend request
            		if(inLength <= offset + packetSize-1)
                		request = new DatagramPacket(buf, (int) inLength-offset+20,host, PORT);
            		else
                		request = new DatagramPacket(buf, packetSize+20, host, PORT); // packetSize+20 breaks code

        			//resending for timeout and err ack
        			time = System.nanoTime();
        			socket.send(request);
        			time = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - time);
        			//logging resend
        			System.out.println("ReSend " + counter +  " " + offset + ":" + (offset+data.length-1) + " "
                    		+ time + " " + requestStatus);
        		}
      
        		baos.reset();
        		}
        	}
        } catch (IOException ex) {
            ex.printStackTrace();
        }
}

    private static String dropCheck (double bad) {
    	double badCheck = Math.random();
		if (badCheck < bad/2)
			return "DROP";
		else if (badCheck < bad)
			return "ERRR";
		else
			return "SENT";
    }

}