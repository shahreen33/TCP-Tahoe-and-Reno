/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcp_tahoe_client;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class get_ack implements Runnable {

    Thread t = null;
    public boolean done = false;
    
    static boolean error()
    {
//            return false;
            int bound = 2;
            Random rand = new Random();
            int nw = rand.nextInt(100);
            if(nw <= bound - 1)
                return true;
            return false;
    }
    
    boolean isThereNotAckedPacket(int ack_seq_num)
    {
    	return Client.last_sent_packet >= ack_seq_num;
    }
    @Override
    public void run() {
        String input = "";
        int ack_seq_num;
    	while(!done)
        {
            try {
            input = Client.inFromServer.readLine();
            } catch (IOException ex) {
            Logger.getLogger(get_ack.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if(error())
            {
                System.out.println("ACK Error");
                continue;
            }
            
            
            ack_seq_num =  Integer.parseInt(input);
            
            System.out.println("ACK: " + ack_seq_num);
            ack_seq_num /= 1000;
            
//            System.out.println("Ack received for " + ack_seq_num);
            if (ack_seq_num > Client.sendBase) 
            { 
            	Client.calculate_timeout();
//                int travelling_packet = (int)Client.CW - (ack_seq_num - Client.sendBase);
                Client.updateCW(ack_seq_num - Client.sendBase);
                if (isThereNotAckedPacket(ack_seq_num))
                {
                	Client.start_timer();
//                	for(int i = 1; i<=(Client.CW - travelling_packet); i++)
//					{
//                		try {
//							Client.send_packet(Client.last_sent_packet+1);
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
                }
                else
                	Client.timer_running = false;
            	Client.sendBase = ack_seq_num;  
                if(ack_seq_num >  Client.total)
                    break;
            }
        }
    }
    public void start()
    {
        if(t == null)
            t = new Thread(this, "whatever");
        t.start();
    }
}