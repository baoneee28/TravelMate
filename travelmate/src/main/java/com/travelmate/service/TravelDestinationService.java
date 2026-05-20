package com.travelmate.service;

import com.travelmate.entity.TravelDestination;
import com.travelmate.repository.TravelDestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelDestinationService {

    private final TravelDestinationRepository travelDestinationRepository;

    public List<TravelDestination> getActiveDestinations() {
        return travelDestinationRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    public List<TravelDestination> getActiveByRegion(TravelDestination.Region region) {
        return travelDestinationRepository.findByRegionAndActiveTrueOrderByDisplayOrderAscNameAsc(region);
    }
}
