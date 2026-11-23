package com.mendel.challenge.model;

/**
 * Domain model: a transaction, stored in memory by {@code TransactionRepository} keyed by id.
 *
 * <pre>
 *   id      Long    -- assigned by the client (PUT /api/transactions/{id})
 *   amount  float   -- required
 *   type    String  -- required, at most {@link #TYPE_MAX_LENGTH} characters
 *   parent  Transaction or null for a root transaction
 * </pre>
 */
public class Transaction {

	/** Max length of {@link #type}. Shared with the request validation in TransactionRequest. */
	public static final int TYPE_MAX_LENGTH = 100;

	/** Assigned, not generated: the client chooses the id in PUT /api/transactions/{id}. */
	private Long id;

	private float amount;

	private String type;

	/** Self-reference: the parent transaction, or null for a root transaction. */
	private Transaction parent;

	public Transaction(Long id, float amount, String type, Transaction parent) {
		this.id = id;
		this.amount = amount;
		this.type = type;
		this.parent = parent;
	}

	public Long getId() {
		return id;
	}

	public float getAmount() {
		return amount;
	}

	public void setAmount(float amount) {
		this.amount = amount;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Transaction getParent() {
		return parent;
	}

	public void setParent(Transaction parent) {
		this.parent = parent;
	}
}
