package com.souza.mdp.services;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.souza.mdp.models.PriceUpdate;
import com.souza.mdp.models.ProductOfInterest;

@Path("productsOfInterest")
public class ProductOfInterestManager {

	@Context
	SecurityContext securityContext;

	public static final String PRODUCTS_OF_INTEREST_KIND = "ProductsOfInterest";
	public static final String PROP_CPF = "cpf";
	public static final String PROP_CLIENT_ID = "clientId";
	public static final String PROP_PRODUCT_ID = "productId";
	public static final String PROP_TARGET_PRICE = "targetPrice";

	@POST
	@Path("priceAlert")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN" })
	public Status alertPriceUpdated(@Valid PriceUpdate priceUpdate) {

		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Filter prodIdFilter = new FilterPredicate(PROP_PRODUCT_ID,
				FilterOperator.EQUAL, priceUpdate.getProductId());
		Filter priceFilter = new FilterPredicate(PROP_TARGET_PRICE, FilterOperator.GREATER_THAN_OR_EQUAL,
				priceUpdate.getNewPrice());

		Filter prodIdAndPriceFilter = CompositeFilterOperator.and(prodIdFilter, priceFilter);

		Query query = new Query(PRODUCTS_OF_INTEREST_KIND)
				.setFilter(prodIdAndPriceFilter);
		
		List<Entity> productsOfInterestEntity = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
		
		MessageManager messageManager = new MessageManager();
		if (productsOfInterestEntity != null && productsOfInterestEntity.size() > 0){
			for (Entity entity : productsOfInterestEntity) {
				messageManager.sendPriceAlertMessage((String) entity.getProperty(PROP_CPF),
						priceUpdate);
			}
		}

		return Status.OK;
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN", "USER" })
	public Status saveOrUpdateProduct(@Valid ProductOfInterest prodOfInterest) {

		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		
		String userMail = UserManager.getUserMailByCpf(prodOfInterest.getCpf(), datastore);

		if (securityContext == null)
			System.out.println(" ######## security context ###################");
		
		if(securityContext.getUserPrincipal() == null)
			System.out.println(" ######## getUserPrincipal ###################");
		
		if (securityContext.getUserPrincipal().getName() == null)
			System.out.println(" ######## security context ###################");
		
		if (securityContext.isUserInRole("ADMIN"))
			System.out.println(" ######## ADMIN ###################");
		
		// ONLY OWNER OR ADMIN CAN SAVE/UPDATE THE PRODUCT OF INTEREST
		if (securityContext.getUserPrincipal().getName().equals(userMail)
				|| securityContext.isUserInRole("ADMIN")) {

			Entity prodOfInteretEntity = getProductOfInterest(
					prodOfInterest.getCpf(), prodOfInterest.getProductId(),
					datastore);

			if (prodOfInteretEntity != null) { // EDIT

				productOfInterestToEntity(prodOfInterest, prodOfInteretEntity);

				datastore.put(prodOfInteretEntity);
				return Status.OK;

			} else { // CREATE NEW USER

				Key prodOfInterestKey = KeyFactory.createKey(
						PRODUCTS_OF_INTEREST_KIND, "productOfInterestKey");
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

	private Entity getProductOfInterest(String cpf, long productId,
			DatastoreService datastore) {
		Filter cpfFilter = new FilterPredicate(PROP_CPF, FilterOperator.EQUAL,
				cpf);
		Filter prodIdFilter = new FilterPredicate(PROP_PRODUCT_ID,
				FilterOperator.EQUAL, productId);

		Filter cpfAndProdIdFilter = CompositeFilterOperator.and(cpfFilter,
				prodIdFilter);

		Query query = new Query(PRODUCTS_OF_INTEREST_KIND)
				.setFilter(cpfAndProdIdFilter);

		Entity prodOfInteretEntity = datastore.prepare(query).asSingleEntity();
		return prodOfInteretEntity;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN", "USER" })
	@Path("/bycpf/{cpf}")
	public List<ProductOfInterest> getProductsByCpf(
			@PathParam(PROP_CPF) String cpf) {

		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		
		String userMail = UserManager.getUserMailByCpf(cpf, datastore);

		if (securityContext.getUserPrincipal().getName().equals(userMail)
				|| securityContext.isUserInRole("ADMIN")) {

			List<ProductOfInterest> productsOfInterest = new ArrayList<>();
			List<Entity> productsOfInterestEntities = listProducts(cpf);

			if (productsOfInterestEntities != null
					&& productsOfInterestEntities.size() > 0) {

				for (Entity prodOfInterestEntity : productsOfInterestEntities) {
					ProductOfInterest prodOfInterest = entityToProductOfInterest(prodOfInterestEntity);

					productsOfInterest.add(prodOfInterest);
				}

				return productsOfInterest;

			} else {
				throw new WebApplicationException(Status.NOT_FOUND);
			}

		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}

	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{cpf}/{productId}")
	@RolesAllowed({ "ADMIN", "USER" })
	public Status deleteUser(@PathParam(PROP_CPF) String cpf,
			@PathParam(PROP_PRODUCT_ID) Long productId) {

		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		
		String userMail = UserManager.getUserMailByCpf(cpf, datastore);

		if (securityContext.getUserPrincipal().getName().equals(userMail)
				|| securityContext.isUserInRole("ADMIN")) {
			
			Entity prodOfInteretEntity = getProductOfInterest(cpf, productId,
					datastore);

			if (prodOfInteretEntity != null) {

				datastore.delete(prodOfInteretEntity.getKey());
				return Status.OK;

			} else {
				throw new WebApplicationException(Status.NOT_FOUND);

			}
		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
	}

	private List<Entity> listProducts(String cpf) {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Filter cpfFilter = new FilterPredicate(PROP_CPF, FilterOperator.EQUAL,
				cpf);
		Query query = new Query(PRODUCTS_OF_INTEREST_KIND).setFilter(cpfFilter)
				.addSort(PROP_PRODUCT_ID, SortDirection.ASCENDING);
		List<Entity> userEntity = datastore.prepare(query).asList(
				FetchOptions.Builder.withDefaults());

		return userEntity;
	}

	private void productOfInterestToEntity(ProductOfInterest prodOfInterest,
			Entity userEntity) {

		userEntity.setProperty(PROP_CPF, prodOfInterest.getCpf());
		userEntity.setProperty(PROP_PRODUCT_ID, prodOfInterest.getProductId());
		userEntity.setProperty(PROP_CLIENT_ID, prodOfInterest.getClientId());
		userEntity.setProperty(PROP_TARGET_PRICE,
				prodOfInterest.getTargetPrice());

	}

	static ProductOfInterest entityToProductOfInterest(Entity userEntity) {
		ProductOfInterest prodOfInterest = new ProductOfInterest();

		prodOfInterest.setCpf((String) userEntity.getProperty(PROP_CPF));
		prodOfInterest.setProductId((Long) userEntity
				.getProperty(PROP_PRODUCT_ID));
		prodOfInterest.setClientId((Long) userEntity
				.getProperty(PROP_CLIENT_ID));
		prodOfInterest.setTargetPrice((Double) userEntity
				.getProperty(PROP_TARGET_PRICE));

		return prodOfInterest;
	}

}
