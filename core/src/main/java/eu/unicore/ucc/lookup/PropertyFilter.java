package eu.unicore.ucc.lookup;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.lookup.Filter;
import eu.unicore.ucc.UCC;

/**
 * filter according to property values<br/>
 * @author schuller
 */
public class PropertyFilter implements Filter {

	public static enum MOD {
		EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, GREATER, LESS
	}

	private final MOD modifier;

	private final String property;

	private final String value;

	private PropertyFilter(String property, MOD modifier, String value){
		this.modifier = modifier;
		this.property = property;
		this.value = value;
		if(modifier==null || property==null || value==null) {
			throw new IllegalArgumentException("Cannot create filter");
		}
	}

	@Override
	public boolean accept(BaseServiceClient resource) {
		try{
			return accept(resource.getProperties());
		}catch(Exception e){
			UCC.console.error(e, "Can't find property <{}>", property);
		}
		return false;
	}

	public boolean accept(JSONObject properties) throws Exception {
		try{
			return compare(value, properties.get(property).toString());
		}catch(JSONException e) {
			return false;
		}
	}

	private boolean compare(String expected, String actual){
		switch(modifier) {
		case EQUALS:
			return expected.equalsIgnoreCase(actual);
		case NOT_EQUALS:
			return !expected.equalsIgnoreCase(actual);
		case CONTAINS:
			return actual.contains(expected);
		case NOT_CONTAINS:
			return !actual.contains(expected);
		case LESS:
			return actual.compareTo(expected)<0;
		case GREATER:
			return actual.compareTo(expected)>0;
		default: return false;
		}
	}

	/**
	 * accepts arguments: "Name [OPERAND] Value"
	 * or the short version "NameOPValue", where OP is "=", "!=" etc
	 */
	public static PropertyFilter create(String... args) {
		String propertyName;
		MOD mod;
		String val;
		if(args.length==3){
			propertyName = args[0];
			mod = createModifier(args[1]);
			val = args[2];
		}else if(args.length==1) {
			Matcher m = filterPattern.matcher(args[0]);
			m.matches();
			propertyName = m.group(1);
			mod = createModifier(m.group(2));
			val = m.group(3);
		}
		else throw new IllegalArgumentException();
		return new PropertyFilter(propertyName, mod, val);
	}

	static final Pattern filterPattern = Pattern.compile("(\\w+)(\\W+)(\\w+)");

	private static final Map<MOD, String[]> modifiers = Map.of(
		MOD.EQUALS , new String[]{ "=", "==", "equals", "eq" },
		MOD.NOT_EQUALS , new String[]{ "!=", "notequals", "neq" },
		MOD.CONTAINS, new String[]{ "~=", "contains", "c"},
		MOD.NOT_CONTAINS, new String[]{ "!~", "!~=", "notcontains", "nc"},
		MOD.LESS, new String[]{ "<", "lessthan", "lt"},
		MOD.GREATER, new String[]{ ">", "greaterthan", "gt"}
	);

	static MOD createModifier(String modSpec) {
		for(var e: modifiers.entrySet()) {
			String[] vs = e.getValue();
			for(String v: vs) {
				if(modSpec.equals(v)) {
					return e.getKey();
				}
			}
		}
		return null;
	}

}
