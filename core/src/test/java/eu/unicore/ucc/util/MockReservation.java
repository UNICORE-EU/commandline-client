package eu.unicore.ucc.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.idb.Incarnation;
import eu.unicore.xnjs.resources.ResourceRequest;
import eu.unicore.xnjs.tsi.IReservation;
import eu.unicore.xnjs.tsi.ReservationStatus;
import eu.unicore.xnjs.tsi.ReservationStatus.Status;
import eu.unicore.xnjs.tsi.remote.TSIMessages;
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

	public String makeReservation(Map<String,String> resources, Calendar startTime, Client client)
	throws ExecutionException {
		try{
			//merge resource request with IDB defaults...
			Incarnation gr=configuration.get(Incarnation.class);
			List<ResourceRequest>resourceRequest = new ArrayList<>();
			for(String name: resources.keySet()) {
				resourceRequest.add(new ResourceRequest(name, resources.get(name)));
			}
			List<ResourceRequest>incarnated=gr.incarnateResources(resourceRequest, client);
			String tsiCmd = new TSIMessages(configuration).makeMakeReservationCommand(incarnated,startTime,client);
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
