package com.mendel.challenge.repository;

import com.mendel.challenge.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data layer: Spring Data generates the implementation at runtime
 * (save, findById, findAll, existsById, deleteById, ...).
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
