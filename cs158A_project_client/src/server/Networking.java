package server;

public interface Networking {

	public void setOutputStream(int code, Object object);
    
    public void setOutputStream(Object object);
    
    public Object getInputStream();
    
    public int getInputStreamCode();
}
