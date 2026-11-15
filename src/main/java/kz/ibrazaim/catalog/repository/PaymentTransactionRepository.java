package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Order;
import kz.ibrazaim.catalog.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByExternalTransactionId(String externalTransactionId);

    List<PaymentTransaction> findByOrderOrderByCreatedAtDesc(Order order);

    Optional<PaymentTransaction> findFirstByOrderOrderByCreatedAtDesc(Order order);
}

