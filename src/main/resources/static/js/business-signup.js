/* ================= 공통 알림 ================= */
function showAlert(msg) {
  document.getElementById("alertMessage").innerText = msg;
  new bootstrap.Modal(document.getElementById("alertModal")).show();
}

/* ================= AJAX 중복 체크 ================= */
async function checkDuplicate(field, value) {
  if (!value.trim()) return null;
  const url = `/businessuser/check-${field}?${field}=${encodeURIComponent(value)}`;
  try {
    const res = await fetch(url, { headers: { Accept: "application/json" } });
    return await res.json(); // true | false | null
  } catch (e) {
    console.error(`${field} check 실패:`, e);
    return null;
  }
}

/* ================= 아이디 검증 ================= */
function attachUsernameCheck() {
  return attachCheck("username", "usernameError", "usernameSuccess", "username");
}
function attachCheck(inputId, errorId, successId, field) {
  const input = document.getElementById(inputId);
  const error = document.getElementById(errorId);
  const success = document.getElementById(successId);

  let timer = null;
  let lastResult = null;

  async function runCheck() {
    const available = await checkDuplicate(field, input.value);

    if (!input.value) {
      if (error) error.classList.add("hidden");
      if (success) success.classList.add("hidden");
      lastResult = null;
      return;
    }

    if (available === true) {
      if (success) success.classList.remove("hidden");
      if (error) error.classList.add("hidden");
      lastResult = true;
    } else if (available === false) {
      if (success) success.classList.add("hidden");
      if (error) {
        error.classList.remove("hidden");
        if (inputId === "username") error.innerText = "이미 사용중인 아이디입니다.";
      }
      lastResult = false;
    } else {
      if (success) success.classList.add("hidden");
      if (error) error.classList.add("hidden");
      lastResult = null;
    }
  }

  input.addEventListener("input", () => {
    if (error) error.classList.add("hidden");
    if (success) success.classList.add("hidden");
    clearTimeout(timer);
    timer = setTimeout(runCheck, 300);
  });
  input.addEventListener("blur", runCheck);

  return () => lastResult;
}

/* ================= 이메일 검증 ================= */
function attachEmailCheck() {
  const input = document.getElementById("email");
  const error = document.getElementById("emailError");
  const success = document.getElementById("emailSuccess");

  let timer = null;
  let lastResult = null;

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const allowedDomains = ["gmail.com", "naver.com", "daum.net", "kakao.com"];

  async function runCheck() {
    const email = (input.value || "").trim();

    if (!email) {
      error.classList.add("hidden");
      success.classList.add("hidden");
      lastResult = null;
      return;
    }

    if (!emailRegex.test(email)) {
      error.classList.remove("hidden");
      success.classList.add("hidden");
      error.innerText = "유효한 이메일 형식이 아닙니다.";
      lastResult = false;
      return;
    }

    const domain = email.split("@")[1];
    if (!allowedDomains.includes(domain)) {
      error.classList.remove("hidden");
      success.classList.add("hidden");
      error.innerText = "허용되지 않은 이메일 도메인입니다.";
      lastResult = false;
      return;
    }

    const available = await checkDuplicate("email", email);

    if (available === true) {
      success.classList.remove("hidden");
      error.classList.add("hidden");
      success.innerText = "사용 가능한 이메일입니다.";
      lastResult = true;
    } else if (available === false) {
      error.classList.remove("hidden");
      success.classList.add("hidden");
      error.innerText = "이미 등록된 이메일입니다.";
      lastResult = false;
    } else {
      error.classList.add("hidden");
      success.classList.add("hidden");
      lastResult = null;
    }
  }

  input.addEventListener("input", () => {
    error.classList.add("hidden");
    success.classList.add("hidden");
    clearTimeout(timer);
    timer = setTimeout(runCheck, 400);
  });
  input.addEventListener("blur", runCheck);

  return () => lastResult;
}

/* ================= 닉네임 검증 ================= */
function attachNicknameCheck() {
  const input = document.getElementById("nickname");
  const msg = document.getElementById("nicknameMessage");

  async function runCheck() {
    const available = await checkDuplicate("nickname", input.value);

    if (!input.value) {
      msg.classList.remove("text-success");
      msg.classList.add("text-danger");
      msg.innerText = "2~10자, 특수문자 제외";
    } else if (available === true) {
      msg.classList.remove("text-danger");
      msg.classList.add("text-success");
      msg.innerText = "사용 가능한 닉네임입니다.";
    } else if (available === false) {
      msg.classList.remove("text-success");
      msg.classList.add("text-danger");
      msg.innerText = "이미 사용중인 닉네임입니다.";
    } else {
      msg.classList.remove("text-success");
      msg.classList.add("text-danger");
      msg.innerText = "2~10자, 특수문자 제외";
    }
  }

  input.addEventListener("input", runCheck);
  input.addEventListener("blur", runCheck);
}

