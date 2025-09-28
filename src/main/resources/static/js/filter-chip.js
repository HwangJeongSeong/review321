const form = document.getElementById("filterForm");
const selectedFilters = document.getElementById("selectedFilters");
const counter = document.getElementById("selectedCount");
const checkboxes = document.querySelectorAll("input[name='keyList']");

function updateCount() {
  counter && (counter.textContent = selectedFilters.querySelectorAll(".chip").length);
}

function addChipForCheckbox(cb) {
  // 이미 있으면 중복 생성 X
  if (selectedFilters.querySelector(`[data-id="${cb.value}"]`)) return;

  const chip = document.createElement("span");
  chip.className = "chip";
  chip.dataset.id = cb.value;
  chip.innerHTML = `
    ${cb.dataset.label}
    <button type="button" aria-label="Remove filter">×</button>
  `;
  chip.querySelector("button").addEventListener("click", () => {
    cb.checked = false;
    chip.remove();
    updateCount();
    // 칩 제거 즉시 반영하고 싶으면 주석 해제:
    // form.submit();
  });
  selectedFilters.appendChild(chip);
}

function rebuildChipsFromChecked() {
  selectedFilters.innerHTML = "";
  checkboxes.forEach(cb => cb.checked && addChipForCheckbox(cb));
  updateCount();
}

// 변경 시 칩 동기화
checkboxes.forEach(cb => {
  cb.addEventListener("change", () => {
    if (cb.checked) addChipForCheckbox(cb);
    else selectedFilters.querySelector(`[data-id="${cb.value}"]`)?.remove();
    updateCount();
  });
});

// 새로고침 후 복원!
window.addEventListener("DOMContentLoaded", rebuildChipsFromChecked);

// 초기화 버튼과도 동기화
document.querySelector("button[type='reset']")?.addEventListener("click", () => {
  selectedFilters.innerHTML = "";
  updateCount();
});