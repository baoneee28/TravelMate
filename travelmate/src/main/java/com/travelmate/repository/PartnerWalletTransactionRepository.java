package com.travelmate.repository;

import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.PartnerWalletTransaction;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.PartnerWalletTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerWalletTransactionRepository extends JpaRepository<PartnerWalletTransaction, Long> {

    List<PartnerWalletTransaction> findByPartnerOrderByCreatedAtDesc(User partner);

    boolean existsBySettlementAndTransactionType(
            PartnerSettlement settlement,
            PartnerWalletTransactionType transactionType
    );

    boolean existsByTransactionCode(String transactionCode);
}
