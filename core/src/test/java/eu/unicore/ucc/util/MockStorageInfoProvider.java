package eu.unicore.ucc.util;

import java.util.HashMap;
import java.util.Map;

import eu.unicore.services.Kernel;
import eu.unicore.uas.impl.sms.DefaultStorageInfoProvider;
import eu.unicore.uas.impl.sms.StorageDescription;

public class MockStorageInfoProvider extends DefaultStorageInfoProvider {

	public MockStorageInfoProvider(Kernel kernel) {
		super(kernel);
	}

	@Override
	public Map<String, String> getUserParameterInfo(StorageDescription storageDescription) {
		Map<String,String>res = new HashMap<String, String>();
		res.put("foo","Some parameter settable by the user");
		res.put("bar","Some other parameter");
		return res;
	}

}
