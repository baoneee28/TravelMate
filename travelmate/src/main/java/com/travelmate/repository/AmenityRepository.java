package com.travelmate.repository;

import com.travelmate.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    List<Amenity> findAllByOrderByCategoryAscNameAsc();

    List<Amenity> findByCategory(String category);
}
