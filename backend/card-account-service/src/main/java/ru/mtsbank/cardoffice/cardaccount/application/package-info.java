/**
 * Use cases / application services orchestrating domain objects through ports.
 * The only layer allowed to coordinate more than one domain object at a time.
 * Populated by story 3-card-account-cards (CAP-3): {@code GetCardsUseCase}.
 * Story 4-card-account-documents (CAP-4) adds {@code GetDocumentsUseCase},
 * {@code SendDocumentUseCase} and {@code PrintDocumentUseCase}.
 */
package ru.mtsbank.cardoffice.cardaccount.application;
