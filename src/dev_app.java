import RequestThrottle.HttpRequestCon;
import RequestThrottle.*;



class dev_app implements HttpResponse{
	public static void main(String[] args)
	{
		HttpRequestCon.setThreshold(5);
		for(int i=0;i<20;i++)
		{
			mythread t = new mythread();
			t.start();
		}

		try{
			Thread.sleep(5000);
		}
		catch(Exception e)
			{
			System.out.println("Sleep Exception");
		}


//		for(int i=0;i<20;i++)
//		{
//			mythread t = new mythread();
//			t.start();
//		}
	}

		public void onSuccess(StringBuffer response){
			System.out.println("Response recieved ");
		}

		public void onFailure(){
			System.out.println("Failed");
		}

		public void onTimeout(){
			System.out.println("Timed Out");
		}

}

class mythread extends Thread{
	static int rc=0;
	public void run(){
			boolean request_status;
			StringBuffer response = new StringBuffer();
			rc++;
			String url = "https://www.google.com       ";//+ rc;
			HttpRequestCon h = new HttpRequestCon();

			dev_app d = new dev_app();

 			request_status = h.RequestHandler(url,d);	
// 			System.out.println("Response = "+response);
	}
}