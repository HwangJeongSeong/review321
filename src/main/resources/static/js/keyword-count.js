  document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("filterForm");
    const checkboxes = document.querySelectorAll("input[name='keyList'][form='filterForm']");
    const counter = document.getElementById("selectedCount");
    const countText = document.getElementById("disable-text");
    const resetBtn = document.getElementById("resetBtn");

    // 선택 개수 세기
    function updateCount() {
      const count = Array.from(checkboxes).filter(cb => cb.checked).length;
      counter.textContent = count;
      if (count > 0) {
        counter.classList.add("active");   // 강조 효과 ON
        countText.classList.add("active");   // 강조 효과 ON
      } else {
        counter.classList.remove("active"); // 기본 상태
        countText.classList.remove("active");   // 강조 효과 ON
      }


    }

    // 체크박스 변화 감지 → 카운트 업데이트
    checkboxes.forEach(cb => cb.addEventListener("change", updateCount));

    // 페이지 로드 시 초기 카운트 반영 (selectedKeys 있을 때 대비)
    updateCount();

    // 초기화 버튼 눌렀을 때 체크 해제 + 카운트 리셋
    resetBtn.addEventListener("click", () => {
      updateCount();
    });
  });