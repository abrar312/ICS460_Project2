
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

public class Receiver {

    private final static int PORT = 1300;
    private final static Logger audit = Logger.getLogger("requests");
    private final static Logger errors = Logger.getLogger("errors");

    public static void main(String[] args) throws UnknownHostException {
    	File output = new File("test.jpg");
    	SortedMap <Integer, byte[]> serverByte = new TreeMap <Integer, byte[]>();
    	byte[] data;
    	int sequence;
    	int offset;
    	long inLength;
        InetAddress host = InetAddress.getByName("localhost");
    	boolean last = false;
    	boolean complete = false;
    	ByteBuffer headerBB;
    	int counter= 0;
    	double bad = Double.parseDouble(JOptionPane.showInputDialog(null, "Enter Droprate: "));
    	int packetSize = Integer.parseInt(JOptionPane.showInputDialog(null, "Enter Packet Size: "));
    	long ackTime;
    	long reqTime;  
    	int errStatus = 0;
    	String ackStatus = "";
    	int ackno = 1;
    	DatagramPacket ack;
    	byte[] ackData;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	DataOutputStream dos = new DataOutputStream(baos);
    	
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            while (true) {
                try {
                    DatagramPacket request = new DatagramPacket(new byte[packetSize+20], packetSize+20);
                    reqTime = System.nanoTime();
                    socket.receive(request);
                    reqTime = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()-reqTime);
                    
                    //parse out out header
                    data = new byte[request.getLength()-20];
                    headerBB = ByteBuffer.wrap(request.getData());
                    sequence = headerBB.getInt();
                    offset = headerBB.getInt();
                    inLength = headerBB.getLong();
                    errStatus = headerBB.getInt();
                    
                    headerBB.get(data, 0, request.getLength()-20);                    
                    serverByte.put(sequence, data);
                    
                    if (errStatus == 1){
                		System.out.println("RECV "+ reqTime + " " + sequence +" CRPT "+errStatus);
                		baos.reset();
                		continue;
                	}
                    else if (errStatus == -1) {
                    	baos.reset();
						continue;
                    }
                	else{
                		if(ackno != sequence) {
                			System.out.println("DUPL "+ reqTime + " " + sequence +" !Seq");
                			ackno--;
                		}
                		else
                			System.out.println("RECV "+ reqTime + " " + sequence +" RECV");
                		dos.writeInt(sequence);
                		ackStatus = dropCheck(bad);
                		
                		if(ackStatus.equals("DROP"))
                			dos.writeInt(-1);
                			//continue;
                		else if (ackStatus.equals("SENT"))
                    		dos.writeInt(0);
                		else if(ackStatus.equals("ERR"))
                			dos.writeInt(1);
                		
                		ackData = baos.toByteArray();
                		ack = new DatagramPacket(ackData, 8, host, 14);
                		ackTime = System.nanoTime();
                		socket.send(ack);
                		ackTime = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()-ackTime);
                		System.out.println("SENDing ACK "+sequence+" "+ackTime+" "+ackStatus);
                		//if (ackStatus.equals("SENT"))
                		ackno++;
                		baos.reset();
                	}
                    
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
                    		ackno=1;
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
    private static String dropCheck (double bad) {
    	double badCheck = Math.random();
		if (badCheck < bad/2)
			return "DROP";
		else if (badCheck < bad)
			return "ERR";
		else
			return "SENT";
    }
}