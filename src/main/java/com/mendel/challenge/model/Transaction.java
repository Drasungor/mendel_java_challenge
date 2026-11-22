package com.mendel.challenge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Data layer: JPA entity mapped to the "transactions" table.
 *
 * <pre>
 * transactions
 *   id         BIGINT  PRIMARY KEY        -- sequential, from the "transactions_seq" sequence
 *   amount     REAL    NOT NULL           -- Java float
 *   parent_id  BIGINT  NULL  FOREIGN KEY -> transactions(id)
 * </pre>
 */
@Entity
@Table(name = "transactions")
public class Transaction {

	/**
	 * Sequential id from a database sequence. Hibernate reserves ids in blocks, so it doesn't
	 * need a database round trip per insert, and can batch inserts.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id;

	@Column(nullable = false)
	private float amount;

	/**
	 * Self-reference: the parent transaction, or null for a root transaction.
	 * LAZY: the parent row is only loaded from the database if its fields are actually read
	 * (reading just its id does not need a query).
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Transaction parent;

	/** Required by JPA. */
	protected Transaction() {
	}

	public Transaction(float amount, Transaction parent) {
		this.amount = amount;
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

	public Transaction getParent() {
		return parent;
	}

	public void setParent(Transaction parent) {
		this.parent = parent;
	}
}
