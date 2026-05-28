package com.travelmate.repository;

import com.travelmate.entity.Room;
import com.travelmate.entity.RoomVoucherAssignment;
import com.travelmate.entity.User;
import com.travelmate.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomVoucherAssignmentRepository extends JpaRepository<RoomVoucherAssignment, Long> {

    Optional<RoomVoucherAssignment> findByVoucherAndRoom(Voucher voucher, Room room);

    Optional<RoomVoucherAssignment> findByIdAndAssignedByPartner(Long id, User assignedByPartner);

    List<RoomVoucherAssignment> findByAssignedByPartnerOrderByAssignedAtDesc(User assignedByPartner);

    List<RoomVoucherAssignment> findByRoomAndActiveTrue(Room room);

    boolean existsByVoucherAndRoomAndActiveTrue(Voucher voucher, Room room);
}
