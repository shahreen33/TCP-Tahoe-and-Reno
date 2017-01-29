/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcp_tahoe_client;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Socket;
import java.net.UnknownHostException;

/*
 * Let's write the pseudo code at first
 * Open file and create packets reading from the file
 * Create connection with the server
 * Get the first packet from server to know about the server window size
 * Set thresh hold equal to server window size
 * Send the first packet to set EstimatedRTT = SampleRTT
 * CW = 1
 * SendBase = 1
 * Now start loop forever
 * loop (forever) { 
           	while(timer running)
           	{
           		if(timeout)
           		{
           			CW = 1;
           			thresh hold /= 2; 
           			break;
           		}
           	}
			
			start timer
           	for(i:1->CW)
           		send SendBase+i-1 th packet
         }   end of loop forever 
	In the ack part
	ACK received, with ACK field value of y 
                 if (y > SendBase) { 
                 	calculate timeout
                      SendBase = y
                      if (there are currently not-yet-acknowledged segments)
                               start timer and send the next packets so that cw number of packets are on the fly
                      else
                      		   Stop timer. ( The break statement in your waiting for timeout while loop ) .
                      } 
 */
public class Client {
	
	public static String packet[] = new String[1010];
	public static int total, thresh_hold = 16, sendBase, last_sent_packet = 0, receive_window;
	public static Socket clientSocket;
	public static DataOutputStream outToServer;
        public static BufferedReader inFromServer ;
	public static boolean timer_running;
	public static long last_time_sent = 0,sampleRTT, estimatedRTT, devRTT = 0, timeout = 0;;
	public static double alpha = 0.125, beta = 0.25, CW = 1;
	
	static void open_file_and_init_packet() throws IOException
	{
		File input = new File("in.txt");
        Reader reader = new InputStreamReader(new FileInputStream(input));
        
        int idx = 0, cur_seq_num = 0;
        while(true)
        {
            int tmp = -1;
            packet[idx] = Integer.toString(cur_seq_num) +"000~";
            for(int i = 0; i<1000; i++)
            {
                tmp = reader.read();
                if(tmp == -1)
                    break;
                packet[idx] += (char)tmp;
            }
            packet[idx] += '@';
            if(tmp == -1)
                break;
//            System.out.println("Sent " + cur_seq_num);
            cur_seq_num++;
            idx++;
        }
        idx++;
        cur_seq_num++;
        packet[idx] = Integer.toString(cur_seq_num) +"000~";
        packet[idx] += "`";
        total = idx;
	}
	
	static void connect_with_server() throws UnknownHostException, IOException
	{
		clientSocket = new Socket("127.0.0.1", 6789);     
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		inFromServer =new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	}
	
	static int get_bandwidth_of_server() throws IOException
	{
		String input = inFromServer.readLine();
                int ret = Integer.parseInt(input);
//                System.out.println("got bandwidth from server : " + ret);
                return ret/1000;
	}
	
	static void send_packet(int idx) throws IOException
	{
		if(idx > total)
			return;
//                System.out.println("Sending " + idx);
//                System.out.println("Sending this packet: " + packet[idx]);
        outToServer.writeBytes(packet[idx]);
        System.out.println("Sent Packet Sequence: " + idx);
        last_sent_packet = idx;
	}
	
	//It's assumed that the first packet doesn't get any error
	static long get_RTT() throws IOException
	{
		long current = System.currentTimeMillis();
		send_packet(0);
		String input = inFromServer.readLine();
//                System.out.println("Starting RTT : " + (System.currentTimeMillis() - current));
		return System.currentTimeMillis() - current;
	}
	static boolean timeout_occured(long last, long timeout)
	{
		return (System.currentTimeMillis() - last) > timeout;
	}
	
	public static void start_timer()
	{
		last_time_sent = System.currentTimeMillis();
	}
	
	public static void calculate_timeout()
	{
		sampleRTT = System.currentTimeMillis() - last_time_sent;
		estimatedRTT = (long)((1 - alpha) * estimatedRTT + beta * sampleRTT);
		devRTT = (long) ((1 - beta) * devRTT + beta * Math.abs(sampleRTT - estimatedRTT));
		timeout = estimatedRTT + devRTT*4;
                
                System.out.println("TimeOut Interval: " + timeout);
	}
	
        public static void updateCW(int cumulative_ack)
        {
            if(CW >= thresh_hold)
            {
//                System.out.println("I'm here " + CW + " " + thresh_hold);
                for(int i = 1; i<=cumulative_ack; i++)
                    CW += 1/CW;
            }
            else
                CW *= 2;
//            CW = Math.min(CW, receive_window);
            System.out.println("CW: " + CW);
        }
        
	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
		
		open_file_and_init_packet();
		connect_with_server();
		
                thresh_hold = 16;
		receive_window = get_bandwidth_of_server();
		
		sampleRTT = estimatedRTT = get_RTT();
		
		sendBase = 1;
		CW = 2;
		timeout = 700;
		timer_running = false;
		get_ack ACK = new get_ack();
		ACK.start();
		
		while(true)
		{
			if(sendBase > total)
				break;
			
			while(timer_running)
			{
                        if(timeout_occured(last_time_sent, timeout))
           		{
                                System.out.println("Timeout has been occurred");
           			thresh_hold = (int)(CW/2);
           			thresh_hold = Math.max(thresh_hold, 1);
           			CW = 1;
                                break;
           		}
			}
			timer_running = true;
			start_timer();
                        int current_sendBase = sendBase;
			for(int i = 0; i<Math.min(CW,receive_window); i++)
                        {
                            send_packet(sendBase+i);
                            Thread.sleep(50);
                            if(sendBase != current_sendBase)
                            {
                                i = 0;
                                current_sendBase = sendBase;
                            }
                        }
			
		}
                System.out.println("Closing Client");
                ACK.done = true;
                
		Thread.sleep(5000);
	}

}