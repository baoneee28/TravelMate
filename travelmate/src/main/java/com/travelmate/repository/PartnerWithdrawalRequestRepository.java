package com.travelmate.repository;

import com.travelmate.entity.PartnerWithdrawalRequest;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.PartnerWithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerWithdrawalRequestRepository extends JpaRepository<PartnerWithdrawalRequest, Long> {

    List<PartnerWithdrawalRequest> findAllByOrderByRequestedAtDesc();

    List<PartnerWithdrawalRequest> findByPartnerOrderByRequestedAtDesc(User partner);

    long countByWithdrawalStatus(PartnerWithdrawalStatus status);

    boolean existsByRequestCode(String requestCode);
}
