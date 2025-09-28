window.addEventListener("DOMContentLoaded", () => {
	const alertBox = document.getElementById("alertBox");
	if (alertBox) {
		setTimeout(() => {
			alertBox.style.display = "none";
		}, 5000);
	}
});

// 모든 input-with-clear 처리
document.querySelectorAll(".input-with-clear").forEach((wrapper) => {
	const input = wrapper.querySelector("input");
	const clearBtn = wrapper.querySelector(".clear-btn");

	// 버튼 클릭 시 값 초기화
	clearBtn.addEventListener("click", () => {
		input.value = "";
		input.focus();
		clearBtn.style.display = "none";
	});

	// 입력 시 버튼 표시/숨김
	input.addEventListener("input", () => {
		if (input.value.length > 0) {
			clearBtn.style.display = "block";
		} else {
			clearBtn.style.display = "none";
		}
	});
});
