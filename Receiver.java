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

public class Receiver {

    private final static int PORT = 13;
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
    	int counter=0;
    	int packetSize = 1024;
    	long ackTime;
    	long reqTime;
    	double bad =0.40;
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
                    reqTime = System.nanoTime()-reqTime;
                    
                    //parse out out header
                    data = new byte[request.getLength()-20];
                    headerBB = ByteBuffer.wrap(request.getData());
                    sequence = headerBB.getInt();
                    offset = headerBB.getInt();
                    inLength = headerBB.getLong();
                    errStatus = headerBB.getInt();
                    
                    headerBB.get(data, 0, request.getLength()-20);                    
                    serverByte.put(sequence, data);
                    
                    if (ackno != sequence){
                    	System.out.println("DUPL "+ reqTime + " " + sequence +" !Seq");
                    	continue;
                    }
                    else if (errStatus == 1){
                		System.out.println("RECV "+ reqTime + " " + sequence +" CRPT");
                		continue;
                	}
                	else{
                		System.out.println("RECV "+ reqTime + " " + sequence +" RECV");
                		dos.writeInt(sequence);
                		ackStatus = dropCheck(bad);
                		
                		if(ackStatus.equals("DROP"))
                			continue;
                		else if (ackStatus.equals("SENT"))
                    		dos.writeInt(0);
                		else if(ackStatus.equals("ERR"))
                			dos.writeInt(1);
                		
                		
                		
                		ackData = baos.toByteArray();
                		ack = new DatagramPacket(ackData, 8, host, 14);
                		ackTime = System.nanoTime();
                		socket.send(ack);
                		ackTime = System.nanoTime()-ackTime;
                		System.out.println("SENDing ACK "+sequence+" "+ackTime+" "+dropCheck(bad));
                		ackno++;
                	}
                 
                    //logging for prog 1
                    //System.out.println(sequence + "-"+offset + "-"+(offset+data.length-1));
                    
                    //check if last packet has been received
                    if(inLength <= offset+data.length) {
                    	last = true;
                    }
                    
                    //if last packet has been received, check that all packets have been received
                    if(last) {
                    	System.out.println("yay???");
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
                		System.out.println("yay");
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