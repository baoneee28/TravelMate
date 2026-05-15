package com.travelmate.service;

import com.travelmate.entity.TravelPost;
import com.travelmate.repository.TravelPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class TravelPostService {

    private final TravelPostRepository travelPostRepository;

    public List<TravelPost> getVisibleByCategory(TravelPost.Category category) {
        return travelPostRepository.findByCategoryAndStatusOrderByCreatedAtDesc(
                category, TravelPost.Status.VISIBLE);
    }

    public List<TravelPost> getAllForAdmin() {
        return travelPostRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<TravelPost> findById(Long id) {
        return travelPostRepository.findById(id);
    }

    @Transactional
    public TravelPost create(String title, String summary, String content,
                             String thumbnailUrl, String sourceUrl,
                             TravelPost.Category category, String createdBy) {
        TravelPost post = new TravelPost();
        post.setTitle(title);
        post.setSummary(summary);
        post.setContent(content != null && !content.isBlank() ? content : null);
        post.setThumbnailUrl(thumbnailUrl != null && !thumbnailUrl.isBlank() ? thumbnailUrl : null);
        post.setSourceUrl(sourceUrl);
        post.setCategory(category);
        post.setStatus(TravelPost.Status.VISIBLE);
        post.setCreatedBy(createdBy);
        return travelPostRepository.save(post);
    }

    @Transactional
    public TravelPost update(Long id, String title, String summary, String content,
                             String thumbnailUrl, String sourceUrl,
                             TravelPost.Category category) {
        TravelPost post = travelPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết id=" + id));
        post.setTitle(title);
        post.setSummary(summary);
        post.setContent(content != null && !content.isBlank() ? content : null);
        post.setThumbnailUrl(thumbnailUrl != null && !thumbnailUrl.isBlank() ? thumbnailUrl : null);
        post.setSourceUrl(sourceUrl);
        post.setCategory(category);
        return travelPostRepository.save(post);
    }

    @Transactional
    public TravelPost toggleStatus(Long id) {
        TravelPost post = travelPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết id=" + id));
        post.setStatus(post.getStatus() == TravelPost.Status.VISIBLE
                ? TravelPost.Status.HIDDEN
                : TravelPost.Status.VISIBLE);
        return travelPostRepository.save(post);
    }

    @Transactional
    public void delete(Long id) {
        travelPostRepository.deleteById(id);
    }
}
