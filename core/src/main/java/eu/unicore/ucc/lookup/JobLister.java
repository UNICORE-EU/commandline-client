package eu.unicore.ucc.lookup;

import java.util.Iterator;

import org.apache.logging.log4j.Logger;

import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.util.Log;

/**
 * Convenient access to the jobs on a given target system
 *
 * @author schuller
 */
public class JobLister implements Iterable<JobClient>{

	private static final Logger logger = Log.getLogger("UCC", JobLister.class);

	private final CoreClient site;

	private final String[] tags;

	public JobLister(CoreClient site, String[] tags){
		this.site = site;
		this.tags = tags;
	}

	/**
	 * returns an iterator over the available jobs, providing a
	 * pre-initialised JobClient per job
	 */
	public Iterator<JobClient> iterator(){
		try{
			final EnumerationClient ec = site.getJobsList();
			ec.setDefaultTags(tags);
			return new Iterator<JobClient>(){

				Iterator<String> iter = ec.iterator();

				public boolean hasNext() {
					return iter.hasNext();
				}

				public JobClient next() {
					String url = iter.next();
					try{
						return new JobClient(site.getEndpoint().cloneTo(url),
								site.getSecurityConfiguration(), 
								site.getAuth());
					}catch(Exception e){
						logger.error("Can't create Job Client.",e);
						return null;
					}
				}

				public void remove() {
					iter.remove();
				}
			};
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

}
