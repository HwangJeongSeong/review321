/* ================= 비밀번호 검증 ================= */
console.log("✅ modify-password.js 로드됨");

function validatePassword() {
	const pw = document.getElementById("password").value.trim();
	const msg = document.getElementById("passwordError");
	const regex = /^(?=.*[!@#$%^&*(),.?":{}|<>]).{8,}$/;

	if (!pw || !regex.test(pw)) {
		msg.classList.remove("text-success");
		msg.classList.add("text-danger");
		msg.innerText =
			"8자 이상, 특수문자 1개 이상 포함/^(?=.*[!@#$%^&*(),.?:{}|]).{8,}$/;><";
		msg.classList.remove("d-none");
		return false;
	} else {
		msg.classList.remove("text-danger", "d-none");
		msg.classList.add("text-success");
		msg.innerText = "사용 가능한 비밀번호입니다.";
		return true;
	}

	//    if (!regex.test(pw)) {
	//        msg.classList.remove("text-success");
	//        msg.classList.add("text-danger");
	//        msg.innerText = "조건을 만족하지 않는 비밀번호입니다.";
	//        msg.classList.remove("d-none");
	//        return false;
	//    } else {
	//        msg.classList.remove("text-danger");
	//        msg.classList.add("text-success");
	//        msg.innerText = "사용 가능한 비밀번호입니다.";
	//        msg.classList.remove("d-none");
	//        return true;
	//    }
}

/* ================= 비밀번호 확인 검증 ================= */
function validatePasswordConfirm() {
	const pw = document.getElementById("password").value.trim();
	const pw2 = document.getElementById("passwordConfirm").value.trim();
	const msg = document.getElementById("passwordConfirmError");

	console.log("pw:", pw, "pw2:", pw2); // 실제 값 확인

	if (pw !== pw2) {
		msg.innerText = "비밀번호가 일치하지 않습니다.";
		msg.classList.remove("text-success");
		msg.classList.remove("d-none");
		msg.classList.add("text-danger");
		return false;
	} else {
		msg.innerText = "비밀번호가 일치합니다.";
		msg.classList.remove("text-danger");
		msg.classList.remove("d-none");
		msg.classList.add("text-success");
		return true;
	}
}

/* ================= 폼 제출 검증 ================= */
function validateForm(e) {
	e.preventDefault(); // 먼저 기본 제출 막기

	const username = document.getElementById("username").value.trim();
	const verifiedId = document.getElementById("verifiedUserId")?.value; // 인증 완료 아이디

	// 비밀번호 검증
	const pwValid = validatePassword();
	const pwConfirmValid = validatePasswordConfirm();

	if (!pwValid || !pwConfirmValid) {
		e.preventDefault(); // 반드시 막기
		showAlert("비밀번호를 확인해주세요.");
		return;
	}

	// 검증 통과하면 폼 제출
	e.target.submit();
}

/* ================= 알림 모달 표시 ================= */
function showAlert(message) {
	const modalEl = document.getElementById("alertModal");
	const msgEl = document.getElementById("alertMessage");
	msgEl.innerText = message;

	const modal = new bootstrap.Modal(modalEl);

	// 모달 닫힐 때 페이지 새로고침
	// 이벤트 제거 후 다시 등록 (중복 방지)
	modalEl.removeEventListener("hidden.bs.modal", reloadPage);
	function reloadPage() {
		window.location.reload();
	}
	modalEl.addEventListener("hidden.bs.modal", reloadPage, { once: true });

	modal.show();
}

/* ================= 이벤트 등록 ================= */
document.addEventListener("DOMContentLoaded", function () {
	const toggleWrappers = document.querySelectorAll(".input-with-toggle");
	const password = document.getElementById("password");
	const passwordConfirm = document.getElementById("passwordConfirm");
	const form = document.getElementById("resetPasswordForm");

	if (password) password.addEventListener("keyup", validatePassword);
	if (passwordConfirm)
		passwordConfirm.addEventListener("keyup", validatePasswordConfirm);
	if (form) form.addEventListener("submit", validateForm);

	toggleWrappers.forEach((wrapper) => {
		const input = wrapper.querySelector("input");
		const toggleBtn = wrapper.querySelector(".toggle-btn");

		// 입력 시 버튼 표시/숨김
		input.addEventListener("input", () => {
			if (input.value.length > 0) {
				toggleBtn.style.display = "block";
			} else {
				toggleBtn.style.display = "none";
				// 입력값이 없어지면 항상 비밀번호 타입으로 되돌림
				input.type = "password";
				toggleBtn.classList.remove("bi-eye-slash");
				toggleBtn.classList.add("bi-eye");
			}
		});

		// 버튼 클릭 시 보이기/숨기기 토글
		toggleBtn.addEventListener("click", () => {
			if (input.type === "password") {
				input.type = "text";
				toggleBtn.classList.remove("bi-eye");
				toggleBtn.classList.add("bi-eye-slash");
			} else {
				input.type = "password";
				toggleBtn.classList.remove("bi-eye-slash");
				toggleBtn.classList.add("bi-eye");
			}
		});
	});

	// 비밀번호 변경 성공 모달
	function showSuccessModal(message) {
		// 메시지 세팅
		document.getElementById("비밀번호가 변경되었습니다.").textContent = message;

		// 모달 띄우기
		var modal = new bootstrap.Modal(document.getElementById("alertModal"));
		modal.show();
	}
});
