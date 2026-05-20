package com.travelmate.service;

import com.travelmate.entity.TravelPost;
import com.travelmate.repository.TravelPostRepository;
import com.travelmate.util.DestinationAliasUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class TravelPostService {

    private final TravelPostRepository travelPostRepository;

    public List<TravelPost> getVisibleByCategory(TravelPost.Category category) {
        return travelPostRepository.findByCategoryAndStatusOrderByCreatedAtDesc(
                category, TravelPost.Status.VISIBLE);
    }

    public List<TravelPost> getVisiblePosts() {
        return travelPostRepository.findByStatusOrderByCreatedAtDesc(TravelPost.Status.VISIBLE);
    }

    public List<TravelPost> getVisibleByDestination(String destination) {
        Set<String> slugs = DestinationAliasUtil.searchSlugs(destination);
        if (slugs.isEmpty()) {
            return List.of();
        }
        return travelPostRepository.findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                slugs, TravelPost.Status.VISIBLE);
    }

    public List<TravelPost> getTopVisibleByDestination(String destination) {
        Set<String> slugs = DestinationAliasUtil.searchSlugs(destination);
        if (slugs.isEmpty()) {
            return List.of();
        }
        return travelPostRepository.findTop3ByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                slugs, TravelPost.Status.VISIBLE);
    }

    public List<TravelPost> getAllForAdmin() {
        return travelPostRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<TravelPost> findById(Long id) {
        return travelPostRepository.findById(id);
    }

    public Optional<TravelPost> findVisibleById(Long id) {
        return travelPostRepository.findByIdAndStatus(id, TravelPost.Status.VISIBLE);
    }

    @Transactional
    public TravelPost create(String title, String summary, String content,
                             String destination, String thumbnailUrl,
                             String sourceName, String sourceUrl,
                             TravelPost.Category category, String createdBy) {
        TravelPost post = new TravelPost();
        post.setTitle(title);
        applyDestination(post, destination);
        post.setSummary(summary);
        post.setContent(content != null && !content.isBlank() ? content : null);
        post.setThumbnailUrl(thumbnailUrl != null && !thumbnailUrl.isBlank() ? thumbnailUrl : null);
        post.setSourceName(sourceName != null && !sourceName.isBlank() ? sourceName.trim() : "Vietnam.travel");
        post.setSourceUrl(sourceUrl != null ? sourceUrl.trim() : null);
        post.setCategory(category);
        post.setStatus(TravelPost.Status.VISIBLE);
        post.setCreatedBy(createdBy);
        return travelPostRepository.save(post);
    }

    @Transactional
    public TravelPost update(Long id, String title, String summary, String content,
                             String destination, String thumbnailUrl,
                             String sourceName, String sourceUrl,
                             TravelPost.Category category) {
        TravelPost post = travelPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết id=" + id));
        post.setTitle(title);
        applyDestination(post, destination);
        post.setSummary(summary);
        post.setContent(content != null && !content.isBlank() ? content : null);
        post.setThumbnailUrl(thumbnailUrl != null && !thumbnailUrl.isBlank() ? thumbnailUrl : null);
        post.setSourceName(sourceName != null && !sourceName.isBlank() ? sourceName.trim() : "Vietnam.travel");
        post.setSourceUrl(sourceUrl != null ? sourceUrl.trim() : null);
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

    public String normalizeDestination(String input) {
        return DestinationAliasUtil.normalizeSlug(input);
    }

    public String getDestinationDisplayName(String input) {
        return DestinationAliasUtil.displayName(input);
    }

    private void applyDestination(TravelPost post, String destination) {
        String label = getDestinationDisplayName(destination);
        post.setDestination(label.isBlank() ? null : label);
        String slug = normalizeDestination(destination);
        post.setDestinationSlug(slug.isBlank() ? null : slug);
    }
}
