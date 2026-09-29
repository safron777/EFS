/**
 * REST controllers implementing the resource shape mirrored from the frontend
 * contract (ARCHITECTURE-SPINE.md AD-8). Translates HTTP to application-layer
 * calls only -- no business logic. Populated by story 3-card-account-cards
 * (CAP-3): {@code CardController}, {@code CardResponse}, {@code ErrorResponse}
 * and {@code CardExceptionHandler}. Story 4-card-account-documents (CAP-4)
 * adds {@code DocumentController}, {@code DocumentResponse} and
 * {@code OkResponse}, and extends {@code CardExceptionHandler} with a
 * {@code DocumentNotFoundException} mapping.
 */
package ru.mtsbank.cardoffice.cardaccount.adapters.in.web;
