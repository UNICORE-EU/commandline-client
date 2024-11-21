package eu.unicore.ucc.runner;

public class RunnerException extends Exception {

	private static final long serialVersionUID=1l;

	private final String errorCode;

	private final String errorReason;

	public RunnerException() {
		this(Runner.ERR_UNKNOWN,null,null);
	}

	public RunnerException(String errorCode, String errorReason) {
		this(errorCode,errorReason,null);
	}

	public RunnerException(String errorCode, String errorReason, Throwable cause) {
		super(errorReason,cause);
		this.errorCode=errorCode;
		this.errorReason=errorReason;
	}

	public String getErrorReason() {
		return errorReason;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
