// ================== account.js (휴대폰 + 비밀번호 모달 검증) ==================
(() => {
  "use strict";

  // --- 디버그 on/off ---
  const DEBUG = true;
  const log = (...args) => { if (DEBUG) console.log("[account.js]", ...args); };
  const err = (...args) => console.error("[account.js]", ...args);

  // --- 컨텍스트 경로(meta로 주입 권장) ---
  const ctxMeta = document.querySelector('meta[name="ctx"]');
  const CTX = (ctxMeta && ctxMeta.content) ? ctxMeta.content : ""; // 예: "" 또는 "/cafe"

  // --- CSRF (없으면 빈 값) ---
  const csrfMeta = document.querySelector('meta[name="_csrf"]');
  const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
  const csrfToken  = csrfMeta ? csrfMeta.content : "";
  const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.content : "X-CSRF-TOKEN";

  // ================== 휴대폰 번호 수정 ==================

  // --- 공통 요소 (휴대폰) ---
  const phoneModal    = document.getElementById("phoneModal");
  const openPhoneBtn  = document.getElementById("openPhoneModal");
  const closePhoneBtn = document.getElementById("closePhoneModal");
  const phoneDisplay  = document.getElementById("phoneDisplay");

  // 3-4-4 입력
  const p1 = document.getElementById("phone1");
  const p2 = document.getElementById("phone2");
  const p3 = document.getElementById("phone3");
  const hiddenPhone = document.getElementById("phone");
  const phoneInputsContainer = document.querySelector(".phone-inputs"); // (선택) 유도문 힌트 컨테이너

  // 저장(수정) 버튼
  const updatePhoneBtn = document.getElementById("updatePhoneBtn");

  // --- 초기 로그 ---
  log("readyState:", document.readyState);
  ["openPhoneModal","closePhoneModal","updatePhoneBtn","phoneModal","phoneDisplay","phone1","phone2","phone3","phone"]
    .forEach(id => log(id, !!document.getElementById(id)));
  log("CTX:", CTX || "(empty)");
  log("csrfHeader:", csrfHeader, "csrfToken exists:", !!csrfToken);

  // --- 유틸: 입력 초기화 & 유도문 표시 토글 ---
  function updateGuideState() {
    const hasAny = !!(p1?.value || p2?.value || p3?.value);
    phoneInputsContainer?.classList.toggle("has-value", hasAny); // .has-value일 때 힌트 숨기도록 CSS가 있다면 사용
  }
  function resetPhoneInputs() {
    if (p1) p1.value = "";
    if (p2) p2.value = "";
    if (p3) p3.value = "";
    if (hiddenPhone) hiddenPhone.value = "";
    updateGuideState();
  }

  // --- 모달 열기/닫기 (휴대폰) ---
  if (openPhoneBtn && phoneModal) {
    openPhoneBtn.addEventListener("click", () => {
      resetPhoneInputs();                 // 열 때 항상 비우기 → 유도문 보이도록
      phoneModal.classList.add("show");
      setTimeout(() => p1?.focus(), 0);   // UX: 첫 칸 포커스
      log("openPhoneModal clicked → modal.show");
    });
  }
  if (closePhoneBtn && phoneModal) {
    closePhoneBtn.addEventListener("click", () => {
      resetPhoneInputs();                 // 닫을 때도 비우기(선택)
      phoneModal.classList.remove("show");
      log("closePhoneModal clicked → modal.hide");
    });
  }

  // --- 3-4-4 입력 처리 ---
  if (p1 && p2 && p3 && hiddenPhone) {
    const inputs = [p1, p2, p3];
    inputs.forEach((input, idx) => {
      input.addEventListener("input", () => {
        input.value = input.value.replace(/\D/g, "");
        const max = parseInt(input.maxLength || "4", 10);
        if (idx < 2 && input.value.length === max) inputs[idx + 1].focus();
        hiddenPhone.value = `${p1.value}-${p2.value}-${p3.value}`;
        updateGuideState();               // 값 생기면 힌트 숨김
        log("compose phone:", hiddenPhone.value);
      });
      input.addEventListener("keydown", (e) => {
        if (e.key === "Backspace" && input.value.length === 0 && idx > 0) {
          inputs[idx - 1].focus();
        }
      });
    });
  } else {
    log("phone inputs not ready → p1/p2/p3/hidden missing?");
  }

  // --- 저장 클릭 → 서버 전송 (휴대폰) ---
  if (updatePhoneBtn && hiddenPhone) {
    updatePhoneBtn.addEventListener("click", async () => {
      const newPhone = (hiddenPhone.value || "").trim(); // 예: 010-1234-5678
      log("updatePhoneBtn clicked. newPhone:", newPhone);

      if (!/^\d{2,3}-\d{3,4}-\d{4}$/.test(newPhone)) {
        alert("휴대폰 번호 형식을 확인해주세요. (예: 010-1234-5678)");
        return;
      }

      const url = `${CTX}/mypage/account/update-phone`;
      log("fetch url:", url);

      try {
        const res = await fetch(url, {
          method: "POST",
          credentials: "same-origin",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            [csrfHeader]: csrfToken
          },
          body: `phone=${encodeURIComponent(newPhone)}`
        });

        const text = await res.text();
        const ctype = (res.headers.get("content-type") || "").toLowerCase();
        const body = (text || "").trim().toLowerCase();

        log("[update-phone] status:", res.status,
            "redirected:", res.redirected,
            "finalURL:", res.url,
            "contentType:", ctype,
            "body(raw):", text);

        if (res.ok && body === "success") {
          alert("휴대폰 번호가 변경되었습니다.");
          if (phoneDisplay) phoneDisplay.textContent = newPhone;
          resetPhoneInputs();             // 성공 후 비우기
          phoneModal?.classList.remove("show");
          return;
        }

        if (res.status === 401) {
          alert("로그인이 필요합니다. 다시 로그인 후 시도해주세요.");
          location.href = `${CTX}/user/login?continue=` + encodeURIComponent(location.pathname + location.search);
          return;
        }
        if (res.status === 403 || res.redirected || ctype.includes("text/html")) {
          if (confirm("변경에 실패했습니다. 보안 인증을 다시 진행할까요?")) {
            const cont = location.pathname + location.search;
            location.href = `${CTX}/mypage/verify_password?continue=` + encodeURIComponent(cont);
          }
          return;
        }

        alert("변경에 실패했습니다: " + text);
      } catch (e) {
        err("fetch error:", e);
        alert("서버 오류가 발생했습니다.");
      }
    });
  } else {
    log("updatePhoneBtn or hiddenPhone missing → click handler not bound");
  }

// ================== 비밀번호 변경 (모달 + 단순 가이드/통과만 표시) ==================

// 요소
const pwModal     = document.getElementById("passwordModal");
const openPwBtn   = document.getElementById("openPasswordModal");
const closePwBtn  = document.getElementById("closePasswordModal");
const newPwInput  = document.getElementById("newPassword");
const newPw2Input = document.getElementById("newPasswordConfirm");
const updatePwBtn = document.getElementById("updatePasswordBtn");
const pwMsg       = document.getElementById("passwordMessage"); // 메시지 한 줄 영역
const pwForm      = document.getElementById("passwordForm");    // form 있으면 기본 제출 막기

// ✅ 기본 가이드(항상 보이게) — 조건 충족 전까지는 이 문구를 '빨간색'으로 유지
const BASE_MSG = "8자 이상, 특수문자 포함";

// 메시지 헬퍼 (빨강/초록 전환)
function setPwMsg(text, ok = false) {
  if (!pwMsg) return;
  pwMsg.textContent = text;

  // 커스텀 클래스
  pwMsg.classList.toggle("ok", !!ok);
  pwMsg.classList.toggle("err", !ok);

  // 부트스트랩 호환(있으면)
  pwMsg.classList.toggle("text-success", !!ok);
  pwMsg.classList.toggle("text-danger", !ok);
}

// ✅ 최종 규칙: 길이 8~64 + 특수문자 1개 이상(반드시)
function isValidPw(pw) {
  if (!pw) return false;
  if (pw.length < 8 || pw.length > 64) return false;
  // 영문/숫자가 아닌 문자(= 특수문자) 최소 1개
  return /[^a-zA-Z0-9]/.test(pw);
}

function resetPasswordInputs() {
  if (newPwInput) newPwInput.value = "";
  if (newPw2Input) newPw2Input.value = "";
  // 모달 열릴 때는 기본 가이드를 '빨간색'으로 표시
  setPwMsg(BASE_MSG, false);
}

// 모달 열고 닫기
if (openPwBtn && pwModal) {
  openPwBtn.addEventListener("click", () => {
    resetPasswordInputs();
    pwModal.classList.add("show");
    setTimeout(() => newPwInput?.focus(), 0);
  });
}
if (closePwBtn && pwModal) {
  closePwBtn.addEventListener("click", () => {
    resetPasswordInputs();
    pwModal.classList.remove("show");
  });
}

// form 기본 submit 방지
if (pwForm) pwForm.addEventListener("submit", (e) => e.preventDefault());

// 입력 이벤트(새 비밀번호)
// - 조건을 만족하기 전: 기본 가이드(빨강)만 보임
// - 조건을 만족하면: "사용 가능한 비밀번호입니다."(초록)만 보임
if (newPwInput) {
  setPwMsg(BASE_MSG, false); // 초기 진입 시 가이드 출력
  newPwInput.addEventListener("input", () => {
    const v = newPwInput.value || "";
    if (isValidPw(v)) {
      setPwMsg("사용 가능한 비밀번호입니다.", true);
    } else {
      setPwMsg(BASE_MSG, false);
    }

    if (newPw2Input) {
      newPw2Input.addEventListener("focus", () => {
        const v1 = newPwInput?.value || "";
        const v2 = newPw2Input.value || "";
        if (isValidPw(v1) && !v2) {
          // 형식은 맞지만 확인란이 아직 비었으면 가이드 문구(빨강)로 리셋
          setPwMsg("새 비밀번호 확인이 일치하지 않습니다.", false);
        }
      });
    }


    // 확인란이 이미 채워져 있으면 불일치 경고(형식이 유효할 때만 체크)
    if (newPw2Input && newPw2Input.value) {
      if (!isValidPw(v)) {
        setPwMsg(BASE_MSG, false);
      } else if (newPw2Input.value !== v) {
        setPwMsg("새 비밀번호 확인이 일치하지 않습니다.", false);
      } else {
        setPwMsg("사용 가능한 비밀번호입니다.", true);
      }
    }
  });
}

// 확인 입력 이벤트
if (newPw2Input) {
  newPw2Input.addEventListener("input", () => {
    const v1 = newPwInput?.value || "";
    const v2 = newPw2Input.value || "";

    if (!isValidPw(v1)) {
      // 원 비밀번호가 유효하지 않으면 기본 가이드만
      setPwMsg(BASE_MSG, false);
      return;
    }
    if (!v2 || v1 !== v2) {
      setPwMsg("새 비밀번호 확인이 일치하지 않습니다.", false);
    } else {
      setPwMsg("사용 가능한 비밀번호입니다.", true);
    }
  });
}

// 저장 클릭 → 서버 전송 (필수: 특수문자 포함 규칙 준수)
if (updatePwBtn) {
  updatePwBtn.addEventListener("click", async () => {
    const newPw  = (newPwInput?.value  || "").trim();
    const newPw2 = (newPw2Input?.value || "").trim();

    // 1) 형식(특수문자 포함) 통과 필수
    if (!isValidPw(newPw)) {
      setPwMsg(BASE_MSG, false);
      newPwInput?.focus();
      return;
    }
    // 2) 확인 일치 필수
    if (newPw !== newPw2) {
      setPwMsg("새 비밀번호 확인이 일치하지 않습니다.", false);
      newPw2Input?.focus();
      return;
    }

    const url = `${CTX}/mypage/account/update-password`;
    updatePwBtn.disabled = true;

    try {
      const res = await fetch(url, {
        method: "POST",
        credentials: "same-origin",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          [csrfHeader]: csrfToken
        },
        body: `newPassword=${encodeURIComponent(newPw)}&newPasswordConfirm=${encodeURIComponent(newPw2)}`
      });

      const text  = await res.text();
      const ctype = (res.headers.get("content-type") || "").toLowerCase();
      const bodyL = (text || "").trim().toLowerCase();

      if (res.ok && (bodyL === "success" || bodyL === "ok")) {
        alert("비밀번호가 변경되었습니다. 다시 로그인해 주세요.");
        resetPasswordInputs();
        pwModal?.classList.remove("show");
        location.href = `${CTX}/user/login?continue=` + encodeURIComponent(location.pathname + location.search);
        return;
      }

      if (res.status === 401) {
        alert("로그인이 필요합니다. 다시 로그인 후 시도해주세요.");
        location.href = `${CTX}/user/login?continue=` + encodeURIComponent(location.pathname + location.search);
        return;
      }

      if (res.status === 403 || res.redirected || ctype.includes("text/html")) {
        if (confirm("변경에 실패했습니다. 보안 인증을 다시 진행할까요?")) {
          const cont = location.pathname + location.search;
          location.href = `${CTX}/mypage/verify_password?continue=` + encodeURIComponent(cont);
        }
        return;
      }

      alert("변경에 실패했습니다: " + text);
    } catch (e) {
      console.error("[account.js] fetch error (password):", e);
      alert("서버 오류가 발생했습니다.");
    } finally {
      updatePwBtn.disabled = false;
    }
  });
} else {
  console.log("[account.js] updatePasswordBtn missing → password click handler not bound");
}

})();
