package de.fzj.unicore.ucc.admin;

import java.util.Map;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.admin.AdminAction;
import de.fzj.unicore.wsrflite.admin.AdminActionResult;

public class MockAdminAction implements AdminAction {

	@Override
	public String getName() {
		return "mock";
	}
	
	@Override
	public String getDescription() {
		return "echoes back parameters";
	}


	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		AdminActionResult aar=new AdminActionResult(true, "ok");
		for(Map.Entry<String, String>e: params.entrySet()){
			aar.addResult(e.getKey(), "echo-"+e.getValue());
		}
		return aar;
	}

}
