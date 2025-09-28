// review-edit.js
(function () {
  const form = document.getElementById('reviewEditForm');
  if (!form) return;

  const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  const DEFAULT_MAX_FILE_COUNT = 5;
  const imageInput = document.getElementById('editImages');
  const imageList = document.getElementById('reviewEditFileList');
  const supportsFileApi = typeof DataTransfer !== 'undefined';
  const selectedFiles = [];

  const getMaxFileCount = () => {
    if (!imageInput) return DEFAULT_MAX_FILE_COUNT;
    const parsed = Number.parseInt(imageInput.getAttribute('data-max-files') || '', 10);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : DEFAULT_MAX_FILE_COUNT;
  };

  const getExistingImageInputs = () => {
    return Array.from(document.querySelectorAll('.review-edit__existing-item input[name="imageUrl"]'));
  };

  // ===== 글자 수 카운터 =====
  const content = document.getElementById('content');
  const counter = document.getElementById('contentCounter');
  if (content && counter) {
    const updateCount = () => {
      const length = (content.value || '').trim().length;
      counter.textContent = `${length}자 / 최소 5자`;
      counter.style.color = length >= 5 ? '#10b981' : '#6b7280';
    };
    content.addEventListener('input', updateCount);
    updateCount();
  }

  // ===== 별점 동기화 =====
  const ratingInputs = Array.from(form.querySelectorAll('input[name="rating"]'));
  const ratingDisplay = form.querySelector('[data-rating-display]');
  const ratingValue = document.getElementById('editRatingValue');
  const ratingChoices = form.querySelector('.rating-picker__choices');
  const starWrapper = form.querySelector('.rating-picker__star-wrapper');

  const ratingValues = ratingInputs
    .map((input) => Number.parseFloat(input.value))
    .filter((value) => Number.isFinite(value))
    .sort((a, b) => a - b);

  const minRating = ratingValues.length ? ratingValues[0] : 0;
  const maxRating = ratingValues.length ? ratingValues[ratingValues.length - 1] : 5;

  const syncRatingDisplay = (value) => {
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
    if (checked) return checked.value;
    const defaultChecked = ratingInputs.find((input) => input.defaultChecked);
    if (defaultChecked) return defaultChecked.value;
    return ratingValues.length ? ratingValues[Math.floor(ratingValues.length / 2)] : maxRating;
  };

  if (ratingInputs.length) {
    syncRatingDisplay(getCheckedRating());
    ratingInputs.forEach((input) => {
      input.addEventListener('change', () => {
        syncRatingDisplay(input.value);
      });
    });
  }

  const interactiveRatingTargets = ratingInputs
    .map((input) => {
      if (!input.id) return null;
      const label = form.querySelector(`label[for="${input.id}"]`);
      if (!label) return null;
      return { input, label };
    })
    .filter((entry) => entry != null);

  const resetRatingDisplay = () => {
    syncRatingDisplay(getCheckedRating());
  };

  interactiveRatingTargets.forEach(({ input, label }) => {
    const showPreview = () => {
      syncRatingDisplay(input.value);
    };

    label.addEventListener('pointerenter', showPreview);
    label.addEventListener('mouseenter', showPreview);
    input.addEventListener('focus', showPreview);

    label.addEventListener('pointerdown', showPreview);
    input.addEventListener('blur', resetRatingDisplay);
  });

  if (ratingChoices) {
    ratingChoices.addEventListener('pointerleave', resetRatingDisplay);
    ratingChoices.addEventListener('mouseleave', resetRatingDisplay);
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
      if (event.pointerType && event.pointerType !== 'mouse' && event.pointerType !== 'pen') return;
      const hoveredRating = getRatingFromPosition(event.clientX);
      if (hoveredRating != null) {
        syncRatingDisplay(hoveredRating);
      }
    });

    starWrapper.addEventListener('pointerleave', resetRatingDisplay);

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
      syncRatingDisplay(selectedRating);
    });

    starWrapper.addEventListener('click', (event) => {
      if (event.target instanceof Element && event.target.tagName === 'LABEL' && event.detail !== 0) {
        event.preventDefault();
      }
    });
  }

  // ===== 새 이미지 파일 목록 관리 =====
  const renderFileList = () => {
    if (!imageList) return;
    imageList.innerHTML = '';

    const filesForDisplay = supportsFileApi
      ? selectedFiles
      : (imageInput && imageInput.files ? Array.from(imageInput.files) : []);

    if (!filesForDisplay.length) {
      const empty = document.createElement('li');
      empty.className = 'review-edit__file-empty';
      empty.textContent = '선택된 이미지가 없습니다.';
      imageList.appendChild(empty);
      return;
    }

    filesForDisplay.forEach((file, index) => {
      const item = document.createElement('li');
      item.className = 'review-edit__file-item';

      const name = document.createElement('span');
      name.className = 'review-edit__file-name';
      name.textContent = file.name;

      const remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'review-edit__file-remove';
      remove.setAttribute('data-index', String(index));
      remove.setAttribute('aria-label', `${file.name} 삭제`);
      remove.innerHTML = '&times;';

      item.append(name, remove);
      imageList.appendChild(item);
    });
  };

  const syncImageInput = () => {
    if (!imageInput || !supportsFileApi) return false;
    const dt = new DataTransfer();
    selectedFiles.forEach((file) => dt.items.add(file));
    imageInput.files = dt.files;
    return true;
  };

  if (imageList) {
    imageList.addEventListener('click', (event) => {
      const target = event.target instanceof Element ? event.target.closest('.review-edit__file-remove') : null;
      if (!target) return;
      if (!supportsFileApi) {
        if (imageInput) {
          imageInput.value = '';
          selectedFiles.length = 0;
          renderFileList();
          imageInput.focus();
        }
        return;
      }

      const index = Number.parseInt(target.getAttribute('data-index') || '', 10);
      if (!Number.isInteger(index) || index < 0 || index >= selectedFiles.length) return;
      selectedFiles.splice(index, 1);
      syncImageInput();
      renderFileList();
      if (imageInput) {
        imageInput.focus();
      }
    });
  }

  if (imageInput) {
    imageInput.addEventListener('change', (event) => {
      const input = event.currentTarget;
      if (!(input instanceof HTMLInputElement) || !input.files) return;

      const newFiles = Array.from(input.files);
      if (!newFiles.length) return;

      const messages = [];
      const maxCount = getMaxFileCount();
      const existingCount = getExistingImageInputs().length;

      if (!supportsFileApi) {
        if (existingCount + newFiles.length > maxCount) {
          messages.push(`이미지는 최대 ${maxCount}장까지 선택할 수 있습니다. 등록된 이미지를 삭제한 뒤 다시 시도해 주세요.`);
        }

        newFiles.forEach((file) => {
          if (file.size > MAX_FILE_SIZE) {
            messages.push(`"${file.name}"은(는) 10MB를 초과하여 제외되었습니다.`);
          }
        });

        if (messages.length) {
          input.value = '';
        }

        renderFileList();

        if (messages.length) {
          alert(messages.filter((value, index, arr) => arr.indexOf(value) === index).join('\n'));
        }

        return;
      }

      newFiles.forEach((file) => {
        if (file.size > MAX_FILE_SIZE) {
          messages.push(`"${file.name}"은(는) 10MB를 초과하여 제외되었습니다.`);
          return;
        }
        if (existingCount + selectedFiles.length >= maxCount) {
          messages.push(`이미지는 최대 ${maxCount}장까지 선택할 수 있습니다. 등록된 이미지를 삭제한 뒤 다시 시도해 주세요.`);
          return;
        }
        const isDuplicate = selectedFiles.some((existing) => existing.name === file.name && existing.size === file.size && existing.lastModified === file.lastModified);
        if (isDuplicate) {
          messages.push(`"${file.name}"은(는) 이미 선택된 이미지입니다.`);
          return;
        }
        selectedFiles.push(file);
      });

      const synced = syncImageInput();
      renderFileList();
      if (synced) {
        input.value = '';
        syncImageInput();
      }

      if (messages.length) {
        alert(messages.filter((value, index, arr) => arr.indexOf(value) === index).join('\n'));
      }
    });
  }

  renderFileList();

  // ===== 기존 이미지 삭제 =====
  const removeExistingImage = (button) => {
    const wrapper = button.closest('.review-edit__existing-item');
    if (!wrapper) return;
    wrapper.remove();
  };

  const existingContainer = document.getElementById('existingImageList');
  if (existingContainer) {
    existingContainer.addEventListener('click', (event) => {
      const button = event.target instanceof Element ? event.target.closest('.review-edit__existing-remove') : null;
      if (!button) return;
      removeExistingImage(button);
    });
  }

  // ===== 제출 시 검증 =====
  form.addEventListener('submit', (event) => {
    if (!form.checkValidity()) {
      event.preventDefault();
      event.stopPropagation();
      form.classList.add('was-validated');
      return;
    }

    const maxCount = getMaxFileCount();
    const existingCount = getExistingImageInputs().length;
    if (existingCount + selectedFiles.length > maxCount) {
      event.preventDefault();
      event.stopPropagation();
      alert(`이미지는 최대 ${maxCount}장까지만 업로드할 수 있어요.`);
    }
  });
})();
