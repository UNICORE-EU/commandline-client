package eu.unicore.ucc;

public class UCCException extends Exception {

	private static final long serialVersionUID = 1L;

	public UCCException(String message) {
		super(message);
	}

	public UCCException(String message, Throwable cause) {
		super(message, cause);
	}

}
