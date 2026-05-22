package com.travelmate.repository;

import com.travelmate.entity.PartnerWallet;
import com.travelmate.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerWalletRepository extends JpaRepository<PartnerWallet, Long> {
    Optional<PartnerWallet> findByPartner(User partner);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from PartnerWallet w where w.partner = :partner")
    Optional<PartnerWallet> findByPartnerForUpdate(@Param("partner") User partner);
}
