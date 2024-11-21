package eu.unicore.ucc;

public class UCCException extends Exception {

	private static final long serialVersionUID = 1L;

	public UCCException(String message) {
		super(message);
	}

	public UCCException(Throwable cause) {
		super(cause);
	}

	public UCCException(String message, Throwable cause) {
		super(message, cause);
	}

	public static UCCException wrapped(Exception e) throws UCCException {
		if(e instanceof UCCException) {
			throw (UCCException)e;
		}
		throw new UCCException(e);
	}

}
