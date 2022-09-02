package eu.unicore.ucc.helpers;

public class EndProcessingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private int exitcode;
	
	public EndProcessingException(int exitcode, String message){
		super(message);
		this.exitcode=exitcode;
	}
	
	public EndProcessingException(int exitcode){
		this.exitcode=exitcode;
	}
	
	public int getExitCode(){
		return exitcode;
	}
}
