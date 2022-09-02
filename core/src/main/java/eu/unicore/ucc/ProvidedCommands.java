package eu.unicore.ucc;

/**
 * this interface is used to dynamically load the provided commands via
 * the ServiceLoader mechanism
 *
 * @since 1.4.0
 * @author schuller
 */
public interface ProvidedCommands {

	/**
	 * get the list of {@link Command} instances that are provided
	 */
	public Command[]getCommands();
	
}
