package com.souza.mdp.services;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.gson.Gson;
import com.souza.mdp.models.OrderUpdateMsg;
import com.souza.mdp.models.User;
import com.souza.mdp.services.UserManager;

@Path("/message")
public class MessageManager {
	
	private static final Logger log = Logger.getLogger("MessageManager");
	private static final String API_KEY = "AIzaSyDLDmBjPteeAQlKvjkoalC9hsYu6-a-R9s";
	
	@Context
	SecurityContext securityContext;

	@POST
	@Path("/sendmessage/{cpf}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN" })
	public Status sendMessage(@PathParam("cpf") String cpf,
			@Valid OrderUpdateMsg msg) {

		User user;
		if ((user = findUserByCpf(cpf)) == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		// This is the API_KEY value generated on GAE application console
		Sender sender = new Sender(API_KEY);
		Gson gson = new Gson();
		Message message = new Message.Builder().addData("orderUpdateMsg",
				gson.toJson(msg)).build();
		Result result;
		
		try {
			result = sender.send(message, user.getGcmRegId(), 5);
			if (result.getMessageId() != null) {
				String canonicalRegId = result.getCanonicalRegistrationId();
				if (canonicalRegId != null) {
					log.severe("Usuário	[" + user.getEmail()
							+ "]	com	mais de	um	registro");
				}
			} else {
				String error = result.getErrorCodeName();
				log.severe("Usuário	[" + user.getEmail() + "]	não	registrado");
				log.severe(error);
				throw new WebApplicationException(Status.NOT_FOUND);
			}
		} catch (IOException e) {
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}
		
		return Status.OK;
	}

	private User findUserByCpf(String cpf) {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Filter cpfFilter = new FilterPredicate(UserManager.PROP_CPF,
				FilterOperator.EQUAL, cpf);
		Query query = new Query(UserManager.USER_KIND).setFilter(cpfFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();
		if (userEntity != null) {
			return UserManager.entityToUser(userEntity);
		} else {
			return null;
		}
	}

}
