package de.fzj.unicore.ucc.helpers;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Simple cache support. Multiple caches identified by a cache ID can be used.
 * 
 * @author schuller
 */
public class ResourceCache {

	private static final ResourceCache cache = new ResourceCache();
	
	private final Map<String,Cache>caches;
	
	private final CacheManager cacheManager;
	
	private final AtomicInteger cacheHits=new AtomicInteger(0);
	
	private ResourceCache(){
		caches=new ConcurrentHashMap<String,Cache>();
		ByteArrayInputStream bis=new ByteArrayInputStream(ehCacheConfig.getBytes());
		cacheManager=CacheManager.create(bis);
	}
	
	/**
	 * config for ehcache</br>
	 * This is an in-memory cache only, 
	 * using the default LRU policy 
	 */
	private String ehCacheConfig="<ehcache name=\"__ucc_resource_cache__\">\n" +
			   "<defaultCache maxElementsInMemory=\"500\"\n"+
		        "eternal=\"false\"\n"+
		        "timeToIdleSeconds=\"3600\"\n"+
		        "timeToLiveSeconds=\"3600\"\n"+
		        "overflowToDisk=\"false\"\n"+
		        "diskPersistent=\"false\"\n"+
		        "/>\n"+
		        "</ehcache>";

	public static ResourceCache getInstance(){
		return cache;
	}
	
	public void put(String cacheID, Object key, Object value){
		Cache c=doGetCache(cacheID);
		Element el=new Element(key,value);
		c.put(el);
	}
	
	public Object get(String cacheID, Object key){
		Cache c=doGetCache(cacheID);
		Element el=c.get(key);
		if(el==null)return null;
		cacheHits.incrementAndGet();
		return el.getObjectValue();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getAs(String cacheID, Object key, Class<T>target){
		Object o=get(cacheID,key);
		if(o==null)return null;
		return (T)o;
	}

	public int getCacheHits(){
		return cacheHits.intValue();
	}
	
	private synchronized Cache doGetCache(String id){
		if(id==null)throw new NullPointerException("cache id can't be null");
		Cache c=caches.get(id);
		if(c==null){
			cacheManager.addCache(id);
			c=cacheManager.getCache(id);
			caches.put(id, c);
		}
		return c;
	}
	

	
	
}
