package com.souza.mdp.models;

import javax.validation.constraints.NotNull;

public class OrderUpdateMsg {

	private long orderId; // ID do pedido no provedor de vendas/logística
	private long userId; // ID do cliente cadastrada no provedor de vendas
	@NotNull
	private String cpf;

	private String reason;

	private String newStatus;

	public long getOrderId() {
		return orderId;
	}

	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getNewStatus() {
		return newStatus;
	}

	public void setNewStatus(String newStatus) {
		this.newStatus = newStatus;
	}

}
