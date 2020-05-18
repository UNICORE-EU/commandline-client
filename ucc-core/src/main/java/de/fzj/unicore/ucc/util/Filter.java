package de.fzj.unicore.ucc.util;

import de.fzj.unicore.wsrflite.xmlbeans.client.BaseWSRFClient;

/**
 * interface for filtering ws-resources according to some criterion
 *
 * Typical usage:s
 *   
 *   <code>
 *    Filter someFilterImpl=new SomeFilterImpl();
 *    Filter someFilter=someFilterImpl.create(new String[]{"SubmissionTime","before","2008-07-01"});
 *   </code>
 * @author schuller
 */
public interface Filter {

	/**
	 * test whether the given resource matches
	 * 
	 * @param resource - the resource to test
	 * @return true if the resource passes the filter, false otherwise
	 */
	public boolean filterMatches(BaseWSRFClient resource);

}

