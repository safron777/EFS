package ru.mtsbank.cardoffice.cardissuance.adapters.in.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.mtsbank.cardoffice.cardissuance.application.CreateApplicationUseCase;
import ru.mtsbank.cardoffice.cardissuance.application.GetIssuanceUseCase;
import ru.mtsbank.cardoffice.cardissuance.domain.IssuanceApplication;

/**
 * CAP-5 REST resource (AD-8): {@code GET
 * /clients/{clientId}/products/{productId}/issuance}, mirroring
 * {@code client.js}'s {@code getIssuance} exactly, and {@code POST
 * /clients/{clientId}/products/{productId}/issuance/applications}, a new
 * endpoint invented for this story with no existing call to mirror (story
 * Intent/Design Notes). Translates HTTP to the application layer only; no
 * business logic lives here (spine Design Paradigm).
 *
 * {@code clientId}/{@code productId} path variables are accepted at face
 * value and never cross-checked -- history is global in this demo (story
 * Boundaries & Constraints), same rationale as card-account-service's
 * {@code DocumentController}.
 */
@RestController
@RequestMapping("/clients/{clientId}/products/{productId}/issuance")
public class IssuanceController {

    private final GetIssuanceUseCase getIssuanceUseCase;
    private final CreateApplicationUseCase createApplicationUseCase;

    public IssuanceController(GetIssuanceUseCase getIssuanceUseCase, CreateApplicationUseCase createApplicationUseCase) {
        this.getIssuanceUseCase = getIssuanceUseCase;
        this.createApplicationUseCase = createApplicationUseCase;
    }

    @GetMapping
    public IssuanceResponse getIssuance(
            @PathVariable("clientId") String clientId, @PathVariable("productId") String productId) {
        return IssuanceResponse.from(getIssuanceUseCase.execute(clientId, productId));
    }

    @PostMapping("/applications")
    public ResponseEntity<ApplicationResponse> createApplication(
            // clientId/productId are bound only to match the URL shape -- intentionally
            // unused in the body, same rationale as getIssuance above.
            @PathVariable("clientId") String clientId,
            @PathVariable("productId") String productId,
            @Valid @RequestBody CreateApplicationRequest request) {
        IssuanceApplication created = createApplicationUseCase.execute(request.card(), request.method());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationResponse.from(created));
    }
}
