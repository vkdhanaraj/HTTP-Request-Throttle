/*
Status
0 : not processed yet
-1 : being processed
1 : request completed
2 : Failed once
3 : Failed twice
4,5,6.... : Failed....
Integer.MAX_VALUE : Timeout
 */
package RequestThrottle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

import java.time.Duration;
import java.time.Instant;

import java.util.Timer; 
import java.util.TimerTask;

import java.io.FileWriter; 
import java.io.IOException;

class request{
	String url;
	long status;
	Instant time_in;
	Duration waiting_time;
	StringBuffer response;
	HttpResponse resp_obj;
	
	request(){
	}

	request(String url, HttpResponse resp_obj){
		this.url = url;
		this.status = 0;
		this.time_in = Instant.now();
		this.response = new StringBuffer("\0");
		this.resp_obj = resp_obj;
	}

	//getter and setter for status
    long getStatus(){
    	return this.status;
    }

    void setStatus(long status){
    	this.status = status;
    }
} 


class Helper extends TimerTask 
{ 
	request obj;

	Helper(request obj){
		this.obj = obj;
	}

    public void run() 
    { 
    	if(obj.status <=0){
    		obj.resp_obj.onTimeout();
    		obj.status = Integer.MAX_VALUE;
    		System.out.println("Request Timed Out" + obj.url);
    	}
    }
} 

public class HttpRequestCon {
	static Queue<request> pending_buffer = new LinkedBlockingDeque<request>();
	static ArrayList<request> active_list = new ArrayList<request>();
	static long LIMIT = 6;
	static long TIMEOUT =5000;
	static long RETRY_LIMIT = 2;
	static Scheduler s = new Scheduler();
	static long PENDING_LIMIT = 1000;
	
//This Method called by user.
	public boolean RequestHandler(String url,HttpResponse resp_obj){
			if(pending_buffer.size() >= PENDING_LIMIT)
			{
				StringBuffer resp = new StringBuffer("Server Busy");
			//	System.out.println("Server Busy");
				return false;
			}
			request obj = new request(url, resp_obj);
    		pending_buffer.add(obj);   

    		schedulerThread o = new schedulerThread(s);
			o.start(); 
			StringBuffer temp  = new StringBuffer("\0");
			return true;
  }

//Method to perform buffered request.
    void Executer(){
	 	int i;
	 	request obj;
		for(i=0;i<HttpRequestCon.active_list.size();i++){
			obj = HttpRequestCon.active_list.get(i);
			try{
				if(obj.status == 0 || obj.status >1 && obj.status <= HttpRequestCon.RETRY_LIMIT){
	    			if(obj.status == 0)
		    			obj.status = -1;
		    		else
		    			obj.status *= -1;
				 	HttpThread obj1 = new HttpThread(obj);
    				obj1.start(); 
    			}
    		}catch(Exception e){}
		}
		schedulerThread o = new schedulerThread(HttpRequestCon.s);
		o.start();    	
	}

//Getters and Setters
    public static void setThreshold(long lim){
    	LIMIT = lim;
    }

    public static long getThreshold(){
    	return LIMIT;
    }

    public static void setPendingLimit(long p_limit){
    	PENDING_LIMIT = p_limit;
    }

    public static long getPendingLimit(){
    	return PENDING_LIMIT;
    }

    public static void setRetryLimit(long r_limit){
    	RETRY_LIMIT = r_limit;
    }

    public static long getRetryLimit(){
    	return RETRY_LIMIT;
    }

    public static void setTimeout(long t_out){
    	TIMEOUT = t_out;
    }

    public static long getTimeout(){
    	return TIMEOUT;
    }
}
 
//Thread to perform http calls
class HttpThread extends Thread{
	request obj;
	Timer timer;
	TimerTask task;
	
	HttpThread(){}

	HttpThread(request obj)
	{
		this.obj = obj;
	}

	public void run(){
		try{
            timer = new Timer();
            task = new Helper(obj);
            timer.schedule(task, HttpRequestCon.TIMEOUT);
            obj.waiting_time = Duration.between(obj.time_in,Instant.now());  
			
			try{
				FileWriter fw = new FileWriter("Log", true); 
				fw.write(obj.url +"          "+ obj.time_in +"    "+obj.waiting_time+"\n");
				fw.close();
			}
			catch(IOException e){
				System.out.println("IO Exception");
			}

			executeGet(obj);	
		}
		catch(Exception e){
			if(obj.status != Integer.MAX_VALUE){
				System.out.println("Network error!!!!  URL:" + obj.url);
				if((obj.status* -1) +1 == HttpRequestCon.RETRY_LIMIT+1){
					System.out.println("Failed to fetch response...   URL: " + obj.url);
				}
				obj.resp_obj.onFailure();
				obj.status = obj.status*-1 + 1;
			}
	
		}
		timer.cancel();
	}

//This function makes the call
	private void executeGet(request obj_req) throws Exception {
		URL obj = new URL(obj_req.url);
		System.out.println("\nSending 'GET' request to URL : " + obj_req.url + 
			"        Active Requests :"+HttpRequestCon.active_list.size());
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream(	)));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
		    response.append(inputLine);
		    if(obj_req.status == Integer.MAX_VALUE)
		    	break;
		}

		in.close();
		if(obj_req.status != Integer.MAX_VALUE){
			obj_req.resp_obj.onSuccess(response);
			System.out.println("Got respose for url :" + obj_req.url + "         Active Requests:"
					+ HttpRequestCon.active_list.size());
			obj_req.status = 1;
		}
    }

}

class schedulerThread extends Thread{    
	Scheduler s ;
	schedulerThread(){}

	schedulerThread(Scheduler s){
		this.s = s;
	}

    public void run(){
    	synchronized(s){
    		s.schedule();
    	}
    }

}

class Scheduler{
	void schedule(){
  		for(int i=0;i<HttpRequestCon.active_list.size();i++){		
			request obj = HttpRequestCon.active_list.get(i);
			try{
				if(obj.status == 1 ||
							obj.status == HttpRequestCon.RETRY_LIMIT+1 || obj.status == Integer.MAX_VALUE){
					HttpRequestCon.active_list.remove(i);
			}
			}catch(Exception e){					
					System.out.println("Exception while removing request");
			}
	
		}

    	int temp = 0;
    	while(HttpRequestCon.pending_buffer.size() > 0 && 
    				HttpRequestCon.active_list.size() < HttpRequestCon.LIMIT){ // Add a pending request into Active List
			try{
				HttpRequestCon.active_list.add(HttpRequestCon.pending_buffer.remove());
	   		}catch(Exception e){
	   			System.out.println("Exception while transfering request");
			}
			if(temp++ == HttpRequestCon.LIMIT)
				break;
		}
		if(HttpRequestCon.active_list.size()>0){
			HttpRequestCon obj_req = new HttpRequestCon();
			obj_req.Executer();
		}
		else if(HttpRequestCon.pending_buffer.size() > 0)
			schedule();
//  	System.out.println("End  A  "+ HttpRequestCon.active_list.size()+"  P    "+HttpRequestCon.pending_buffer.size());
    }
}
