// /js/review-list.js
(function () {
  const form = document.getElementById('reviewCreateForm');
  if (!form) return;

  const cafeId = form.getAttribute('data-cafe-id');
  const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

  const ratingInputs = Array.from(form.querySelectorAll('input[name="rating"]'));
  const ratingDisplay = document.querySelector('[data-rating-display]');
  const ratingValue = document.getElementById('createRatingValue');
  const starWrapper = form.querySelector('.rating-picker__star-wrapper');
  const imageInput = document.getElementById('createImages');
  const imageList = document.getElementById('reviewImageList');
  const certificationNotice = document.getElementById('certifyRequiredMessage');
  const certificationSuccessMessage = document.getElementById('certifySuccessMessage');
  const reviewCreateSection = form.closest('.review-section.review-create');
  const certifyButton = reviewCreateSection ? reviewCreateSection.querySelector('.review-create__certify-btn') : null;
  const selectedFiles = [];
  const supportsDataTransfer = typeof DataTransfer !== 'undefined';
  const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  const MAX_FILE_COUNT = imageInput ? Number.parseInt(imageInput.getAttribute('data-max-files') || '10', 10) : 10;

  const isLocationCertified = () => {
    if (!cafeId) return true;
    return localStorage.getItem('certifiedCafe_' + cafeId) === 'true';
  };

  const setElementVisibility = (element, visible) => {
    if (!element) return;
    if (visible) {
      element.classList.remove('d-none');
      element.classList.add('d-block');
    } else {
      element.classList.remove('d-block');
      element.classList.add('d-none');
    }
  };

  const showCertificationNotice = () => {
    setElementVisibility(certificationNotice, true);
    setElementVisibility(certificationSuccessMessage, false);
  };

  const hideCertificationNotice = () => {
    setElementVisibility(certificationNotice, false);
  };

  const showCertificationSuccess = () => {
    setElementVisibility(certificationSuccessMessage, true);
  };

  const hideCertificationSuccess = () => {
    setElementVisibility(certificationSuccessMessage, false);
  };

  const syncCertificationNotice = () => {
    const certified = isLocationCertified();
    if (certified) {
      hideCertificationNotice();
      showCertificationSuccess();
      if (reviewCreateSection) {
        reviewCreateSection.classList.remove('review-create--locked');
      }
      form.removeAttribute('aria-disabled');
    } else {
      hideCertificationSuccess();
      if (reviewCreateSection && !reviewCreateSection.classList.contains('review-create--locked')) {
        reviewCreateSection.classList.add('review-create--locked');
      }
      form.setAttribute('aria-disabled', 'true');
    }
  };

  let lastAlertAt = 0;
  const notifyCertificationRequired = () => {
    const now = Date.now();
    if (now - lastAlertAt < 1500) return;
    lastAlertAt = now;
    showCertificationNotice();
    if (certifyButton) {
      certifyButton.focus();
    }
  };

  const shouldBlockInputTarget = (target) => {
    if (!(target instanceof HTMLElement)) return false;
    if (target.closest('[data-allow-without-certification]')) return false;
    if (target instanceof HTMLTextAreaElement) return true;
    if (target instanceof HTMLInputElement) {
      const blockTypes = ['text', 'search', 'email', 'tel', 'url', 'number', 'password'];
      const type = (target.getAttribute('type') || 'text').toLowerCase();
      return blockTypes.includes(type);
    }
    return false;
  };

  const handleBlockedInput = (event) => {
    const target = event.target;
    if (!shouldBlockInputTarget(target) || isLocationCertified()) return;
    event.preventDefault();
    event.stopPropagation();
    notifyCertificationRequired();
    if (target instanceof HTMLElement) {
      target.blur();
    }
  };

  form.addEventListener('beforeinput', handleBlockedInput, true);
  form.addEventListener('paste', handleBlockedInput, true);
  form.addEventListener('keydown', (event) => {
    if (event.key === 'Tab' || event.key === 'Shift' || event.key === 'Escape') return;
    if (event.ctrlKey || event.metaKey || event.altKey) return;
    handleBlockedInput(event);
  }, true);

  syncCertificationNotice();

  window.addEventListener('focus', syncCertificationNotice);
  window.addEventListener('storage', (event) => {
    if (!cafeId) return;
    if (event.key === 'certifiedCafe_' + cafeId) {
      syncCertificationNotice();
    }
  });

  const refreshReviewSectionUI = () => {
    if (typeof window.syncReviewImageAspectRatios === 'function') {
      window.syncReviewImageAspectRatios();
    }
    if (typeof window.initReviewImageSlider === 'function') {
      window.initReviewImageSlider();
    }
    if (typeof window.initReviewImageMagnifier === 'function') {
      window.initReviewImageMagnifier();
    }
  };
  window.refreshReviewSectionUI = refreshReviewSectionUI;

  const syncSelectedFileView = () => {
    if (!imageList) return;
    imageList.innerHTML = '';

    if (!selectedFiles.length) {
      const empty = document.createElement('li');
      empty.className = 'review-create__file-empty';
      empty.textContent = '선택된 이미지가 없습니다.';
      imageList.appendChild(empty);
      return;
    }

    selectedFiles.forEach((file, index) => {
      const item = document.createElement('li');
      item.className = 'review-create__file-item';

      const name = document.createElement('span');
      name.className = 'review-create__file-name';
      name.textContent = file.name;

      const remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'review-create__file-remove';
      remove.setAttribute('data-index', String(index));
      remove.setAttribute('aria-label', `${file.name} 삭제`);
      remove.innerHTML = '&times;';

      item.append(name, remove);
      imageList.appendChild(item);
    });
  };

  const syncImageInput = () => {
    if (!imageInput || !supportsDataTransfer) return;
    const dt = new DataTransfer();
    selectedFiles.forEach((file) => dt.items.add(file));
    imageInput.files = dt.files;
  };

  if (imageList) {
    imageList.addEventListener('click', (event) => {
      const target = event.target instanceof Element ? event.target.closest('.review-create__file-remove') : null;
      if (!target) return;
      const index = Number.parseInt(target.getAttribute('data-index') || '', 10);
      if (!Number.isInteger(index) || index < 0 || index >= selectedFiles.length) return;
      selectedFiles.splice(index, 1);
      syncImageInput();
      syncSelectedFileView();
      if (imageInput) {
        imageInput.focus();
      }
    });
  }

  if (imageInput) {
    syncSelectedFileView();

    imageInput.addEventListener('change', (event) => {
      const input = event.currentTarget;
      if (!(input instanceof HTMLInputElement) || !input.files) return;

      const newFiles = Array.from(input.files);
      if (!newFiles.length) return;

      const messages = [];

      newFiles.forEach((file) => {
        if (file.size > MAX_FILE_SIZE) {
          messages.push(`"${file.name}"은(는) 10MB를 초과하여 제외되었습니다.`);
          return;
        }
        if (selectedFiles.length >= MAX_FILE_COUNT) {
          messages.push(`이미지는 최대 ${MAX_FILE_COUNT}장까지 선택할 수 있습니다.`);
          return;
        }
        const isDuplicate = selectedFiles.some((existing) => existing.name === file.name && existing.size === file.size && existing.lastModified === file.lastModified);
        if (isDuplicate) {
          messages.push(`"${file.name}"은(는) 이미 선택된 이미지입니다.`);
          return;
        }
        selectedFiles.push(file);
      });

      syncImageInput();
      syncSelectedFileView();

      input.value = '';

      if (messages.length) {
        alert(messages.filter((value, index, arr) => arr.indexOf(value) === index).join('\n'));
      }
    });

    form.addEventListener('reset', () => {
      selectedFiles.length = 0;
      syncImageInput();
      syncSelectedFileView();
    });
  }

  const ratingValues = ratingInputs
    .map((input) => Number.parseFloat(input.value))
    .filter((value) => Number.isFinite(value))
    .sort((a, b) => a - b);

  const minRating = ratingValues.length ? ratingValues[0] : 0;
  const maxRating = ratingValues.length ? ratingValues[ratingValues.length - 1] : 5;

  const syncRating = (value) => {
    const numericValue = Number.parseFloat(value);
    const fallback = Number.isFinite(numericValue) ? numericValue : minRating;
    const rating = Math.min(maxRating, Math.max(minRating, fallback));
    if (ratingDisplay) {
      ratingDisplay.style.setProperty('--rating', rating.toString());
    }
    if (ratingValue) {
      ratingValue.textContent = rating.toFixed(1);
    }
  };

  const getCheckedRating = () => {
    const checked = ratingInputs.find((input) => input.checked);
    if (checked) {
      return checked.value;
    }
    const defaultChecked = ratingInputs.find((input) => input.defaultChecked);
    if (defaultChecked) {
      return defaultChecked.value;
    }
    return ratingValues.length ? ratingValues[Math.floor(ratingValues.length / 2)] : maxRating;
  };

  if (ratingInputs.length) {
    syncRating(getCheckedRating());
    ratingInputs.forEach((input) => {
      input.addEventListener('change', () => {
        syncRating(input.value);
      });
    });
  } else {
    syncRating(minRating);
  }

  const findRatingInput = (value) => {
    const numericValue = Number.parseFloat(value);
    if (!Number.isFinite(numericValue)) return null;
    return ratingInputs.find((input) => {
      const inputValue = Number.parseFloat(input.value);
      return Number.isFinite(inputValue) && Math.abs(inputValue - numericValue) < 0.001;
    }) || null;
  };

  const getRatingFromPosition = (clientX) => {
    if (!starWrapper || !ratingValues.length) return null;
    const rect = starWrapper.getBoundingClientRect();
    const width = rect.width;
    if (!width || Number.isNaN(width)) return null;
    const ratio = (clientX - rect.left) / width;
    const clamped = Math.min(0.999999, Math.max(0, ratio));
    const index = Math.min(ratingValues.length - 1, Math.floor(clamped * ratingValues.length));
    return ratingValues[index] ?? null;
  };

  if (starWrapper && ratingInputs.length) {
    starWrapper.addEventListener('pointermove', (event) => {
      if (event.pointerType !== 'mouse' && event.pointerType !== 'pen') return;
      const hoveredRating = getRatingFromPosition(event.clientX);
      if (hoveredRating != null) {
        syncRating(hoveredRating);
      }
    });

    starWrapper.addEventListener('pointerleave', () => {
      syncRating(getCheckedRating());
    });

    starWrapper.addEventListener('pointerdown', (event) => {
      if (event.button != null && event.pointerType === 'mouse' && event.button !== 0) {
        return;
      }
      const selectedRating = getRatingFromPosition(event.clientX);
      if (selectedRating == null) return;

      const targetInput = findRatingInput(selectedRating);
      if (!targetInput) return;

      event.preventDefault();
      targetInput.checked = true;
      targetInput.dispatchEvent(new Event('change', { bubbles: true }));
      syncRating(selectedRating);
    });

    starWrapper.addEventListener('click', (event) => {
      if (event.target.tagName === 'LABEL' && event.detail !== 0) {
        event.preventDefault();
      }
    });
  }

  form.addEventListener('submit', async (e) => {
    if (cafeId && localStorage.getItem('certifiedCafe_' + cafeId) !== 'true') {
      e.preventDefault();
      if (confirm('위치인증을 먼저 해주세요. 위치 인증 페이지로 이동하시겠습니까?')) {
        window.location.href = '/cafes/' + cafeId + '/reviews/location';
      }
      return;
    }
    if (!form.checkValidity()) {
      e.preventDefault();
      e.stopPropagation();
      form.classList.add('was-validated');
      return;
    }
    e.preventDefault();

    const fd = new FormData(form); // rating, content, images[]
    // 기본 FormData에는 <input type="file">가 가진 FileList가 추가되지만,
    // Safari(iOS 등)처럼 DataTransfer를 지원하지 않는 환경에서는
    // 커스텀 선택 로직 때문에 FileList가 비어버릴 수 있다.
    // selectedFiles 배열을 신뢰 가능한 단일 소스로 사용하도록 강제한다.
    fd.delete('images');
    selectedFiles.forEach((file) => fd.append('images', file));

    try {
      // 1) AJAX로 리뷰 생성 (컨트롤러가 JSON 반환)
      const res = await fetch(form.getAttribute('action'), {
        method: 'POST',
        headers: {
          'X-Requested-With': 'XMLHttpRequest',
          ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
          // Content-Type 은 FormData 사용 시 자동 지정됩니다.
        },
        body: fd
      });

      if (!res.ok) {
        // 컨트롤러는 400일 때 에러 목록 JSON/텍스트를 내려줄 수 있음
        const text = await res.text();
        throw new Error(text || `HTTP ${res.status}`);
      }

      const data = await res.json();
      if (!data.ok) throw new Error(data.message || '등록 실패');

      // 2) 최신 리뷰 섹션 로드해서 교체
      const oldSection = document.getElementById('reviewsSection');
      const sectionUrl = oldSection?.getAttribute('data-review-section-url');
      if (!oldSection || !sectionUrl) throw new Error('섹션 로드 URL을 찾을 수 없습니다.');
      const secRes = await fetch(sectionUrl, {
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
      });
      if (!secRes.ok) throw new Error(`섹션 로드 실패: HTTP ${secRes.status}`);

      const html = await secRes.text();
      const wrapper = document.createElement('div');
      wrapper.innerHTML = html.trim();

      const newSection = wrapper.querySelector('#reviewsSection');
      if (!newSection || !oldSection) throw new Error('섹션 엘리먼트를 찾을 수 없습니다.');

      oldSection.replaceWith(newSection);
      refreshReviewSectionUI();

      // 3) 폼 리셋
      form.reset();
      form.classList.remove('was-validated');
      selectedFiles.length = 0;
      syncImageInput();
      syncSelectedFileView();
      if (ratingInputs.length) {
        syncRating(getCheckedRating());
      } else {
        syncRating(minRating);
      }
      if (cafeId) {
        localStorage.removeItem('certifiedCafe_' + cafeId);
        syncCertificationNotice();
      }
    } catch (err) {
      console.error(err);
      alert('리뷰 등록에 실패했어요. 잠시 후 다시 시도해 주세요.');
    }
  });
})();

