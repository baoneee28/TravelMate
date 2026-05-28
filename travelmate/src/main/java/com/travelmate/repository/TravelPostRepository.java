package com.travelmate.repository;

import com.travelmate.entity.TravelPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TravelPostRepository extends JpaRepository<TravelPost, Long> {

    List<TravelPost> findByCategoryAndStatusOrderByCreatedAtDesc(
            TravelPost.Category category, TravelPost.Status status);

    List<TravelPost> findByStatusOrderByCreatedAtDesc(TravelPost.Status status);

    List<TravelPost> findByDestinationSlugAndStatusOrderByCreatedAtDesc(
            String destinationSlug, TravelPost.Status status);

    List<TravelPost> findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
            Collection<String> destinationSlugs, TravelPost.Status status);

    List<TravelPost> findTop3ByDestinationSlugAndStatusOrderByCreatedAtDesc(
            String destinationSlug, TravelPost.Status status);

    List<TravelPost> findTop3ByDestinationSlugInAndStatusOrderByCreatedAtDesc(
            Collection<String> destinationSlugs, TravelPost.Status status);

    java.util.Optional<TravelPost> findByIdAndStatus(Long id, TravelPost.Status status);

    java.util.Optional<TravelPost> findFirstBySourceUrlOrderByIdAsc(String sourceUrl);

    java.util.Optional<TravelPost> findFirstByTitleOrderByIdAsc(String title);

    List<TravelPost> findAllByOrderByCreatedAtDesc();
}
