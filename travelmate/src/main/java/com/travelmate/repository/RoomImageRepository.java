package com.travelmate.repository;

import com.travelmate.entity.Room;
import com.travelmate.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {

    List<RoomImage> findByRoomOrderByPrimaryImageDescSortOrderAscIdAsc(Room room);

    void deleteByRoom(Room room);
}
