// /js/review-like.js
$(function () {
  const csrfToken  = $('meta[name="_csrf"]').attr('content');
  const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

  function applyButtonLabel(btn, liked) {
    const likeLabel = btn.data('labelLike') || '좋아요';
    const unlikeLabel = btn.data('labelUnlike') || '좋아요 취소';
    const label = liked ? unlikeLabel : likeLabel;

    btn.text(label);
    btn.attr({
      'aria-pressed': liked ? 'true' : 'false',
      'aria-label': label,
      title: label
    });
  }

  function setLikedUI(box, btn, liked) {
    const likedState = !!liked;
    box.attr('data-liked', likedState ? 'true' : 'false');
    box.data('liked', likedState);
    btn.toggleClass('on', likedState);
    applyButtonLabel(btn, likedState);
  }

  $('.review-like-box').each(function () {
    const box = $(this);
    const btn = box.find('.review-like-btn');

    if (!btn.length) {
      return;
    }

    const likedData = box.data('liked');
    const likedState = likedData === true || likedData === 'true';
    setLikedUI(box, btn, likedState);
  });

  $(document).on('click', '.review-like-btn', function () {
    const btn = $(this);
    const box = btn.closest('.review-like-box');
    const reviewId = box.data('review-id');
    const countEl = box.find('.review-like-count');

    btn.prop('disabled', true);

    $.ajax({
      url: '/reviews/' + reviewId + '/like',
      type: 'POST',
      dataType: 'json',
      headers: csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {},
      cache: false
    })
    .done(function (res) {
      countEl.text(String(res.count));
      setLikedUI(box, btn, !!res.liked);
    })
    .fail(function (xhr) {
      if (xhr.status === 401 || xhr.status === 403) {
        window.location.href = '/user/login';
      } else {
        console.error('리뷰 좋아요 오류:', xhr.status, xhr.responseText);
        alert('좋아요 처리 중 오류가 발생했어요.');
      }
    })
    .always(function () {
      btn.prop('disabled', false);
    });
  });
});
