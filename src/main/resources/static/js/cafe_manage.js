// /static/js/cafe_manage.js
(function () {
  "use strict";
  console.log("[cafe_manage] LOADED", new Date().toISOString(), location.pathname, location.search);

  // ================== BFCache / 뒤로가기 새로고침 방지 ==================
  try {
    var nav = performance.getEntriesByType('navigation')[0];
    if (nav && nav.type === 'back_forward') {
      console.log("[cafe_manage] back_forward detected → reload");
      location.reload();
      return;
    }
  } catch (_) {}

  window.addEventListener('pageshow', function (e) {
    console.log("[cafe_manage] pageshow", { persisted: e.persisted, flag: sessionStorage.getItem('force-reload') });
    if (e.persisted || sessionStorage.getItem('force-reload') === '1') {
      sessionStorage.removeItem('force-reload');
      location.reload();
    }
  });

  window.addEventListener('pagehide', function (e) {
    console.log("[cafe_manage] pagehide", { persisted: e.persisted });
    if (e.persisted) {
      sessionStorage.setItem('force-reload', '1');
    }
  });

  // ================== URL에서 reauth 파라미터 제거 ==================
  (function cleanUrl() {
    try {
      var u = new URL(window.location.href);
      if (u.searchParams.has('reauth')) {
        u.searchParams.delete('reauth');
        var q = u.searchParams.toString();
        var newUrl = u.pathname + (q ? "?" + q : "") + u.hash;
        history.replaceState(null, '', newUrl);
        console.log("[cafe_manage] removed reauth from URL");
      }
    } catch (err) {
      console.error("[cafe_manage] URL cleanup error:", err);
    }
  })();

  // ==================== 공통 유틸 ====================
  function qs(sel, root){ return (root||document).querySelector(sel); }
  function escapeHtml(s){ return (s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
  function formatPrice(v){ try{ const n = Number(v||0); return n.toLocaleString() + '원'; }catch{ return (v??'') + '원'; } }
  function getCsrf() {
    const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    const token  = document.querySelector('meta[name="_csrf"]')?.content || '';
    return { header, token };
  }

  // ==================== 메뉴 모달 열기/닫기 ====================
  function openMenuModalImpl() {
    const m = qs('#menuModal');
    if (!m) return;
    m.style.display = 'block';
    ensureMenuFormBound();   // 모달 열릴 때도 안전하게 바인딩
    reloadMenus();
  }
  function closeMenuModalImpl() {
    const m = qs('#menuModal');
    if (!m) return;
    m.style.display = 'none';
  }
  window.openMenuModal = openMenuModalImpl;
  window.closeMenuModal = closeMenuModalImpl;

  // ==================== 목록 로드/렌더링(모달) ====================
  async function reloadMenus() {
    const cafeId = Number(qs('#cafeId')?.value || 0);
    const cont = qs('#menuList');
    if (!cont) return;
    if (!cafeId) {
      cont.innerHTML = '<div class="empty">카페 정보가 없습니다.</div>';
      return;
    }
    try {
      const res = await fetch(`/api/mypage/cafe/menu?cafeId=${encodeURIComponent(cafeId)}`, {
        headers: { 'Accept':'application/json' },
        credentials: 'same-origin'
      });
      if (!res.ok) throw new Error('목록 불러오기 실패');
      const list = await res.json();
      renderMenuList(list);
    } catch (e) {
      console.error(e);
      cont.innerHTML = '<div class="empty">메뉴를 불러오지 못했습니다.</div>';
    }
  }

  function renderMenuList(list) {
    const cont = qs('#menuList');
    if (!cont) return;
    if (!list || list.length === 0) {
      cont.innerHTML = '<div class="empty">등록된 메뉴가 없습니다.</div>';
      return;
    }
    cont.innerHTML = `
      <div class="row" style="font-weight:700; border-bottom:2px solid #ddd;">
        <div>메뉴</div><div>가격</div><div>설명</div><div>삭제</div>
      </div>
      ${list.map(m => `
        <div class="row" data-id="${m.id}">
          <div class="name">${escapeHtml(m.name || '')}</div>
          <div>${formatPrice(m.price)}</div>
          <div>${escapeHtml(m.description || '')}</div>
          <div><button class="btn danger btn-sm" onclick="deleteMenu(${m.id})">X</button></div>
        </div>
      `).join('')}
    `;
  }

  // ==================== 등록/삭제 ====================
  let __menuSubmitInFlight = false;

  async function doSubmitMenu() {
    if (__menuSubmitInFlight) return;

    const cafeId = Number(qs('#cafeId')?.value || 0);
    const name   = qs('#menuName')?.value?.trim();
    const price  = qs('#menuPrice')?.value;
    const desc   = qs('#menuDesc')?.value?.trim() || '';

    if (!cafeId) return alert('카페 정보가 없습니다.');
    if (!name)   return alert('메뉴 이름을 입력하세요.');
    if (!price)  return alert('가격을 입력하세요.');

    const btn = qs('#menuSubmitBtn');
    const { header, token } = getCsrf();

    try {
      __menuSubmitInFlight = true;
      btn && (btn.disabled = true);

      const res = await fetch('/api/mypage/cafe/menu', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
          [header]: token
        },
        credentials: 'same-origin',
        body: new URLSearchParams({ cafeId, name, price, description: desc }).toString()
      });
      if (!res.ok) throw new Error(await res.text() || '등록 실패');

      const form = qs('#menuForm');
      form && form.reset();
      await reloadMenus();
      await reloadMenuSummary();
      qs('#menuName')?.focus();
    } catch (err) {
      console.error(err);
      alert('등록에 실패했습니다: ' + (err.message || err));
    } finally {
      __menuSubmitInFlight = false;
      btn && (btn.disabled = false);
    }
  }
  // 전역 노출(HTML onclick 보호용)
  window.doSubmitMenu = doSubmitMenu;

  window.deleteMenu = async function (menuId) {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    const { header, token } = getCsrf();
    try {
      const res = await fetch(`/api/mypage/cafe/menu/${menuId}`, {
        method: 'DELETE',
        headers: { [header]: token },
        credentials: 'same-origin'
      });
      if (!res.ok) throw new Error(await res.text() || '삭제 실패');

      await reloadMenus();
      await reloadMenuSummary();
    } catch (e) {
      console.error(e);
      alert('삭제에 실패했습니다: ' + e.message);
    }
  };

  // ==================== 폼/버튼 바인딩 보장 ====================
  function ensureMenuFormBound() {
    const form = qs('#menuForm');
    if (!form) return;

    // form submit (submit 타입 버튼을 쓰더라도 한 번만 처리)
    if (!form.dataset.bound) {
      form.addEventListener('submit', function(e){
        e.preventDefault();
        doSubmitMenu();
      });
      form.dataset.bound = '1';
    }

    // 버튼 클릭 (type="button"이어도 동작)
    const btn = qs('#menuSubmitBtn');
    if (btn && !btn.dataset.bound) {
      btn.addEventListener('click', function(e){
        e.preventDefault();
        doSubmitMenu();
      });
      btn.dataset.bound = '1';
    }
  }

  // ==================== 요약 섹션 갱신 ====================
  async function reloadMenuSummary() {
    const cafeId = Number(qs('#cafeId')?.value || 0);
    if (!cafeId) return;
    try {
      const res = await fetch(`/api/mypage/cafe/menu?cafeId=${encodeURIComponent(cafeId)}`, {
        headers: { 'Accept':'application/json' },
        credentials: 'same-origin'
      });
      if (!res.ok) throw new Error('요약 불러오기 실패');
      const items = await res.json();
      renderMenuSummary(items.slice(0, 5));
    } catch (e) {
      console.error(e);
      renderMenuSummary([]);
    }
  }

  function renderMenuSummary(items) {
    const box = qs('#menuSummary');
    if (!box) return;
    if (!items || items.length === 0) {
      box.innerHTML = `<div class="item">등록된 메뉴가 없습니다.</div>`;
      return;
    }
    box.innerHTML = items.map(m => `
      <div class="item">
        <div style="flex:1">
          <div style="font-weight:700">${escapeHtml(m.name || '')}</div>
          <div class="muted">
            <span>${escapeHtml(m.description || '설명 없음')}</span>
            · <span>${formatPrice(m.price)}</span>
          </div>
        </div>
        <span class="chip">판매중</span>
      </div>
    `).join('');
  }

  // ==================== 초기화 ====================
  document.addEventListener('DOMContentLoaded', function(){
    ensureMenuFormBound();
    reloadMenuSummary(); // 페이지 진입 시 요약 섹션 동기화
  });

})();
