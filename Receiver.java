import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.logging.*;

public class Receiver {

    private final static int PORT = 13;
    private final static Logger audit = Logger.getLogger("requests");
    private final static Logger errors = Logger.getLogger("errors");

    public static void main(String[] args) {
    	File output = new File("test.jpg");
    	SortedMap <Integer, byte[]> serverByte = new TreeMap <>();
    	byte[] data;
    	int sequence;
    	int offset;
    	int packetSize = 1024;
    	long inLength;
    	boolean last = false;
    	boolean complete = false;
    	ByteBuffer headerBB;
    	int counter=0;
    	int ackno = 1;

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            while (true) {
                try {
                    DatagramPacket request = new DatagramPacket(new byte[packetSize+17], packetSize+17);
                    socket.receive(request);

                    //parse out out header
                    data = new byte[request.getLength()-17];
                    headerBB = ByteBuffer.wrap(request.getData());
                    sequence = headerBB.getInt();
                    offset = headerBB.getInt();
                    inLength = headerBB.getLong();


                    headerBB.get(data, 0, request.getLength()-17);
                    serverByte.put(sequence, data);

                    /*
                     *	if (ackno == sequence){
                    		System.out.println("Receive, time, sequence no, recv");
                    		Send window ack
                    		ackno++;
                    	}
                    	else{
                    		System.out.println("Duplicate, time, sequence no, out of order");
                    		Send dupe ack
                    		continue;
                    	}
                     */

                    System.out.println(sequence + "-"+offset + "-"+(offset+data.length-1));

                    //check if last packet has been received
                    if(inLength <= offset+data.length) {
                    	last = true;
                    }

                    //if last packet has been received, check that all packets have been received
                    if(last) {
                    	complete = true;
                    	for(Integer i: serverByte.keySet()) {

                    		//iterate through sequence numbers, compare to counter, make sure all number are present and in order
                    		if(i-1 == counter) {
                    			counter++;
                    			continue;
                    		}
                    		else {
                    			complete = false;
                    			break;
                    		}
                    	}
                    	counter = 0;
                    }

                    //if all packets in and in order, write to file
                	if(last && complete) {
                		FileOutputStream fos = new FileOutputStream(output);
                		try {
                    		for(byte[] b: serverByte.values()) {

                    			fos.write(b);
                    		}

                    		//close fileoutputstream, reset complete and last for server to continue receiving
                    		fos.close();
                    		complete = false;
                    		last = false;
                		} catch (IOException e) {
                		    e.printStackTrace();
                		}
            		}
                } catch (IOException | RuntimeException ex) {
                    errors.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        } catch (IOException ex) {
            errors.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}