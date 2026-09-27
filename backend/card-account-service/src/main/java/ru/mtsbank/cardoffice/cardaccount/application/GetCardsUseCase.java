package ru.mtsbank.cardoffice.cardaccount.application;

import java.util.List;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.cardaccount.domain.Card;
import ru.mtsbank.cardoffice.cardaccount.domain.CardRepository;

/**
 * CAP-3: fetch a client's card list for a product. Pure pass-through over
 * the {@link CardRepository} port -- no not-found branching, since an
 * unmatched {@code clientId}/{@code productId} is an ordinary empty result
 * for this collection endpoint, not an error (story Boundaries &
 * Constraints).
 */
@Service
public class GetCardsUseCase {

    private final CardRepository cardRepository;

    public GetCardsUseCase(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public List<Card> execute(String clientId, String productId) {
        return cardRepository.findByClientIdAndProductId(clientId, productId);
    }
}
