package eu.unicore.ucc.runner;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import de.fzj.unicore.uas.util.MessageWriter;
import eu.unicore.client.core.SiteClient;
import eu.unicore.ucc.helpers.DefaultMessageWriter;

/**
 * this strategy tries to balance job submissions over the available sites,
 * according to weights that are assigned to each site. 
 * 
 * @author schuller
 */
public class WeightedSelection implements SiteSelectionStrategy {

	private MessageWriter msg=new DefaultMessageWriter();
	
	private final File weightsFile;
	
	private long lastAccess=0;
	
	private final Map<String, Integer>weights=new HashMap<>();
	
	private final Map<String, AtomicInteger>selected=new HashMap<>();

	public static final String DEFAULT_WEIGHT="UCC_DEFAULT_SITE_WEIGHT";
	
	public WeightedSelection(File weightsFile, MessageWriter msg){
		this.weightsFile=weightsFile;
		if(msg!=null)this.msg=msg;
	}

	public SiteClient select(List<SiteClient> available) {
		List<String>names=new ArrayList<>();
		Map<String, SiteClient>map=new HashMap<>();
		for(SiteClient tss: available){
			try{
				String name=tss.getProperties().getString("siteName");
				names.add(name);
				map.put(name,tss);
			}catch(Exception e){
				msg.error("",e);
			}
		}
		return map.get(select(map.keySet()));
	}
	
	public String select(Set<String>available){
		checkUpdate();
		String selectedTSS=null;
		float worstRatio=-1;
		//for each tss available, check its current ration of submitted jobs to weight.
		//select the one with the worst ratio
		for(String name: available){
			try{
				if(selectedTSS==null)selectedTSS=name;
				float ratio=getRatio(name);
				if(ratio<0){
					selected.put(name, new AtomicInteger());
					continue;
				}
				if(ratio<worstRatio || worstRatio<0){
					worstRatio=ratio;
					selectedTSS=name;
				}
			}catch(Exception e){
				msg.error("",e);
			}
		}
		storeSelection(selectedTSS);
		return selectedTSS;
	}
	
	/**
	 * returns the current ratio of submitted jobs to site weight
	 * @param name - the site name
	 * @return the ratio or <code>-1</code> if no jobs should be submitted to this site
	 */
	protected float getRatio(String name){
		Integer w=weights.get(name);
		if(w==null){
			w=weights.get(DEFAULT_WEIGHT);
		}
		if(w==0){
			return -1;
		}
		return ((float)get(name).get())/(float)w;
	}
	
	private synchronized AtomicInteger get(String key) {
		AtomicInteger s = selected.get(key);
		if(s==null) {
			s=new AtomicInteger();
			selected.put(key,s);
		}
		return s;
	}
	
	protected synchronized void storeSelection(String name){
		AtomicInteger val = selected.get(name);
		val.incrementAndGet();
	}
	
	//check if file was modified, and read the weights if it was
	protected void checkUpdate(){
		if(lastAccess<weightsFile.lastModified()){
			lastAccess=weightsFile.lastModified();
			Properties p=new Properties();
			try{
				FileInputStream fis=new FileInputStream(weightsFile);
				p.load(fis);
				fis.close();
			}catch(Exception e){
				msg.error("Problem reading from site weights file", e);
				return;
			}
			for(Object oKey: p.keySet()){
				Integer i=1;
				String key=String.valueOf(oKey);
				try{
					i=Integer.parseInt(p.getProperty(key));
				}catch(Exception e){
					msg.error("Syntax error in weights file (Format: Site name = <integer weight>", e);
				}
				weights.put(key,i);
				if(DEFAULT_WEIGHT.equalsIgnoreCase(key)){
					msg.verbose("Default site weight <"+i+">");
				}
				else{
					msg.verbose("Site <"+key+"> weight <"+i+">");
				}
				if(weights.get(DEFAULT_WEIGHT)==null){
					weights.put(DEFAULT_WEIGHT, Integer.valueOf(1));
					msg.verbose("Default site weight is <1> (can be changed using "+DEFAULT_WEIGHT+")");
				}
			}
		}
	}
	
	public Map<String,AtomicInteger>getSelectionStatistics(){
		return selected;
	}

	public File getWeightsFile() {
		return weightsFile;
	}
	
}
