package eu.unicore.ucc.helpers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Simple cache support. Multiple caches identified by a cache ID can be used.
 * 
 * @author schuller
 */
public class ResourceCache {

	private static final ResourceCache cache = new ResourceCache();
	
	private final Map<String,Cache<Object, Object>>caches;
	
	private ResourceCache(){
		caches = new ConcurrentHashMap<>();
	}

	public static ResourceCache getInstance(){
		return cache;
	}
	
	public void put(String cacheID, Object key, Object value){	
		doGetCache(cacheID).put(key,value);
	}
	
	public Object get(String cacheID, Object key){
		return doGetCache(cacheID).getIfPresent(key);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getAs(String cacheID, Object key, Class<T>target){
		Object o=get(cacheID,key);
		if(o==null)return null;
		return (T)o;
	}

	private synchronized Cache<Object,Object> doGetCache(String id){
		if(id==null)throw new NullPointerException("cache id can't be null");
		Cache<Object,Object> c=caches.get(id);
		if(c==null){
			c = CacheBuilder.newBuilder()
					.maximumSize(250)
					.expireAfterAccess(3600, TimeUnit.SECONDS)
					.expireAfterWrite(3600, TimeUnit.SECONDS)
					.build();
			caches.put(id, c);
		}
		return c;
	}
	

	
	
}
