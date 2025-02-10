package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Card;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;

    public void saveCard(User user, String cardNumber, String cardHolderName, String expiryMonth, String expiryYear, String cvv){
        try {
            Card card = new Card();
            card.setUser(user);
            card.setCardNumber(cardNumber);
            card.setCardHolder(cardHolderName);
            card.setMonth(expiryMonth);
            card.setYear(expiryYear);
            card.setCvv(cvv);

            cardRepository.save(card);
        } catch (Exception e) {
            System.err.println("Error occurred while saving card details: " + e.getMessage());
            throw new RuntimeException("An error occurred while saving card details.", e);
        }
    }

    public Card findByUser(User user){
        return cardRepository.findByUser(user);
    }
}
