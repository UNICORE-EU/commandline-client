package eu.unicore.ucc.authn;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import eu.unicore.services.rest.impl.ServicesBase;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * fake oidc server
 *
 * @author schuller
 */
@Path("/")
public class MockOIDCServer extends ServicesBase {

	public static final List<JSONObject> x = new ArrayList<>();
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response receive(@FormParam("username")String username,
			@FormParam("password")String password,
			@FormParam("grant_type") String grantType,
			@FormParam("otp") String otp) {
		JSONObject j = new JSONObject();
		if(username!=null)j.put("username", username);
		if(password!=null)j.put("password", password);
		if(grantType!=null)j.put("grant_type", grantType);
		if(otp!=null)j.put("otp", otp);
		x.add(j);
		JSONObject resp = new JSONObject();
		resp.put("access_token", "some_access_token");
		resp.put("refresh_token", "some_refresh_token");
		return Response.ok(resp.toString()).build();
	}
}
