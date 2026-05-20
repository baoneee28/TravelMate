package com.travelmate.repository;

import com.travelmate.entity.TravelDestination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelDestinationRepository extends JpaRepository<TravelDestination, Long> {

    List<TravelDestination> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    List<TravelDestination> findByRegionAndActiveTrueOrderByDisplayOrderAscNameAsc(TravelDestination.Region region);

    Optional<TravelDestination> findBySlug(String slug);
}
