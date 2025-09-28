// /js/cafe-register.js

// ---------- 업로드(프리뷰/검증/누적) ----------
document.addEventListener("DOMContentLoaded", () => {
  const pickBtn   = document.getElementById("btn-photo-pick");
  const inputEl   = document.getElementById("photos");
  const previewEl = document.getElementById("photo-preview");

  // 사진 등록하기 버튼 → 파일 선택
  if (pickBtn && inputEl) {
    pickBtn.addEventListener("click", (e) => {
      e.preventDefault();
      inputEl.click();
    });
  }

  if (!(inputEl && previewEl)) return;

  const MAX_FILES   = 10;
  const MAX_SIZE_MB = 5;

  // 누적 버퍼 (브라우저 지원 시)
  let buffer;
  try { buffer = new DataTransfer(); } catch { buffer = null; }

  // 프리뷰 타일 생성(+ 삭제)
  function appendPreviewTile(file) {
    const url  = URL.createObjectURL(file);

    const tile = document.createElement("div");
    tile.className = "photo-tile";
    tile.style.display = "inline-flex";
    tile.style.flexDirection = "column";
    tile.style.alignItems = "center";
    tile.style.gap = "6px";

    const img = document.createElement("img");
    img.src = url;
    img.alt = file.name || "image";
    img.className = "preview-img";
    img.style.maxWidth  = "150px";
    img.style.maxHeight = "150px";
    img.style.objectFit = "cover";
    img.style.borderRadius = "8px";

    const name = document.createElement("span");
    name.textContent = file.name || "이미지";
    name.style.fontSize = "12px";
    name.style.color = "#6b7280";
    name.style.maxWidth = "150px";
    name.style.whiteSpace = "nowrap";
    name.style.overflow = "hidden";
    name.style.textOverflow = "ellipsis";
    name.style.textAlign = "center";

    const del = document.createElement("button");
    del.type = "button";
    del.textContent = "삭제";
    del.className = "btn btn-sm btn-outline-secondary";
    del.addEventListener("click", () => {
      // 누적 버퍼에서 동일 파일 제거
      if (buffer) {
        const items = Array.from(buffer.items);
        const idx = items.findIndex(it => {
          const f = it.getAsFile();
          return f && f.name === file.name && f.size === file.size && f.type === file.type;
        });
        if (idx > -1) {
          buffer.items.remove(idx);
          inputEl.files = buffer.files;
        }
      }
      URL.revokeObjectURL(url);
      tile.remove();
    });

    tile.appendChild(img);
    tile.appendChild(name);
    tile.appendChild(del);
    previewEl.appendChild(tile);
  }

  // 파일 선택 → 검증 → 프리뷰 추가 → 누적 반영
  inputEl.addEventListener("change", (e) => {
    const selected = Array.from(e.target.files || []);
    if (!selected.length) return;

    const currentCount = buffer ? buffer.files.length : previewEl.querySelectorAll("img.preview-img").length;
    if (currentCount + selected.length > MAX_FILES) {
      alert(`이미지는 최대 ${MAX_FILES}장까지 업로드할 수 있어요.`);
      inputEl.value = "";
      return;
    }

    const tooLarge = selected.find(f => f.size > MAX_SIZE_MB * 1024 * 1024);
    if (tooLarge) {
      alert(`각 이미지는 ${MAX_SIZE_MB}MB 이하여야 해요: ${tooLarge.name}`);
      inputEl.value = "";
      return;
    }

    selected.forEach(file => {
      if (!file.type || !file.type.startsWith("image/")) return;
      if (buffer) buffer.items.add(file);
      appendPreviewTile(file);
    });

    if (buffer) {
      inputEl.files = buffer.files; // 누적 반영
    }
  });
});

// ---------- 개별 즉시 삭제 ----------

async function deletePhoto(btn) {
  console.log('deletePhoto click'); // 눌림 확인용
  const url = btn.dataset.deleteUrl || btn.getAttribute('data-delete-url');
  if (!url) { alert('삭제 URL이 없습니다.'); return; }
  if (!confirm('이 사진을 삭제할까요?')) return;

  const token =
    document.querySelector('input#csrfToken')?.value ||
    document.querySelector('meta[name="_csrf"]')?.content || '';
  const headerName =
    document.querySelector('input#csrfHeaderName')?.value ||
    document.querySelector('meta[name="_csrf_header"]')?.content ||
    'X-CSRF-TOKEN';

  try {
    const res = await fetch(url, {
      method: 'DELETE',
      headers: { [headerName]: token },
      credentials: 'same-origin',
    });

    if (res.ok) {
      btn.closest('.photo-card')?.remove();
      return;
    }
    if (res.redirected || (res.status >= 300 && res.status < 400)) {
      alert('삭제 요청이 리다이렉트되었습니다. 보안/인터셉터 설정을 확인하세요.');
      return;
    }
    const text = await res.text().catch(()=>'');
    alert(`삭제 실패: ${res.status} ${text}`);
  } catch (e) {
    console.error(e);
    alert('네트워크 오류로 삭제에 실패했습니다.');
  }
}

// ✅ inline onclick에서 찾도록 전역에 노출
window.deletePhoto = deletePhoto;

