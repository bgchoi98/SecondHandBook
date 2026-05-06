const BookClub = (() => {

    let debounceTimer = null;

    function resolveImageUrl(url) {
        if (!url) return "";
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/")) {
            return url;
        }
        return "/img/" + url;
    }
    let currentSort = "latest"; // 기본 정렬: 최신순
    let currentPage = 0;        // 현재 페이지 (0부터 시작)
    let pageData = null;        // 현재 페이지 데이터

    /** 초기화 */
    function initList() {
        const keywordInput = document.getElementById("keyword");
        if (!keywordInput) return;

        keywordInput.addEventListener("input", () => {
            clearTimeout(debounceTimer);

            debounceTimer = setTimeout(() => {
                currentPage = 0; // 검색 시 첫 페이지로 리셋
                const keyword = keywordInput.value.trim();
                search(keyword, currentSort, currentPage);
            }, 300); // 입력 멈춘 후 300ms
        });

        // 정렬 버튼 초기화
        initSortButtons();
    }

    // 초기 전체 조회
    search("", "latest", 0);

    /** 서버 검색 요청 */
    function search(keyword, sort, page) {
        let url = `/bookclubs/search?sort=${sort || "latest"}&page=${page || 0}`;
        if (keyword) {
            url += `&keyword=${encodeURIComponent(keyword)}`;
        }

        fetch(url, {
            method: "GET",
            headers: {
                "Accept": "application/json"
            }
        })
            .then(res => res.json())
            .then(data => {
                pageData = data;
                currentPage = data.page;
                renderList(data);
                renderPagination(data);
            })
            .catch(err => {
                console.error("검색 실패", err);
            });
    }

    /** 결과 렌더링 */
    function renderList(data) {
        const grid = document.getElementById("bookclubGrid");
        grid.innerHTML = "";

        const list = data.content;

        // 모임 개수 업데이트 (전체 개수)
        const clubCountEl = document.getElementById("clubCount");
        if (clubCountEl) {
            clubCountEl.textContent = data.totalElements || 0;
            clubCountEl.classList.remove("invisible");
        }

        if (!list || list.length === 0) {
            grid.innerHTML = `
                <div class="empty-state">
                    <p>검색 결과가 없습니다.</p>
                </div>
            `;
            return;
        }

        list.forEach(club => {
            grid.insertAdjacentHTML("beforeend", `
                <article class="bookclub-card" data-club-seq="${club.book_club_seq}">
                    <!-- 지역 태그 - 왼쪽 상단 -->
                    <span class="card-region-tag">${club.book_club_rg ?? '지역 미정'}</span>
                    <!-- 찜 버튼 - 오른쪽 상단 -->
                    <button type="button" class="btn-wish ${club.wished ? 'wished' : ''}"
                        onclick="toggleWish(${club.book_club_seq}, this); event.preventDefault(); event.stopPropagation();"
                        data-club-seq="${club.book_club_seq}">
                        <svg class="wish-icon" width="18" height="18" viewBox="0 0 24 24" fill="${club.wished ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">
                            <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                        </svg>
                    </button>

                    <a href="/bookclubs/${club.book_club_seq}" class="card-link">
                        <div class="card-banner">
                            ${
                                club.banner_img_url
                                    ? `<img src="${resolveImageUrl(club.banner_img_url)}" alt="${club.book_club_name} 배너">`
                                    : `<div class="card-banner-placeholder"><span>📚</span></div>`
                            }
                        </div>
                        <div class="card-body">
                            <div class="card-body-inner">
                                <h3 class="card-title">${club.book_club_name}</h3>
                                ${club.book_club_desc ? `<p class="card-desc">${club.book_club_desc}</p>` : ''}
                                <div class="card-footer">
                                    <span class="card-schedule">${club.book_club_schedule ?? '일정 미정'}</span>
                                    <span class="card-members">${club.joined_member_count}/${club.book_club_max_member}</span>
                                </div>
                            </div>
                        </div>
                    </a>
                </article>
            `);
        });
    }

    /** 페이지네이션 렌더링 */
    function renderPagination(data) {
        let paginationContainer = document.getElementById("pagination");

        // 페이지네이션 컨테이너가 없으면 생성
        if (!paginationContainer) {
            const grid = document.getElementById("bookclubGrid");
            paginationContainer = document.createElement("div");
            paginationContainer.id = "pagination";
            paginationContainer.className = "pagination";
            grid.parentNode.insertBefore(paginationContainer, grid.nextSibling);
        }

        paginationContainer.innerHTML = "";

        // 페이지가 1개 이하면 페이지네이션 숨김
        if (data.totalPages <= 1) {
            paginationContainer.style.display = "none";
            return;
        }

        paginationContainer.style.display = "flex";

        const currentPage = data.page;
        const totalPages = data.totalPages;

        // 이전 버튼
        const prevBtn = document.createElement("button");
        prevBtn.className = "page-btn prev-btn";
        prevBtn.textContent = "이전";
        prevBtn.disabled = data.first;
        prevBtn.addEventListener("click", () => goToPage(currentPage - 1));
        paginationContainer.appendChild(prevBtn);

        // 페이지 번호 버튼 (최대 5개 표시)
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, startPage + 4);

        for (let i = startPage; i <= endPage; i++) {
            const pageBtn = document.createElement("button");
            pageBtn.className = "page-btn" + (i === currentPage ? " active" : "");
            pageBtn.textContent = i + 1;
            pageBtn.addEventListener("click", () => goToPage(i));
            paginationContainer.appendChild(pageBtn);
        }

        // 다음 버튼
        const nextBtn = document.createElement("button");
        nextBtn.className = "page-btn next-btn";
        nextBtn.textContent = "다음";
        nextBtn.disabled = data.last;
        nextBtn.addEventListener("click", () => goToPage(currentPage + 1));
        paginationContainer.appendChild(nextBtn);
    }

    /** 페이지 이동 */
    function goToPage(page) {
        const keywordInput = document.getElementById("keyword");
        const keyword = keywordInput ? keywordInput.value.trim() : "";
        search(keyword, currentSort, page);
        // 페이지 상단으로 스크롤
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    // 외부에서 호출 가능한 메서드
    function reload() {
        const keywordInput = document.getElementById("keyword");
        const keyword = keywordInput ? keywordInput.value.trim() : "";
        search(keyword, currentSort, currentPage);
    }

    function setSort(sort) {
        currentSort = sort;
        currentPage = 0;

        updateSortUI();

        const keywordInput = document.getElementById("keyword");
        const keyword = keywordInput ? keywordInput.value.trim() : "";
        search(keyword, currentSort, currentPage);
    }

    function updateSortUI() {
        const btns = document.querySelectorAll(".club-sort-btn");

        btns.forEach(btn => {
            btn.className =
                "club-sort-btn px-4 py-2 rounded-lg text-sm font-bold transition-all duration-200 " +
                "text-gray-500 hover:text-gray-900 hover:bg-gray-200/50";
        });

        const activeBtn = document.querySelector(
            `.club-sort-btn[data-sort="${currentSort}"]`
        );

        if (activeBtn) {
            activeBtn.className =
                "club-sort-btn px-4 py-2 rounded-lg text-sm font-bold transition-all duration-200 " +
                "bg-white text-gray-900 shadow-sm ring-1 ring-black/5";
        }
    }

    function initSortButtons() {
        const sortBtns = document.querySelectorAll(".club-sort-btn");

        sortBtns.forEach(btn => {
            btn.addEventListener("click", () => {
                const sort = btn.dataset.sort;
                setSort(sort);
            });
        });

        // 초기 활성화
        updateSortUI();
    }

    return {
        initList,
        reload
    };
})();

