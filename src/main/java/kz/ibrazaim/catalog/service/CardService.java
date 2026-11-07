package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.config.CardEncryption;
import kz.ibrazaim.catalog.model.Card;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final CardEncryption cardEncryption;

    @Transactional
    public void saveCard(User user, String cardNumber, String cardHolderName, String expiryMonth, String expiryYear) {
        if (user == null) {
            throw new IllegalStateException("User is not authenticated.");
        }
        if (cardNumber == null || cardNumber.isBlank()
                || cardHolderName == null || cardHolderName.isBlank()
                || expiryMonth == null || expiryMonth.isBlank()
                || expiryYear == null || expiryYear.isBlank()) {
            return;
        }

        String digits = cardNumber.replaceAll("\\D", "");
        String last4 = digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;
        String maskedCard = "**** **** **** " + last4;

        // Обновляем существующую карту или создаём новую (храним только маску последних 4 цифр)
        List<Card> existingCards = cardRepository.findAllByUser(user);
        Card card;
        if (!existingCards.isEmpty()) {
            card = existingCards.get(0);
            if (existingCards.size() > 1) {
                cardRepository.deleteAll(existingCards.subList(1, existingCards.size()));
            }
        } else {
            card = new Card();
            card.setUser(user);
        }
        card.setCardNumber(cardEncryption.encrypt(maskedCard));
        card.setCardHolder(cardEncryption.encrypt(cardHolderName.trim()));
        card.setMonth(cardEncryption.encrypt(expiryMonth.trim()));
        card.setYear(cardEncryption.encrypt(expiryYear.trim()));

        cardRepository.save(card);
    }

    public Card findByUser(User user) {
        if (user == null) {
            return null;
        }

        List<Card> cards = cardRepository.findAllByUser(user);
        if (cards.isEmpty()) {
            return null;
        }

        Card card = cards.get(0);
        Card decrypted = new Card();
        decrypted.setId(card.getId());
        decrypted.setUser(card.getUser());
        decrypted.setCardNumber(cardEncryption.decrypt(card.getCardNumber()));
        decrypted.setCardHolder(cardEncryption.decrypt(card.getCardHolder()));
        decrypted.setMonth(cardEncryption.decrypt(card.getMonth()));
        decrypted.setYear(cardEncryption.decrypt(card.getYear()));
        return decrypted;
    }
}
