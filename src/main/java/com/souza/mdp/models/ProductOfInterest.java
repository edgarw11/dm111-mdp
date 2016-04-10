package com.souza.mdp.models;

import javax.validation.constraints.NotNull;

public class ProductOfInterest {

	@NotNull
	private String cpf;
	@NotNull
	private long clientSalesId;
	@NotNull
	private long productSalesId;
	@NotNull
	private float targetPrice;

	public ProductOfInterest() {
		super();
	}

	public ProductOfInterest(String cpf, long clientSalesId,
			long productSalesId, float targetPrice) {
		super();
		this.cpf = cpf;
		this.clientSalesId = clientSalesId;
		this.productSalesId = productSalesId;
		this.targetPrice = targetPrice;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public long getClientSalesId() {
		return clientSalesId;
	}

	public void setClientSalesId(long clientSalesId) {
		this.clientSalesId = clientSalesId;
	}

	public long getProductSalesId() {
		return productSalesId;
	}

	public void setProductSalesId(long productSalesId) {
		this.productSalesId = productSalesId;
	}

	public float getTargetPrice() {
		return targetPrice;
	}

	public void setTargetPrice(float targetPrice) {
		this.targetPrice = targetPrice;
	}

}
