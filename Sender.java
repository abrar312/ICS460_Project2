import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

public class Sender {

    private final static int PORT = 13;

    public static void main(String[] args) {
    	ByteBuffer headerBB;
    	File in=null;
		double bad = 0.20;
		String requestStatus="";
		int ackerr;
		int ackno;
		int packetSize = 1024;
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

            
            /*
            //for images of smaller size, split them into 12 packets still, packet size = img size/12 + header
            if (inLength < packetSize*12) {
            	for (int counter=1;counter<=12;counter++)
            	{
            		buf=null;
            		offset = packetLength*(counter-1);

            		//get data from image file, if last packet then just get remaining data instead of full packet
            		if(inLength <= offset + packetLength - 1)
            			data = Arrays.copyOfRange(img, offset, (int) inLength);
            		else
            			data = Arrays.copyOfRange(img, offset, offset+packetLength);

            		//add header for packet
            		dos.writeInt(counter);
            		dos.writeInt(offset);
            		dos.writeLong(inLength);
            		dos.write(data);
            		buf = baos.toByteArray();


            		System.out.println(counter + "-" + offset+ "-" +(offset+data.length-1));

            		//create packet and send request
            		if(inLength <= offset + packetLength - 1)
                		request = new DatagramPacket(buf, (int) inLength-offset+20,host, PORT);
            		else
            			request = new DatagramPacket(buf, packetLength+20,host, PORT);

            		long time = System.nanoTime();
            		socket.send(request);
            		//logging message for smaller files
            		System.out.println("SENDing " + counter +  " " + offset + ":" + (offset+data.length-1) + " "
            		+ TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - time) + " " + dropCheck(bad));

            		socket.setSoTimeout(2000);

            		//receiving ack and resending if drop
            		while(true) {
            			try{
            				DatagramPacket ack = new DatagramPacket(new byte[8], 8);
	            			socket.receive(ack);

	            			//parsing ack
	                        headerBB = ByteBuffer.wrap(ack.getData());
	                        
	                        ackno = headerBB.getInt();
	                        ackerr = headerBB.getInt();
	                        
	                        System.out.println("ackerr: "+ackerr+"- ackno:"+ackno+" counter:"+counter);

	                        if (ackerr == 1) {
	                        	ackStatus = "ErrAck";
	                        }
	                        else if(ackno != counter) {
		                        	ackStatus = "DuplAck";
	                        }	                        
	                        else {
	                        	ackStatus = "MoveWnd";
	                        }

	                        //logging ack
	            			System.out.println("AckRcvd " + ackno + " " + ackStatus);
            				if(ackStatus.equals("MoveWnd")){
            					break;
            				}
            			}
            			//logging timeout
            			catch (SocketTimeoutException e) {
            				System.out.println("TimeOut: " + counter);
            			}

            			//resend for timeout and err acks
            			time = System.nanoTime();
            			socket.send(request);
            			//logging resend message
            			System.out.println("ReSend " + counter +  " " + offset + ":" + (offset+data.length-1) + " "
                        		+ TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - time) + " " + dropCheck(bad));
            		}

            		baos.reset();
            	}
            }

            //for images of larger size, just use packet size of 1024 bytes of data + header
        	else */{
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

    			//---------------------------this through "end" needs to be implemented for smaller code
        		requestStatus = dropCheck(bad);
        		
        		if(requestStatus.equals("DROP")) {
                    //System.out.println("SENDing " + counter +  " " + offset + ":" + (offset+data.length-1) + " " + requestStatus);
                    dos.writeInt(-1);
                    //counter--;
        		}
        		else if (requestStatus.equals("SENT"))
            		dos.writeInt(0);
        		else if(requestStatus.equals("ERRR"))
        			dos.writeInt(1);
        		//----------------end
        		
        		dos.write(data);
        		buf = baos.toByteArray();
        		
        		//logging prog 1
        		//System.out.println(counter + "-" + offset +"-"+(offset+data.length-1));

        		//create packet and send request
        		if(inLength <= offset + packetSize-1)
            		request = new DatagramPacket(buf, (int) inLength-offset+20,host, PORT);
        		else
            		request = new DatagramPacket(buf, packetSize+20, host, PORT); // packetSize+20 breaks code

        		long time = System.nanoTime();
                socket.send(request);
                time = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - time);
                //------------new logging needs to be implemented for smaller file
                //logging send message for larger files
                System.out.println("SENDing " + counter +  " " + offset + ":" + (offset+data.length-1) + " "+ time +" " + requestStatus);
                
        		socket.setSoTimeout(2000);

        		//-----------------this through "end" needs to be implemented for smaller files
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
                        	//counter++;
                        }

                        //loggin ack
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
        		//------------------------end
        		//counter++;
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