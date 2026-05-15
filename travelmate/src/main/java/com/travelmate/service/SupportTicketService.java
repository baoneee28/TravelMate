package com.travelmate.service;

import com.travelmate.entity.SupportTicket;
import com.travelmate.entity.User;
import com.travelmate.repository.SupportTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings("null")
@Service
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final NotificationService notificationService;

    public SupportTicketService(SupportTicketRepository ticketRepository,
                                NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.notificationService = notificationService;
    }

    // ─── Queries ───────────────────────────────────────────────────────────────

    public List<SupportTicket> getTicketsForPartner(User partner) {
        return ticketRepository.findByPartnerOrderByCreatedAtDesc(partner);
    }

    public List<SupportTicket> getAllTicketsForAdmin() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<SupportTicket> getTicketsByRole(String requesterRole) {
        return ticketRepository.findByRequesterRoleOrderByCreatedAtDesc(requesterRole);
    }

    // ─── Create ticket từ Partner ──────────────────────────────────────────────

    @Transactional
    public SupportTicket createTicket(User partner, String category, String subject,
                                      String priority, String description) {
        SupportTicket ticket = new SupportTicket();
        ticket.setRequesterRole("PARTNER");
        ticket.setPartner(partner);
        ticket.setRequesterName(partner.getName());
        ticket.setRequesterEmail(partner.getEmail());
        ticket.setCategory(category);
        ticket.setSubject(subject);
        ticket.setPriority(priority != null && !priority.isBlank() ? priority : "Trung bình");
        ticket.setDescription(description);
        ticket.setStatus("OPEN");
        return ticketRepository.save(ticket);
    }

    // ─── Create contact từ User đã đăng nhập ──────────────────────────────────

    @Transactional
    public SupportTicket createUserContact(User user, String phone, String category,
                                           String subject, String priority, String description) {
        SupportTicket ticket = new SupportTicket();
        ticket.setRequesterRole("USER");
        ticket.setUser(user);
        ticket.setRequesterName(user.getName());
        ticket.setRequesterEmail(user.getEmail());
        ticket.setRequesterPhone(phone);
        ticket.setCategory(category);
        ticket.setSubject(subject);
        ticket.setPriority(priority != null && !priority.isBlank() ? priority : "Trung bình");
        ticket.setDescription(description);
        ticket.setStatus("OPEN");
        return ticketRepository.save(ticket);
    }

    // ─── Create contact từ Guest (chưa đăng nhập) ─────────────────────────────

    @Transactional
    public SupportTicket createGuestContact(String name, String email, String phone,
                                            String category, String subject,
                                            String priority, String description) {
        SupportTicket ticket = new SupportTicket();
        ticket.setRequesterRole("GUEST");
        ticket.setRequesterName(name != null ? name.trim() : "Khách");
        ticket.setRequesterEmail(email != null ? email.trim() : "");
        ticket.setRequesterPhone(phone != null ? phone.trim() : "");
        ticket.setCategory(category);
        ticket.setSubject(subject);
        ticket.setPriority(priority != null && !priority.isBlank() ? priority : "Trung bình");
        ticket.setDescription(description);
        ticket.setStatus("OPEN");
        return ticketRepository.save(ticket);
    }

    // ─── Admin actions ─────────────────────────────────────────────────────────

    /**
     * Admin phản hồi ticket → RESPONDED.
     * Tự động gửi notification cho user hoặc partner nếu có account.
     */
    @Transactional
    public SupportTicket respondToTicket(Long ticketId, String adminResponse) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ticket #" + ticketId));
        ticket.setAdminResponse(adminResponse);
        ticket.setStatus("RESPONDED");
        ticketRepository.save(ticket);

        // Gửi notification cho người gửi ticket nếu có account
        User recipient = null;
        if ("PARTNER".equals(ticket.getRequesterRole()) && ticket.getPartner() != null) {
            recipient = ticket.getPartner();
        } else if ("USER".equals(ticket.getRequesterRole()) && ticket.getUser() != null) {
            recipient = ticket.getUser();
        }
        if (recipient != null) {
            try {
                notificationService.createTicketResponded(recipient, ticket.getId(), ticket.getSubject());
            } catch (Exception ignored) {
                // Notification không quan trọng hơn nghiệp vụ chính
            }
        }

        return ticket;
    }

    @Transactional
    public SupportTicket closeTicket(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ticket #" + ticketId));
        ticket.setStatus("CLOSED");
        return ticketRepository.save(ticket);
    }

    @Transactional
    public SupportTicket reopenTicket(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ticket #" + ticketId));
        ticket.setStatus("OPEN");
        return ticketRepository.save(ticket);
    }

    // ─── Statistics ────────────────────────────────────────────────────────────

    public long countOpenTickets() {
        return ticketRepository.countByStatus("OPEN");
    }

    public long countByRole(String role) {
        return ticketRepository.countByRequesterRole(role);
    }
}
