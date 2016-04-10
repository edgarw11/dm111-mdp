package com.souza.mdp.models;

import javax.validation.constraints.NotNull;

public class PriceUpdate {

	@NotNull
	private Long productId;

	@NotNull
	private Float newPrice;

	public PriceUpdate() {
		super();
	}

	public PriceUpdate(Long productId, Float newPrice) {
		super();
		this.productId = productId;
		this.newPrice = newPrice;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public Float getNewPrice() {
		return newPrice;
	}

	public void setNewPrice(Float newPrice) {
		this.newPrice = newPrice;
	}

}