function initCreateModal() {
    const openBtn = document.getElementById("openCreateModal");
    const modal = document.getElementById("createBookClubModal");
    const closeBtn = document.getElementById("closeCreateModal");
    const overlay = modal?.querySelector(".modal-overlay");
    const form = document.getElementById("createBookClubForm");

    if (!modal || !form) return;

    // 모달 열기
    openBtn?.addEventListener("click", () => {
        modal.classList.remove("hidden");
    });

    // 모달 닫기
    closeBtn?.addEventListener("click", () => {
        modal.classList.add("hidden");
        resetForm();
    });

    overlay?.addEventListener("click", () => {
        modal.classList.add("hidden");
        resetForm();
    });

    // 이미지 업로드 기능
    const imageUploadArea = document.getElementById("imageUploadArea");
    const bannerImgInput = document.getElementById("bannerImgInput");

    imageUploadArea?.addEventListener("click", () => {
        bannerImgInput?.click();
    });

    bannerImgInput?.addEventListener("change", (e) => {
        const file = e.target.files[0];
        if (file) {
            // 이미지 파일인지 검증
            if (!file.type.startsWith('image/')) {
                alert('이미지 파일만 선택할 수 있습니다.');
                bannerImgInput.value = '';
                return;
            }
            const reader = new FileReader();
            reader.onload = (event) => {
                // 기존 미리보기 이미지 제거
                const existingImg = imageUploadArea.querySelector("img");
                if (existingImg) {
                    existingImg.remove();
                }
                // 아이콘, 텍스트 숨기기
                const icon = imageUploadArea.querySelector(".image-upload-icon");
                const text = imageUploadArea.querySelector(".image-upload-text");
                if (icon) icon.style.display = "none";
                if (text) text.style.display = "none";
                // 미리보기 이미지 추가 (input은 유지)
                const img = document.createElement("img");
                img.src = event.target.result;
                img.alt = "미리보기";
                imageUploadArea.appendChild(img);
                imageUploadArea.classList.add("has-image");
            };
            reader.readAsDataURL(file);
        }
    });

    // 오프라인/온라인 토글 버튼 - 카카오 지도 연동 방식으로 변경됨
    // 토글 로직은 initModalPlaceSearch()에서 처리함
    const bookClubType = document.getElementById("bookClubType");
    const bookClubRegion = document.getElementById("bookClubRegion");

    // 정기 일정 선택
    const cycleBtns = document.querySelectorAll(".cycle-btn");
    const weekSelect = document.getElementById("weekSelect");
    const daySelect = document.getElementById("daySelect");
    const timeSelect = document.getElementById("timeSelect");
    const dayBtns = document.querySelectorAll(".day-btn");
    const scheduleWeek = document.getElementById("scheduleWeek");
    const scheduleHour = document.getElementById("scheduleHour");
    const bookClubSchedule = document.getElementById("bookClubSchedule");

    let selectedCycle = "";
    let selectedDay = "";

    // 주기 선택
    cycleBtns.forEach(btn => {
        btn.addEventListener("click", () => {
            cycleBtns.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            selectedCycle = btn.dataset.value;

            // 초기화
            weekSelect.style.display = "none";
            daySelect.style.display = "none";
            scheduleWeek.value = "";
            selectedDay = "";
            dayBtns.forEach(b => b.classList.remove("active"));

            if (selectedCycle === "매주") {
                // 매주: 요일만 표시
                daySelect.style.display = "block";
            } else if (selectedCycle === "매월") {
                // 매월: 주차 + 요일 표시
                weekSelect.style.display = "block";
                daySelect.style.display = "block";
            }
            // 매일: 추가 선택 없음

            // 시간 선택 표시
            timeSelect.style.display = "block";
            updateScheduleValue();
        });
    });

    // 주차 선택
    scheduleWeek?.addEventListener("change", () => {
        updateScheduleValue();
    });

    // 요일 선택
    dayBtns.forEach(btn => {
        btn.addEventListener("click", () => {
            dayBtns.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            selectedDay = btn.dataset.value + "요일";
            updateScheduleValue();
        });
    });

    // 시간 선택
    scheduleHour?.addEventListener("change", () => {
        updateScheduleValue();
    });

    // 일정 값 조합
    function updateScheduleValue() {
        let schedule = "";
        if (selectedCycle) {
            schedule = selectedCycle;
            if (selectedCycle === "매월" && scheduleWeek?.value) {
                schedule += " " + scheduleWeek.value;
            }
            if ((selectedCycle === "매주" || selectedCycle === "매월") && selectedDay) {
                schedule += " " + selectedDay;
            }
            if (scheduleHour?.value) {
                schedule += " " + scheduleHour.value;
            }
        }
        bookClubSchedule.value = schedule;
    }

    // 폼 리셋 함수
    function resetForm() {
        form.reset();
        // 이미지 업로드 영역 초기화
        const existingImg = imageUploadArea.querySelector("img");
        if (existingImg) {
            existingImg.remove();
        }
        // 아이콘, 텍스트 다시 표시
        const icon = imageUploadArea.querySelector(".image-upload-icon");
        const text = imageUploadArea.querySelector(".image-upload-text");
        if (icon) icon.style.display = "";
        if (text) text.style.display = "";
        imageUploadArea.classList.remove("has-image");
        // 활동 지역 토글 버튼 초기화
        const offlineToggle = document.getElementById("offlineToggle");
        const onlineToggle = document.getElementById("onlineToggle");
        offlineToggle?.classList.add("active");
        onlineToggle?.classList.remove("active");
        if (bookClubType) bookClubType.value = "offline";
        if (bookClubRegion) bookClubRegion.value = "";
        // 장소 검색 영역 초기화
        const placeSearchContainer = document.getElementById("modalPlaceSearchContainer");
        const selectedPlaceDiv = document.getElementById("modalSelectedPlace");
        const placeSearchResults = document.getElementById("modalPlaceSearchResults");
        const placeSearchInput = document.getElementById("modalPlaceSearchInput");
        if (placeSearchContainer) placeSearchContainer.style.display = "block";
        if (selectedPlaceDiv) selectedPlaceDiv.style.display = "none";
        if (placeSearchResults) placeSearchResults.style.display = "none";
        if (placeSearchInput) placeSearchInput.value = "";
        // 정기 일정 초기화
        cycleBtns.forEach(b => b.classList.remove("active"));
        dayBtns.forEach(b => b.classList.remove("active"));
        weekSelect.style.display = "none";
        daySelect.style.display = "none";
        timeSelect.style.display = "none";
        selectedCycle = "";
        selectedDay = "";
        scheduleWeek.value = "";
        scheduleHour.value = "";
        bookClubSchedule.value = "";
    }

    // 폼 제출
    form.addEventListener("submit", e => {
        e.preventDefault();

        // [수정 1] CSRF 토큰과 헤더 이름 가져오기
        const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

        const formData = new FormData(form);

        // 온라인인 경우 지역을 "온라인"으로 설정
        if (bookClubType.value === "online") {
            formData.set("book_club_rg", "온라인");
        }

        console.log("=== submit form data ===");
        for (let [k, v] of formData.entries()) {
            console.log(k, v);
        }

        fetch("/bookclubs", {
            method: "POST",
            headers: {
                [csrfHeader]: csrfToken
            },
            body: formData
        })
        .then(async res => {
            if (!res.ok) {
                throw new Error("HTTP_ERROR_" + res.status);
            }
            const text = await res.text();
            return text ? JSON.parse(text) : {};
        })
        .then(data => {
            if (data.status === "fail") {
                if (data.message === "LOGIN_REQUIRED") {
                    alert("로그인이 필요합니다.");
                    return;
                }
                alert(data.message);
                return;
            }

            alert("모임이 생성되었습니다.");
            modal.classList.add("hidden");
            resetForm();
            BookClub.reload();
        })
        .catch(err => {
            console.error("create error", err);
            alert("모임 생성 중 오류가 발생했습니다.");
        });
    });
}
