package eu.unicore.ucc.lookup;

import org.json.JSONObject;

import de.fzj.unicore.uas.util.MessageWriter;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.lookup.Filter;

/**
 * filter according to property values<br/>
 * @author schuller
 */
public class PropertyFilter implements Filter {
	
	private String modifier;
	private String value;
	private String property;
	private MessageWriter msg;
	
	private static final String MOD_EQUAL="equals";
	private static final String MOD_EQUAL_SHORT="eq";
	private static final String MOD_NOTEQUAL="notequals";
	private static final String MOD_NOTEQUAL_SHORT="neq";
	private static final String MOD_GT="greaterthan";
	private static final String MOD_GT_SHORT="gt";
	private static final String MOD_LT="lessthan";
	private static final String MOD_LT_SHORT="lt";
	private static final String MOD_CONTAINS="contains";
	private static final String MOD_CONTAINS_SHORT="c";
	private static final String MOD_NOTCONTAINS="notcontains";
	private static final String MOD_NOTCONTAINS_SHORT="nc";
	
	private PropertyFilter(String property, String modifier, String value,MessageWriter msg){
		this.modifier=modifier;
		this.property=property;
		this.value=value;
		this.msg=msg;
	}
	

	public boolean accept(BaseServiceClient resource) {
		try{
			JSONObject props = resource.getProperties();
			Object obj = props.get(property);
			String propValue=obj.toString();
			return compare(value,propValue);
		}catch(Exception e){
			msg.error("Can't find property <"+property+">", e);
		}
		return false;
	}

	protected boolean compare(String expected, String actual){
		if(modifier.equalsIgnoreCase(MOD_EQUAL)||modifier.equalsIgnoreCase(MOD_EQUAL_SHORT))return expected.equalsIgnoreCase(actual);
		if(modifier.equalsIgnoreCase(MOD_NOTEQUAL))return !expected.equalsIgnoreCase(actual);
		if(modifier.equalsIgnoreCase(MOD_CONTAINS)||modifier.equalsIgnoreCase(MOD_CONTAINS_SHORT))return actual.contains(expected);
		if(modifier.equalsIgnoreCase(MOD_NOTCONTAINS)||modifier.equalsIgnoreCase(MOD_NOTCONTAINS_SHORT))return !actual.contains(expected);
		if(modifier.equalsIgnoreCase(MOD_LT)||modifier.equalsIgnoreCase(MOD_LT_SHORT))return actual.compareTo(expected)<0;
		if(modifier.equalsIgnoreCase(MOD_GT)||modifier.equalsIgnoreCase(MOD_GT_SHORT))return actual.compareTo(expected)>0;
		msg.verbose("Can't compare!");
		return false;
	}
	
	protected boolean compareNumbers(Long expected, Long actual){
		if(modifier.equalsIgnoreCase("lessthan"))return actual<expected;
		if(modifier.equalsIgnoreCase("greaterthan"))return actual>expected;
		return false;
	}

	public static final class Factory {

		private static final String[] modifiers={
				MOD_EQUAL,MOD_EQUAL_SHORT,
				MOD_NOTEQUAL,MOD_NOTEQUAL_SHORT,
				MOD_CONTAINS,MOD_NOTCONTAINS,
				MOD_CONTAINS_SHORT,MOD_NOTCONTAINS_SHORT,
				MOD_GT,MOD_GT_SHORT,
				MOD_LT,MOD_LT_SHORT,
		};

		/**
		 * accepts arguments: PropertyName [modifier] [value]
		 * where [modifier] is one of: contains | lessthan | greaterthan | equals | not
		 */
		public static Filter create(MessageWriter msg, String... args) {
			if(args!=null && args.length==3){
				String propName=args[0];
				String mod=args[1];
				String val=args[2];
				msg.verbose("Filtering on property "+propName);
				if(acceptModifier(mod)){
					return new PropertyFilter(propName,mod,val,msg);
				}
			}
			msg.verbose("Could not create filter.");
			return null;
		}


		private static boolean acceptModifier(String modifier) {
			for(String s: modifiers){
				if(s.equalsIgnoreCase(modifier))return true;
			}
			return false;
		}
	}
	
}
