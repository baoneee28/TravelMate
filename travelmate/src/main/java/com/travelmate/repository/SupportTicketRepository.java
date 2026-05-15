package com.travelmate.repository;

import com.travelmate.entity.SupportTicket;
import com.travelmate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByPartnerOrderByCreatedAtDesc(User partner);

    List<SupportTicket> findAllByOrderByCreatedAtDesc();

    List<SupportTicket> findByStatusOrderByCreatedAtDesc(String status);

    List<SupportTicket> findByRequesterRoleOrderByCreatedAtDesc(String requesterRole);

    long countByStatus(String status);

    long countByRequesterRole(String requesterRole);
}
