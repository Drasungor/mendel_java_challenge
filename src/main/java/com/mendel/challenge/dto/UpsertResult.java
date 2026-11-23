package com.mendel.challenge.dto;

/**
 * Result of an upsert (create-or-replace) in the service layer: the stored transaction,
 * and whether it was newly created (true) or an existing one was replaced (false).
 */
public record UpsertResult(TransactionDto transaction, boolean created) {
}
