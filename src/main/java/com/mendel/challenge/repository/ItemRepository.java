package com.mendel.challenge.repository;

import com.mendel.challenge.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data layer: Spring Data generates the implementation at runtime
 * (save, findById, findAll, deleteById, ...).
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
}
