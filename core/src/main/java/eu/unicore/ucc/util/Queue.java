package eu.unicore.ucc.util;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Simple queue with limited length and possible delay for entry retrieval
 *  
 * @author schuller
*/
public abstract class Queue {

	protected final DelayQueue<QueueEntry<String>> elements;

	protected int limit = -1;

	protected int queued = 0;

	protected long delay;

	/**
	 * create a new queue without any delay
	 */
	public Queue(){
		this(0);
	}

	public Queue(long delay){
		elements = new DelayQueue<>();
		this.delay = delay; //ms.
	}

	public void setDelay(long delay){
		this.delay=delay;
	}

	/**
	 * limit the size of this queue (-1 means no limit)
	 * 
	 * @param lim
	 */
	public void setLimit(int lim){
		limit = lim;
	}

	protected void checkLimit()throws Exception{
		if(limit!=-1 && getSize()>=limit) throw new Exception("Queue limit reached");
	}

	/**
	 * return the current number of things scheduled in this queue
	 */
	protected int getSize(){
		return elements.size(); 
	}

	/**
	 * update the number of queued things
	 */
	protected abstract void update();

	/**
	 * add an element
	 */ 
	public void add(String o) throws Exception{
		checkLimit();
		elements.add(new QueueEntry<String>(o,System.currentTimeMillis(),delay));
	}

	public void add(String o, long lastAccessed) throws Exception{
		checkLimit();
		elements.add(new QueueEntry<String>(o,lastAccessed,delay));
	}

	/**
	 * get the next in line, which is removed from the queue
	 * 
	 * @return null if there's nothing left 
	 */
	public String next(){
		update();
		QueueEntry<String> e = elements.poll();
		return e!=null? e.entry : null;
	}

	/**
	 * get the next element, waiting if necessary for an element
	 * 
	 * @param timeout - the time to wait
	 * @param unit - time units
	 * @throws InterruptedException
	 */
	public String next(long timeout, TimeUnit unit)throws InterruptedException{
		update();
		QueueEntry<String> e = elements.poll(timeout, unit);
		return e!=null? e.entry : null;
	}

	public int length(){
		return elements.size();
	}

	public static class QueueEntry<T>implements Delayed{

		final long lastAccessed;
		final long queueDelay;
		final T entry;

		public QueueEntry(T entry, long lastAccessed, long queueDelay){
			this.entry = entry;
			this.lastAccessed = lastAccessed;
			this.queueDelay = queueDelay;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(queueDelay-System.currentTimeMillis()+lastAccessed, TimeUnit.MILLISECONDS);
		}

		@Override
		public int compareTo(Delayed o) {
			return (int)(lastAccessed-((QueueEntry<?>)o).lastAccessed);
		}
	}

}
