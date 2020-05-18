package de.fzj.unicore.ucc.util;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.jsdl.JSDLParser;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.tsi.IReservation;
import de.fzj.unicore.xnjs.tsi.ReservationStatus;
import de.fzj.unicore.xnjs.tsi.ReservationStatus.Status;
import de.fzj.unicore.xnjs.tsi.remote.TSIUtils;
import eu.unicore.security.Client;

public class MockReservation implements IReservation {

	private final static Map<String, ReservationStatus> reservations = new HashMap<String, ReservationStatus>();

	public static String lastTSICommand;
	
	public static AtomicInteger queries=new AtomicInteger(0);
	
	private final XNJS configuration;
	
	@Inject
	public MockReservation(XNJS configuration){
		this.configuration=configuration;
	}
	
	public void cancelReservation(String resID, Client arg1)
	throws ExecutionException {
		reservations.remove(resID);
	}

	public String makeReservation(XmlObject resources, Calendar startTime, Client client)
	throws ExecutionException {
		try{
			ResourcesType rt=ResourcesType.Factory.parse(resources.newInputStream());
			//merge resource request with IDB defaults...
			Incarnation gr=configuration.get(Incarnation.class);
			List<ResourceRequest>resourceRequest = new JSDLParser().parseRequestedResources(rt);
			List<ResourceRequest>incarnated=gr.incarnateResources(resourceRequest, client);
			String tsiCmd=TSIUtils.makeMakeReservationCommand(incarnated,startTime,client);
			lastTSICommand=tsiCmd;
			String resID=UUID.randomUUID().toString();
			ReservationStatus rs=new ReservationStatus();
			rs.setStartTime(startTime);
			rs.setStatus(Status.WAITING);
			rs.setDescription("Testing reservation");
			reservations.put(resID, rs);
			return resID;
		}catch(Exception e){
			e.printStackTrace();
			throw new ExecutionException(e);
		}
	}

	public ReservationStatus queryReservation(String resID,Client arg2)
	throws ExecutionException {
		queries.incrementAndGet();
		return reservations.get(resID);
	}


}