/* ================= 비밀번호 검증 ================= */
function validatePassword() {
  const pw = document.getElementById("password").value || "";
  const msg = document.getElementById("passwordError");
  const ok = /^(?=.*[!@#$%^&*(),.?":{}|<>]).{8,}$/.test(pw);

  if (ok) {
    msg.classList.remove("text-danger");
    msg.classList.add("text-success");
    msg.innerText = "사용 가능한 비밀번호입니다.";
    return true;
  } else {
    msg.classList.remove("text-success");
    msg.classList.add("text-danger");
    msg.innerText = "8자 이상, 특수문자 포함";
    return false;
  }
}
function validatePasswordConfirm() {
  const pw = document.getElementById("password").value || "";
  const pw2 = document.getElementById("passwordConfirm").value || "";
  const msg = document.getElementById("passwordConfirmError");

  if (!pw2) {
    msg.classList.add("hidden");
    return false;
  }
  if (pw !== pw2) {
    msg.classList.remove("hidden");
    msg.classList.remove("text-success");
    msg.classList.add("text-danger");
    msg.innerText = "비밀번호가 일치하지 않습니다.";
    return false;
  } else {
    msg.classList.remove("hidden");
    msg.classList.remove("text-danger");
    msg.classList.add("text-success");
    msg.innerText = "비밀번호가 일치합니다.";
    return true;
  }
}

/* ================= 주소 검색 팝업 ================= */
function openAddressPopup() {
  window.open("/jusoPopup", "주소검색", "width=500,height=600,scrollbars=yes");
}
function jusoCallBack(selectedAddress) {
  const addressInput = document.getElementById("address");
  const detailInput = document.getElementById("addressDetail");
  if (addressInput) {
    addressInput.value = selectedAddress.roadAddr || selectedAddress.jibunAddr || "";
    detailInput && detailInput.focus();
  }
}

/* 알림팝업 + 포커스 */
function alertAndFocus(msg, inputId) {
  showAlert(msg);
  const el = document.getElementById(inputId);
  if (el) setTimeout(() => { el.focus(); el.select && el.select(); }, 150);
}

/* ================== 메인 실행 ================== */
const checkUsername = attachUsernameCheck();
const checkEmail = attachEmailCheck();
attachNicknameCheck();

document.getElementById("password").addEventListener("input", validatePassword);
document.getElementById("passwordConfirm").addEventListener("input", validatePasswordConfirm);

document.addEventListener("DOMContentLoaded", () => {
  // 휴대폰 → hidden 병합
  const phone1 = document.getElementById("phone1");
  const phone2 = document.getElementById("phone2");
  const phone3 = document.getElementById("phone3");
  const phoneHidden = document.getElementById("phone");
  if (phone1 && phone2 && phone3 && phoneHidden) {
    const phoneInputs = [phone1, phone2, phone3];
    phoneInputs.forEach((input, idx) => {
      input.addEventListener("input", () => {
        input.value = input.value.replace(/\D/g, "");
        if (idx < 2 && input.value.length === parseInt(input.maxLength)) {
          phoneInputs[idx + 1].focus();
        }
        phoneHidden.value = `${phone1.value}-${phone2.value}-${phone3.value}`;
      });
      input.addEventListener("keydown", (e) => {
        if (e.key === "Backspace" && input.value.length === 0 && idx > 0) {
          phoneInputs[idx - 1].focus();
        }
      });
    });
  }
});

/* ===================================================================== */
/* =======================  사업자등록번호 (수정)  ======================= */
/* ===================================================================== */

/** ← 여기에 'Decoding' 일반 인증키를 넣으세요 (노출 주의) */
const ODCLOUD_SERVICE_KEY = "9b24c8e7cbbe2db17820a115f7df22ea01dbd7a8fdd21ecd6da344c17eab3673";

/** 숫자만 추출 */
function normalizeBizNo(v) {
  return (v || "").replace(/\D/g, "");
}

/** (서버) 사업자번호 중복 체크: true=미등록(사용 가능), false=이미 존재, null=판단불가 */
async function checkBizDuplicate(digits10) {
  if (!digits10) return null;
  return await checkDuplicate("businessNumber", digits10);
}

/** 입력 제약(10자리) + 자동 하이픈만 적용 — 실시간 메시지 없음 */
function attachBusinessNumberHandlers() {
  const input = document.getElementById("businessNumber");
  const okEl = document.getElementById("businessNumberSuccess");
  const errEl = document.getElementById("businessNumberError");
  if (!input) return;

  input.addEventListener("input", () => {
    // 10자리 제한 + 포맷만 적용
    const d = normalizeBizNo(input.value).slice(0, 10);
    if (d.length >= 6)      input.value = d.replace(/^(\d{3})(\d{2})(\d{0,5}).*$/, "$1-$2-$3");
    else if (d.length >= 4) input.value = d.replace(/^(\d{3})(\d{0,2}).*$/, "$1-$2");
    else                    input.value = d;

    // 실시간 메시지 비표시: 둘 다 숨김
    okEl && okEl.classList.add("hidden");
    errEl && errEl.classList.add("hidden");

    // 레거시 hidden(#corp_reg) 동기화
    const legacy = document.getElementById("corp_reg");
    if (legacy) legacy.value = normalizeBizNo(input.value);
  });

  // blur에서도 메시지는 띄우지 않음(일관 유지), 단 포맷 유지
  input.addEventListener("blur", () => {
    const d = normalizeBizNo(input.value).slice(0, 10);
    if (d.length >= 6)      input.value = d.replace(/^(\d{3})(\d{2})(\d{0,5}).*$/, "$1-$2-$3");
    else if (d.length >= 4) input.value = d.replace(/^(\d{3})(\d{0,2}).*$/, "$1-$2");
    else                    input.value = d;
  });
}
document.addEventListener("DOMContentLoaded", attachBusinessNumberHandlers);

/** odcloud(국세청) 실검증 */
async function checkBusinessNumber(bizNoDigits) {
  if (!bizNoDigits || bizNoDigits.length !== 10) {
    updateBizNumberUI({ ok: false, msg: "사업자등록번호는 숫자 10자리여야 합니다." });
    return false;
  }

  try {
    const res = await fetch(
      `https://api.odcloud.kr/api/nts-businessman/v1/status?serviceKey=${encodeURIComponent(ODCLOUD_SERVICE_KEY)}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json; charset=UTF-8",
          Accept: "application/json",
        },
        body: JSON.stringify({ b_no: [bizNoDigits] }),
      }
    );

    if (!res.ok) {
      const txt = await res.text();
      console.error("odcloud status:", res.status, txt);
      updateBizNumberUI({ ok: false, msg: "사업자 상태 조회 중 오류가 발생했습니다." });
      return false;
    }

    const result = await res.json();
    const matchCnt = typeof result.match_cnt === "string" ? parseInt(result.match_cnt, 10) : result.match_cnt;

    if (matchCnt === 1) {
      updateBizNumberUI({ ok: true, msg: "확인 완료" });
      return true;
    } else {
      const msg = result?.data?.[0]?.tax_type || "유효하지 않은 사업자번호입니다.";
      updateBizNumberUI({ ok: false, msg });
      return false;
    }
  } catch (e) {
    console.error("odcloud 예외:", e);
    updateBizNumberUI({ ok: false, msg: "네트워크 오류로 사업자번호 확인에 실패했습니다." });
    return false;
  }
}


function updateBizNumberUI({ ok, msg }) {
  const okEl  = document.getElementById("businessNumberSuccess");
  const errEl = document.getElementById("businessNumberError");


  if (errEl) errEl.classList.add("hidden");

  if (ok) {
    if (okEl) {
      okEl.classList.remove("hidden");
      okEl.textContent = msg || "확인 완료";
    }
  } else {
    if (okEl) okEl.classList.add("hidden");
    showAlert(msg || "사업자등록번호를 확인해주세요.");
  }
}


/** 버튼 onclick="corp_chk()" — 중복 통과 후 국세청 실검증 (실시간 메시지 X) */
async function corp_chk() {
  const el = document.getElementById("businessNumber");
  if (!el) { showAlert("사업자등록번호 입력창을 찾을 수 없습니다."); return false; }

  const digits = normalizeBizNo(el.value);
  if (digits.length !== 10) {
    updateBizNumberUI({ ok: false, msg: "사업자등록번호는 숫자 10자리여야 합니다." });
    return false;
  }

  const available = await checkBizDuplicate(digits);
  if (available === false) {
    updateBizNumberUI({ ok: false, msg: "이미 등록된 사업자번호입니다." });
    return false;
  }

  return await checkBusinessNumber(digits);
}

/** 폼 onsubmit 래핑: 기존 validateForm → 중복 → 국세청 검증 (실시간 메시지 X) */
(function wrapValidateFormForBiz() {
  const original = window.validateForm;
  if (typeof original !== "function") return;

  window.validateForm = async function (e) {
    // 1) 기존(아이디/이메일/비번 등)
    const base = await original(e);
    if (base === false) return false;

    // 2) 사업자번호 10자리 + 중복 체크
    const bnInput = document.getElementById("businessNumber");
    const digits = normalizeBizNo(bnInput ? bnInput.value : "");
    if (digits.length !== 10) {
      e && e.preventDefault();
      updateBizNumberUI({ ok: false, msg: "사업자등록번호는 숫자 10자리여야 합니다." });
      return false;
    }

    const available = await checkBizDuplicate(digits);
    if (available === false) {
      e && e.preventDefault();
      updateBizNumberUI({ ok: false, msg: "이미 등록된 사업자번호입니다." });
      return false;
    }

    // 3) 국세청 실검증
    const ok = await checkBusinessNumber(digits);
    if (!ok) { e && e.preventDefault(); return false; }

    return true;
  };
})();
