package com.souza.mdp.authentication;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.DatatypeConverter;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.souza.mdp.models.User;
import com.souza.mdp.services.UserManager;

public class AuthFilter implements ContainerRequestFilter {

	@Context
	private ResourceInfo resourceInfo;

	private static final String ACCESS_UNAUTHORIZED = "Você não tem	permissão para acessar esse recurso";

	@Override
	public void filter(ContainerRequestContext requestContext)
			throws IOException {

		Method method = resourceInfo.getResourceMethod();

		if (method.isAnnotationPresent(PermitAll.class)) {
			return;
		}

		String auth = requestContext.getHeaderString("Authorization");
		if (auth == null) {
			requestContext.abortWith(Response
					.status(Response.Status.UNAUTHORIZED)
					.entity(ACCESS_UNAUTHORIZED).build());
			return;
		}
		String[] loginPassword = decode(auth);
		if (loginPassword == null || loginPassword.length != 2) {
			requestContext.abortWith(Response
					.status(Response.Status.UNAUTHORIZED)
					.entity(ACCESS_UNAUTHORIZED).build());
			return;
		}

		RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
		Set<String> rolesSet = new HashSet<String>(Arrays.asList(rolesAllowed
				.value()));
		if (checkCredentialsAndRoles(loginPassword[0], loginPassword[1],
				rolesSet, requestContext) == false) {
			requestContext.abortWith(Response
					.status(Response.Status.UNAUTHORIZED)
					.entity(ACCESS_UNAUTHORIZED).build());
			return;
		}

	}

	private boolean checkCredentialsAndRoles(String username, String password,
			Set<String> roles, ContainerRequestContext requestContext) {
		boolean isUserAllowed = false;
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Filter emailFilter = new FilterPredicate(UserManager.PROP_EMAIL,
				FilterOperator.EQUAL, username);
		Query query = new Query(UserManager.USER_KIND).setFilter(emailFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();
		if (userEntity != null) {
			if (password.equals(userEntity
					.getProperty(UserManager.PROP_PASSWORD))
					&& roles.contains(userEntity
							.getProperty(UserManager.PROP_ROLE))) {
				final User user = new User();
				user.setEmail((String) userEntity
						.getProperty(UserManager.PROP_EMAIL));
				user.setPassword((String) userEntity
						.getProperty(UserManager.PROP_PASSWORD));
				user.setId(userEntity.getKey().getId());
				user.setGcmRegId((String) userEntity
						.getProperty(UserManager.PROP_GCM_REG_ID));
				user.setLastLogin((Date) Calendar.getInstance().getTime());
				user.setLastGCMRegister((Date) userEntity
						.getProperty(UserManager.PROP_LAST_GCM_REGISTER));
				user.setRole((String) userEntity
						.getProperty(UserManager.PROP_ROLE));
				userEntity.setProperty(UserManager.PROP_LAST_LOGIN,
						user.getLastLogin());
				datastore.put(userEntity);
				requestContext.setSecurityContext(new SecurityContext() {
					@Override
					public boolean isUserInRole(String role) {
						return role.equals(user.getRole());
					}

					@Override
					public boolean isSecure() {
						return true;
					}

					@Override
					public Principal getUserPrincipal() {
						return user;
					}

					@Override
					public String getAuthenticationScheme() {
						return SecurityContext.BASIC_AUTH;
					}
				});
				isUserAllowed = true;
			}
		}
		return isUserAllowed;
	}

	// TODO THIS IS BASIC AUTH. WE NEED OAUTH BEARER TOKEN.
	private String[] decode(String auth) {
		auth = auth.replaceFirst("[B|b]asic ", "");
		byte[] decodedBytes = DatatypeConverter.parseBase64Binary(auth);
		if (decodedBytes == null || decodedBytes.length == 0) {
			return null;
		}
		return new String(decodedBytes).split(":", 2);
	}

}
