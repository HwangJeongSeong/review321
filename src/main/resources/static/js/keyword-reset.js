 (function () {
    const form     = document.getElementById('filterForm');
    const resetBtn = document.getElementById('resetBtn');

    // "filterForm"에 소속된 모든 컨트롤을 한 번에 선택 (폼 내부 + form 속성)
    const allControls = () => document.querySelectorAll(
      '#filterForm input, #filterForm select, [form="filterForm"]'
    );

    resetBtn.addEventListener('click', function () {
      // 1) 브라우저 기본 초기화 (기본값으로)
      form.reset();

      // 2) 서버 템플릿(th:value/th:checked)로 유지된 상태 강제 초기화
      allControls().forEach(el => {
        const tag = el.tagName;
        const type = (el.type || '').toLowerCase();

        if (tag === 'INPUT') {
          if (type === 'text' || type === 'search' || type === 'hidden') {
            el.value = '';                 // 검색어/히든 값 비우기(원하면 유지)
          } else if (type === 'checkbox' || type === 'radio') {
            el.checked = false;            // 체크 해제
            el.removeAttribute('checked'); // 초기 checked 속성 제거(충돌 방지)
          }
        } else if (tag === 'SELECT') {
          el.selectedIndex = 0;            // 첫 옵션으로
        }
      });

      // 3) 키워드 라벨의 active 클래스 제거 (UI 색상 리셋)
      document.querySelectorAll('.list_label.active').forEach(lb => lb.classList.remove('active'));

      // 4) 보조 UI(선택 갯수 뱃지 등) 초기화
      const cntEl = document.getElementById('selectedCount');
      if (cntEl) cntEl.textContent = '0';

      // 5) 필요 시 즉시 재조회(화면 갱신). 원치 않으면 이 줄 주석 처리
      form.submit();
    });
  })();