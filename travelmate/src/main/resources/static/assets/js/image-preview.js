/**
 * Shared image preview for user-facing TravelMate pages.
 */
(function () {
  function initImagePreview() {
    if (window.__tmImagePreviewReady) return;

    const modal = document.getElementById('tmImagePreview');
    const image = document.getElementById('tmImagePreviewImg');
    const caption = document.getElementById('tmImagePreviewCaption');
    const closeButton = document.getElementById('tmImagePreviewClose');
    if (!modal || !image || !caption || !closeButton) return;

    window.__tmImagePreviewReady = true;

    const previewSelectors = [
      '.tm-previewable-image',
      '.booking-preview-main',
      '.booking-preview-thumb',
      '.room-type .rt-img-col > img',
      '.room-modal-main-img',
      '.am-gallery img',
      '.place-gallery__item img',
      '.destination-card img',
      '.promo-img',
      'main img',
      '[data-tm-preview-src]'
    ];

    function isUiImage(img) {
      return img.closest('#tmImagePreview, .logo, .sidebar-logo, .header, .user-menu, .avatar, .profile-avatar, .social-links, .floating-actions, .tm-floating-actions, .ai-chat, .chatbot, .msg-bot-av');
    }

    function isPreviewable(img) {
      if (!img || img.tagName !== 'IMG') return false;
      if (img.matches('[data-tm-no-preview], [data-no-preview]')) return false;
      if (isUiImage(img)) return false;
      if (img.closest('a, button') && !img.classList.contains('tm-previewable-image') && !img.dataset.tmPreviewSrc) return false;
      return previewSelectors.some(selector => img.matches(selector));
    }

    function markPreviewableImages(root) {
      const images = [];
      if (root.matches && root.matches('img')) images.push(root);
      if (root.querySelectorAll) {
        root.querySelectorAll('img').forEach(img => images.push(img));
      }
      images.forEach(img => {
        if (isPreviewable(img)) img.classList.add('tm-previewable-image');
      });
    }

    function openPreview(img) {
      const src = img.dataset.tmPreviewSrc || img.currentSrc || img.src;
      if (!src) return;

      const captionText = img.dataset.tmPreviewCaption || img.alt || '';
      image.src = src;
      image.alt = captionText || 'Ảnh phóng to';
      caption.textContent = captionText;
      caption.hidden = !captionText;
      modal.classList.add('open');
      modal.setAttribute('aria-hidden', 'false');
      document.body.classList.add('tm-image-preview-lock');
      try {
        closeButton.focus({ preventScroll: true });
      } catch (error) {
        closeButton.focus();
      }
    }

    function closePreview() {
      modal.classList.remove('open');
      modal.setAttribute('aria-hidden', 'true');
      document.body.classList.remove('tm-image-preview-lock');
      window.setTimeout(() => {
        if (!modal.classList.contains('open')) {
          image.removeAttribute('src');
          image.alt = '';
          caption.textContent = '';
        }
      }, 160);
    }

    document.addEventListener('click', event => {
      const target = event.target;
      const targetImage = target && target.closest ? target.closest('img') : null;
      if (!isPreviewable(targetImage)) return;

      event.preventDefault();
      event.stopPropagation();
      openPreview(targetImage);
    });

    modal.addEventListener('click', event => {
      if (event.target === modal) closePreview();
    });
    closeButton.addEventListener('click', closePreview);
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape' && modal.classList.contains('open')) closePreview();
    });

    markPreviewableImages(document);
    if (window.MutationObserver) {
      new MutationObserver(mutations => {
        mutations.forEach(mutation => {
          mutation.addedNodes.forEach(node => {
            if (node.nodeType === 1) markPreviewableImages(node);
          });
        });
      }).observe(document.body, { childList: true, subtree: true });
    }
  }

  if (document.getElementById('tmImagePreview')) {
    initImagePreview();
  } else if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initImagePreview);
  } else {
    initImagePreview();
  }
})();
