package com.souza.mdp.models;

import javax.validation.constraints.NotNull;

public class ProductOfInterest {

	private long id;
	@NotNull
	private String cpf;
	@NotNull
	private Long clientId;
	@NotNull
	private Long productId;
	@NotNull
	private Double targetPrice;

	public ProductOfInterest() {
		super();
	}

	public ProductOfInterest(String cpf, Long clientSalesId,
			Long productSalesId, Double targetPrice) {
		super();
		this.cpf = cpf;
		this.clientId = clientSalesId;
		this.productId = productSalesId;
		this.targetPrice = targetPrice;
	}

	public Long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public Long getClientId() {
		return clientId;
	}

	public void setClientId(long clientSalesId) {
		this.clientId = clientSalesId;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productSalesId) {
		this.productId = productSalesId;
	}

	public Double getTargetPrice() {
		return targetPrice;
	}

	public void setTargetPrice(Double targetPrice) {
		this.targetPrice = targetPrice;
	}

}
