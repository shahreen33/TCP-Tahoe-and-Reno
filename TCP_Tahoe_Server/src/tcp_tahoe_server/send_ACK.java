/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcp_tahoe_server;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */




import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author admin
 */
public class send_ACK implements Runnable{
    public int y = 0, last_acked = 0;
	private Thread t;
    boolean start_work = false;
    ReentrantLock L  = new ReentrantLock();
    @Override
    public void run() 
    {
    	while(true)
    	{
    		while(y <= last_acked && !start_work) ;
    		try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		try {
//                                System.out.println("Sending ACK for " + y);
                                System.out.println("ACK # " + y);
				Server.outToClient.writeBytes(Integer.toString(y)+ '\n');
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		L.lock();
    		last_acked = y;
                start_work = false;
    		L.unlock();
    		if(Server.finished())
    			break;
    	}
        
    }
    
    public void start()
    {
    	if(t == null)
    		t = new Thread(this, "whatever");
        t.start();
    }
    
}