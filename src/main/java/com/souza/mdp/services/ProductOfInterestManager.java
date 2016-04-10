package com.souza.mdp.services;

import java.util.Date;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.souza.mdp.models.ProductOfInterest;
import com.souza.mdp.models.User;

public class ProductOfInterestManager {

	@Context
	SecurityContext securityContext;

	public static final String PRODUCTS_OF_INTEREST_KIND = "ProductsOfInterest";
	public static final String PROP_CPF = "cpf";
	public static final String PROP_CLIENT_SALES_ID = "clientSalesId";
	public static final String PROP_PRODUCT_SALES_ID = "productSalesId";
	public static final String PROP_TARGET_PRICE = "targetPrice";

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN", "USER" })
	public Status saveOrUpdateProduct(@Valid ProductOfInterest prodOfInterest) {

		UserManager userManager = new UserManager();
		String userMail = userManager.getUserMailByCpf(prodOfInterest.getCpf());

		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		datastore = DatastoreServiceFactory.getDatastoreService();
		Filter cpfFilter = new FilterPredicate(PROP_CPF, FilterOperator.EQUAL,
				prodOfInterest.getCpf());
		Filter prodIdFilter = new FilterPredicate(PROP_PRODUCT_SALES_ID,
				FilterOperator.EQUAL, prodOfInterest.getProductSalesId());

		Filter cpfAndProdIdFilter = CompositeFilterOperator.and(cpfFilter,
				prodIdFilter);

		Query query = new Query(PRODUCTS_OF_INTEREST_KIND)
				.setFilter(cpfAndProdIdFilter);

		Entity prodOfInteretEntity = datastore.prepare(query).asSingleEntity();

		// ONLY OWNER OR ADMIN CAN SAVE/UPDATE THE PRODUCT OF INTEREST
		if (securityContext.getUserPrincipal().getName().equals(userMail)
				|| securityContext.isUserInRole("ADMIN")) {
			if (prodOfInteretEntity != null) { // EDIT

				productOfInterestToEntity(prodOfInterest, prodOfInteretEntity);

				datastore.put(prodOfInteretEntity);
				return Status.OK;

			} else { // CREATE NEW USER

				Key prodOfInterestKey = KeyFactory.createKey(PRODUCTS_OF_INTEREST_KIND,
						"productOfInterestKey");
				prodOfInteretEntity = new Entity(PRODUCTS_OF_INTEREST_KIND,
						prodOfInterestKey);
				
				productOfInterestToEntity(prodOfInterest, prodOfInteretEntity);
				datastore.put(prodOfInteretEntity);
			}
		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		return Status.OK;
	}

	private void productOfInterestToEntity(ProductOfInterest prodOfInterest,
			Entity userEntity) {

		userEntity.setProperty(PROP_CPF, prodOfInterest.getCpf());
		userEntity.setProperty(PROP_PRODUCT_SALES_ID,
				prodOfInterest.getProductSalesId());
		userEntity.setProperty(PROP_CLIENT_SALES_ID,
				prodOfInterest.getClientSalesId());
		userEntity.setProperty(PROP_TARGET_PRICE,
				prodOfInterest.getTargetPrice());

	}

	static ProductOfInterest entityToProductOfInterest(Entity userEntity) {
		ProductOfInterest prodOfInterest = new ProductOfInterest();

		prodOfInterest.setCpf((String) userEntity.getProperty(PROP_CPF));
		prodOfInterest.setProductSalesId((Long) userEntity
				.getProperty(PROP_PRODUCT_SALES_ID));
		prodOfInterest.setClientSalesId((Long) userEntity
				.getProperty(PROP_CLIENT_SALES_ID));
		prodOfInterest.setTargetPrice((Float) userEntity
				.getProperty(PROP_TARGET_PRICE));

		return prodOfInterest;
	}

}
