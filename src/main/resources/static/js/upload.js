// DOM이 완전히 로드된 후 실행
document.addEventListener('DOMContentLoaded', function () {
    // ---- 공용 ----
    const MB = 1024 * 1024;
    const MAX_SIZE = 20 * MB; // 20MB
    const ALLOWED_EXT = ['ppt','pptx','pdf','hwp','hwpx','doc','docx'];

    // 숫자를 MB 문자열로
    function fmtMB(bytes) {
        if (!bytes || bytes <= 0) return '0 MB';
        return (bytes / MB).toFixed(1) + ' MB';
    }
    // 파일 확장자 추출
    function extOf(name) {
        const i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1).toLowerCase() : '';
    }

    // ---- 요소 참조 (id 대신 name 기반으로 안전하게 잡기) ----
    const fileInput = document.querySelector('input[type="file"][name="file"]');
    const uploadPlaceholder = document.getElementById('uploadPlaceholder');
    const uploadedFileDisplay = document.getElementById('uploadedFileDisplay');

    const uploadedFileNameEl = document.getElementById('uploadedFileName');
    const uploadedFileSizeEl = document.getElementById('uploadedFileSize');
    const replaceFileBtn = document.getElementById('replaceFileBtn');
    const removeFileBtn = document.getElementById('removeFileBtn');

    const fileError = document.getElementById('fileError');

    if (!fileInput || !uploadPlaceholder || !uploadedFileDisplay) {
        console.error('파일 업로드 필수 요소를 찾을 수 없습니다.');
        return;
    }

    // ---- UI 토글 ----
    function showUploadPlaceholder() {
        uploadPlaceholder.classList.remove('hidden');
        uploadedFileDisplay.classList.add('hidden');
        // 값 및 임시 URL 정리
        if (uploadedFileNameEl?.dataset.objectUrl) {
            URL.revokeObjectURL(uploadedFileNameEl.dataset.objectUrl);
            delete uploadedFileNameEl.dataset.objectUrl;
        }
        fileInput.value = '';
        if (fileError) {
            fileError.style.display = 'none';
            fileError.textContent = '';
        }
    }

    function showUploadedFile(file) {
        if (!file) {
            showUploadPlaceholder();
            return;
        }
        uploadPlaceholder.classList.add('hidden');
        uploadedFileDisplay.classList.remove('hidden');

        uploadedFileNameEl.textContent = file.name;
        uploadedFileSizeEl.textContent = fmtMB(file.size);

        // 기존 blob URL 정리
        if (uploadedFileNameEl.dataset.objectUrl) {
            URL.revokeObjectURL(uploadedFileNameEl.dataset.objectUrl);
        }
        const newUrl = URL.createObjectURL(file);
        uploadedFileNameEl.setAttribute('href', newUrl);
        uploadedFileNameEl.dataset.objectUrl = newUrl;
    }

    // ---- 검증 ----
    function validateFile(file) {
        if (!file) return { ok: false, msg: '파일이 선택되지 않았습니다.' };

        const ext = extOf(file.name);
        if (!ALLOWED_EXT.includes(ext)) {
            return { ok: false, msg: `허용되지 않는 형식입니다. (${ALLOWED_EXT.join(', ')})` };
        }
        if (file.size > MAX_SIZE) {
            return { ok: false, msg: `파일 용량이 큽니다. 최대 20MB까지 업로드 가능합니다. (현재 ${fmtMB(file.size)})` };
        }
        return { ok: true };
    }

    function setError(msg) {
        if (!fileError) return;
        fileError.textContent = msg;
        fileError.style.display = msg ? 'block' : 'none';
    }

    // ---- 이벤트 ----
    fileInput.addEventListener('change', function () {
        const file = fileInput.files && fileInput.files[0];
        setError('');
        if (file) {
            const { ok, msg } = validateFile(file);
            if (!ok) {
                setError(msg);
                // 잘못된 파일이면 값 초기화 + 플레이스홀더 복귀
                showUploadPlaceholder();
                return;
            }
            showUploadedFile(file);
        } else {
            showUploadPlaceholder();
        }
    });

    if (replaceFileBtn) {
        replaceFileBtn.addEventListener('click', function () {
            fileInput.click();
        });
    }

    if (removeFileBtn) {
        removeFileBtn.addEventListener('click', function () {
            showUploadPlaceholder();
        });
    }

    // 떠날 때 blob URL 해제
    window.addEventListener('beforeunload', function () {
        if (uploadedFileNameEl?.dataset.objectUrl) {
            URL.revokeObjectURL(uploadedFileNameEl.dataset.objectUrl);
        }
    });

    // 초기 상태
    showUploadPlaceholder();
});