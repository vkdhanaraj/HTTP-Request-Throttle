package RequestThrottle;

public interface HttpResponse{
	public void onSuccess(StringBuffer response);
	public void onFailure();
	public void onTimeout();
}