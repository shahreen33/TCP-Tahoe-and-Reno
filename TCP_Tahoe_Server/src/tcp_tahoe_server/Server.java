/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcp_tahoe_server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

//http://networkengineering.stackexchange.com/questions/23317/how-to-calculate-sample-rtt
/*
 * Let's write the pseudo code at first
 * Host server
 * wait for client to connect
 * send first packet to let him know about your bandwidth
 * loop (forever) { 
           get data
           if(last_packet())
           		break;
           if(data not present)
           		save at appropriate place
           set y appropriately
         }   end of loop forever
         
        write to file.
        
        Ack part 
		send acknowledgments after every 500 ms for y
		if(last_packet_found)
			end
 */
public class Server {

	public static BufferedReader inFromClient;
        public static DataOutputStream  outToClient;
	static send_ACK ACK = new send_ACK();
	static int expected_seq = 0;
	static boolean acked[] = new boolean[1000000];
	static String packet[] = new String[100010];
	public static int last_packet = 819248913;
	static FileWriter writer;
	
	static void send_buffer_size() throws IOException
	{
		String BufferSize = "20000";
        BufferSize = BufferSize +'\n';
        outToClient.writeBytes(BufferSize);
        
	}
	
	static void open_file() throws IOException
	{
		File out= new File("Out.txt");   
        writer = new FileWriter(out);
	}
	
	static int next_expected_seq()
	{
		for(int i = expected_seq; ;i+=1000)
		{
			if(!acked[i])
				return i;
		}
	}
	
        static boolean error()
        {
//            return false;
            int bound = 5;
            Random rand = new Random();
            int nw = rand.nextInt(100);
            if(nw <= bound - 1)
                return true;
            return false;
        }
        
	static void get_and_store_data() throws IOException
	{
		int temp;
        String tmp = "", ret = "";
        while(true)
        {
            temp = inFromClient.read();
            if(temp == (int)'~' || temp == (int)'`')
                break;
            char ch = (char)temp;
            tmp+=ch;
            
        }
//        System.out.println("the number is " + tmp);
        int received_seq = Integer.parseInt(tmp);
        
        while(true)
        {
            temp = inFromClient.read();
            if(temp == (int)'@' || temp == (int)'`')
                break;
            char ch = (char)temp;
            ret += ch;  
        }
//        System.out.println("Received " + received_seq);
//        System.out.println("Received packet is " + ret);
        if(received_seq > 0 && error())
        {
            System.out.println("Data packet dropped: " + received_seq);
            return;
        }
        System.out.println("Received Packet Sequence: " + received_seq);
        if(received_seq == expected_seq)
        {
        	acked[expected_seq] = true;
        	expected_seq = next_expected_seq();
        	ACK.L.lock();
                ACK.y = received_seq+1000;
                ACK.start_work = true;
//                System.out.println("ACK.y = " + ACK.y);
                ACK.L.unlock();
        }
        else if(received_seq > expected_seq)
        {
        	acked[received_seq] = true;
        }
        else
        {
            ACK.L.lock();
            ACK.y = received_seq+1000;
            ACK.start_work = true;
//            System.out.println("ACK.y = " + ACK.y);
            ACK.L.unlock();
        }
        
        	//received_seq /= 1000;
        if(temp == (int)'`')
        {
        	last_packet = received_seq;
//                System.out.println("adskl;jdsagl;kj " + last_packet);
        	return;
        }
        packet[received_seq/1000] = ret;
	}
	
	public static void write_to_file() throws IOException
	{
            last_packet/=1000;
		for(int i =  0; i<last_packet; i++)
		{
//                        System.out.println("Writing " + i);
                        if(packet[i] == null)
                            continue;
			writer.write(packet[i]);
		}
//                System.out.println("packet writing finished");
		writer.close();
	}
	public static boolean finished()
	{
//            System.out.println("last packet is " + last_packet + " and expected_seq is " + expected_seq);
		return expected_seq >= last_packet;
	}
	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
        ServerSocket welcomeSocket = new ServerSocket(6789);
        Socket connectionSocket = welcomeSocket.accept();

        inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        outToClient = new DataOutputStream(connectionSocket.getOutputStream());
        
        send_buffer_size();
        open_file();
        
        ACK.start();
        while(true)
        {
        	get_and_store_data();
            if(finished())
            		break;
        }
        write_to_file();
        System.out.println("Closing Server");
        Thread.sleep(5000);
	}

}