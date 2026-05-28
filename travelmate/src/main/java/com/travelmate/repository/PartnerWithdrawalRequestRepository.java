package com.travelmate.repository;

import com.travelmate.entity.PartnerWithdrawalRequest;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.PartnerWithdrawalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerWithdrawalRequestRepository extends JpaRepository<PartnerWithdrawalRequest, Long> {

    List<PartnerWithdrawalRequest> findAllByOrderByRequestedAtDesc();

    List<PartnerWithdrawalRequest> findByPartnerOrderByRequestedAtDesc(User partner);

    long countByWithdrawalStatus(PartnerWithdrawalStatus status);

    boolean existsByRequestCode(String requestCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PartnerWithdrawalRequest r where r.id = :id")
    Optional<PartnerWithdrawalRequest> findByIdForUpdate(@Param("id") Long id);
}
