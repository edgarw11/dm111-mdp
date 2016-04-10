package com.souza.mdp.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.souza.mdp.models.User;

@Path("users")
public class UserManager {
	private static final Logger log = Logger.getLogger("UserManager");
	public static final String USER_KIND = "Users";
	public static final String PROP_EMAIL = "email";
	public static final String PROP_PASSWORD = "password";
	public static final String PROP_GCM_REG_ID = "gcmRegId";
	public static final String PROP_LAST_LOGIN = "lastLogin";
	public static final String PROP_LAST_GCM_REGISTER = "lastGCMRegister";
	public static final String PROP_ROLE = "role";
	public static final String PROP_CPF = "cpf";
	public static final String PROP_SALES_ID = "salesId";
	public static final String PROP_CRM_ID = "crmId";

	@Context
	SecurityContext securityContext;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN", "USER" })
	@Path("/byemail/{email}")
	public User getUserByEmail(@PathParam(PROP_EMAIL) String email) {

		if (securityContext.getUserPrincipal().getName().equals(email)
				|| securityContext.isUserInRole("ADMIN")) {

			DatastoreService datastore = DatastoreServiceFactory
					.getDatastoreService();
			Filter emailFilter = new FilterPredicate(PROP_EMAIL,
					FilterOperator.EQUAL, email);
			Query query = new Query(USER_KIND).setFilter(emailFilter);
			Entity userEntity = datastore.prepare(query).asSingleEntity();
			if (userEntity != null) {
				User user = entityToUser(userEntity);
				return user;
			} else {
				throw new WebApplicationException(Status.NOT_FOUND);
			}

		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN", "USER" })
	@Path("/bycpf/{cpf}")
	public User getUserByCpf(@PathParam(PROP_CPF) String cpf) {
		
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		Entity userEntity = getUserEntityByCpf(cpf, datastore);

		if (userEntity != null) {
			if (securityContext.getUserPrincipal().getName()
					.equals(userEntity.getProperty(PROP_EMAIL))
					|| securityContext.isUserInRole("ADMIN")) {

				User user = entityToUser(userEntity);
				return user;

			} else {
				throw new WebApplicationException(Status.FORBIDDEN);
			}
		} else {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
	}

	public static String getUserMailByCpf(String cpf, DatastoreService datastore) {

		Entity userEntity = getUserEntityByCpf(cpf, datastore);

		if (userEntity != null) {

			String email = (String)userEntity.getProperty(PROP_EMAIL);
			return email;

		} else {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
	}

	private static Entity getUserEntityByCpf(String cpf, DatastoreService datastore) {
		Filter cpfFilter = new FilterPredicate(PROP_CPF, FilterOperator.EQUAL,
				cpf);
		Query query = new Query(USER_KIND).setFilter(cpfFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();
		return userEntity;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN" })
	public List<User> getUsers() {
		List<User> users = new ArrayList<>();
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		
		if (securityContext == null){
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		if (securityContext.getUserPrincipal().getName().equals("admin@souza.com")
				|| securityContext.isUserInRole("ADMIN")) {
			
			Query query = new Query(USER_KIND).addSort(PROP_EMAIL,
					SortDirection.ASCENDING);
			List<Entity> userEntities = datastore.prepare(query).asList(
					FetchOptions.Builder.withDefaults());
			for (Entity userEntity : userEntities) {
				User user = entityToUser(userEntity);

				users.add(user);
			}
			return users;
		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN", "USER" })
	public User saveOrUpdateUser(@Valid User user) {

		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		datastore = DatastoreServiceFactory.getDatastoreService();
		Filter emailFilter = new FilterPredicate(PROP_EMAIL,
				FilterOperator.EQUAL, user.getEmail());
		Query query = new Query(USER_KIND).setFilter(emailFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();

		if (!checkIfEmailExist(user)) {

			if (!checkIfCpfExist(user)) {

				if (userEntity != null) { // EDIT USER
					// ONLY OWNER OR ADMIN CAN EDIT THE USER
					if (securityContext.getUserPrincipal().getName()
							.equals(user.getEmail())
							|| securityContext.isUserInRole("ADMIN")) {

						if (user.getId() != 0) {

							userToEntity(user, userEntity);
							if (!securityContext.isUserInRole("ADMIN")) {
								user.setRole("USER");
							}
							datastore.put(userEntity);
							return user;

						} else {
							throw new WebApplicationException(
									"O	ID	do	usuário	deve	ser	informado	para	ser	alterado",
									Status.BAD_REQUEST);
						}

					} else {
						throw new WebApplicationException(Status.FORBIDDEN);
					}

				} else { // CREATE NEW USER
					// ONLY ADMIN CAN CREATE NEW USERS
					if (securityContext.isUserInRole("ADMIN")) {

						Key userKey = KeyFactory
								.createKey(USER_KIND, "userKey");
						userEntity = new Entity(USER_KIND, userKey);
						user.setGcmRegId("");
						user.setLastGCMRegister(null);
						user.setLastLogin(null);
						userToEntity(user, userEntity);
						datastore.put(userEntity);
						user.setId(userEntity.getKey().getId());

					} else {
						throw new WebApplicationException(Status.FORBIDDEN);
					}
				}

			} else {
				throw new WebApplicationException(
						"Já	existe	um	usuário	cadastrado	com	o	mesmo	CPF",
						Status.BAD_REQUEST);
			}

		} else {
			throw new WebApplicationException(
					"Já	existe	um	usuário	cadastrado	com	o	mesmo	e-mail",
					Status.BAD_REQUEST);
		}

		return user;
	}

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{email}")
	@RolesAllowed({ "ADMIN", "USER" })
	@Deprecated
	public User alterUser(@PathParam("email") String email, @Valid User user) {

		if (user.getId() != 0) {
			if (securityContext.getUserPrincipal().getName().equals(email)
					|| securityContext.isUserInRole("ADMIN")) {
				if (!checkIfEmailExist(user)) {
					DatastoreService datastore = DatastoreServiceFactory
							.getDatastoreService();
					Filter emailFilter = new FilterPredicate(PROP_EMAIL,
							FilterOperator.EQUAL, email);
					Query query = new Query(USER_KIND).setFilter(emailFilter);
					Entity userEntity = datastore.prepare(query)
							.asSingleEntity();
					if (userEntity != null) {
						userToEntity(user, userEntity);
						if (!securityContext.isUserInRole("ADMIN")) {
							user.setRole("USER");
						}
						datastore.put(userEntity);
						return user;
					} else {
						throw new WebApplicationException(Status.NOT_FOUND);
					}
				} else {
					throw new WebApplicationException(
							"Já	existe	um	usuário	cadastrado	com	o	mesmo	e-mail",
							Status.BAD_REQUEST);
				}
			} else {
				throw new WebApplicationException(Status.FORBIDDEN);
			}
		} else {
			throw new WebApplicationException(
					"O	ID	do	usuário	deve	ser	informado	para	ser	alterado",
					Status.BAD_REQUEST);
		}
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{cpf}")
	@RolesAllowed({ "ADMIN", "USER" })
	public Status deleteUser(@PathParam(PROP_CPF) String cpf) {

		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		Entity userEntity = getUserEntityByCpf(cpf, datastore);

		if (userEntity != null) {
			if (securityContext.getUserPrincipal().getName()
					.equals(userEntity.getProperty(PROP_EMAIL))
					|| securityContext.isUserInRole("ADMIN")) {

				datastore.delete(userEntity.getKey());
				return Status.OK;

			} else {
				throw new WebApplicationException(Status.FORBIDDEN);
			}
		} else {
			throw new WebApplicationException(Status.NOT_FOUND);

		}
	}

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/update_gcm_reg_id/{gcmRegId}")
	@RolesAllowed({ "USER" })
	public User updateGCMRegId(@PathParam("gcmRegId") String gcmRegId) {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Filter emailFilter = new FilterPredicate(PROP_EMAIL,
				FilterOperator.EQUAL, securityContext.getUserPrincipal()
						.getName());
		Query query = new Query(USER_KIND).setFilter(emailFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();

		if (userEntity != null) {
			userEntity.setProperty(PROP_GCM_REG_ID, gcmRegId);
			userEntity.setProperty(PROP_LAST_GCM_REGISTER, Calendar
					.getInstance().getTime());
			User user = entityToUser(userEntity);
			datastore.put(userEntity);
			return user;
		} else {
			throw new WebApplicationException(Status.NOT_FOUND);

		}
	}

	private boolean checkIfEmailExist(User user) {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Filter emailFilter = new FilterPredicate(PROP_EMAIL,
				FilterOperator.EQUAL, user.getEmail());
		Query query = new Query(USER_KIND).setFilter(emailFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();
		if (userEntity == null) {
			return false;
		} else {
			if (userEntity.getKey().getId() == user.getId()) {
				// está alterando o mesmo user
				return false;
			} else {
				return true;
			}
		}
	}

	private boolean checkIfCpfExist(User user) {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Filter cpfFilter = new FilterPredicate(PROP_EMAIL,
				FilterOperator.EQUAL, user.getCpf());
		Query query = new Query(USER_KIND).setFilter(cpfFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();
		if (userEntity == null) {
			return false;
		} else {
			if (userEntity.getKey().getId() == user.getId()) {
				// está alterando o mesmo user
				return false;
			} else {
				return true;
			}
		}
	}

	private void userToEntity(User user, Entity userEntity) {
		userEntity.setProperty(PROP_EMAIL, user.getEmail());
		userEntity.setProperty(PROP_PASSWORD, user.getPassword());
		userEntity.setProperty(PROP_GCM_REG_ID, user.getGcmRegId());
		userEntity.setProperty(PROP_LAST_LOGIN, user.getLastLogin());
		userEntity.setProperty(PROP_LAST_GCM_REGISTER,
				user.getLastGCMRegister());
		userEntity.setProperty(PROP_ROLE, user.getRole());
		userEntity.setProperty(PROP_CPF, user.getCpf());
		userEntity.setProperty(PROP_SALES_ID, user.getSalesId());
		userEntity.setProperty(PROP_CRM_ID, user.getCrmId());
	}

	static User entityToUser(Entity userEntity) {
		User user = new User();
		user.setId(userEntity.getKey().getId());
		user.setEmail((String) userEntity.getProperty(PROP_EMAIL));
		user.setPassword((String) userEntity.getProperty(PROP_PASSWORD));
		user.setGcmRegId((String) userEntity.getProperty(PROP_GCM_REG_ID));
		user.setId(userEntity.getKey().getId());
		user.setLastLogin((Date) userEntity.getProperty(PROP_LAST_LOGIN));
		user.setLastGCMRegister((Date) userEntity
				.getProperty(PROP_LAST_GCM_REGISTER));
		user.setRole((String) userEntity.getProperty(PROP_ROLE));
		user.setCpf((String) userEntity.getProperty(PROP_CPF));
		user.setSalesId((Long) userEntity.getProperty(PROP_SALES_ID));
		user.setCrmId((Long) userEntity.getProperty(PROP_CRM_ID));
		return user;
	}
}
