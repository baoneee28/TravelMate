package com.travelmate.repository;

import com.travelmate.entity.TravelPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelPostRepository extends JpaRepository<TravelPost, Long> {

    List<TravelPost> findByCategoryAndStatusOrderByCreatedAtDesc(
            TravelPost.Category category, TravelPost.Status status);

    List<TravelPost> findAllByOrderByCreatedAtDesc();
}
