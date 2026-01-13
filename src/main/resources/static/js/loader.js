/**
 * 로딩 화면 표시/숨김 함수
 * 사용법:
 * - showLoading(): 로딩 화면 표시
 * - hideLoading(): 로딩 화면 숨김
 */

// 로딩 화면 HTML + CSS
const loaderHTML = `
<style>
    #awesome-loader-container {
        position: fixed;
        inset: 0;
        background-color: rgba(0, 0, 0, 0.7);
        backdrop-filter: blur(4px);
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
        z-index: 9999;
    }

    #awesome-loader-wrapper {
        position: relative;
        width: 200px;
        height: 200px;
    }

    /* 회전하는 외곽 링 */
    .awesome-loader-outer-ring {
        position: absolute;
        width: 200px;
        height: 200px;
        left: 50%;
        top: 50%;
        margin-left: -100px;
        margin-top: -100px;
        border-radius: 50%;
        border: 4px solid transparent;
        border-top-color: #3b82f6;
        border-right-color: #2563eb;
        animation: awesome-loader-rotate-clockwise 3s linear infinite;
    }

    /* 반대로 회전하는 내부 링 */
    .awesome-loader-inner-ring {
        position: absolute;
        width: 160px;
        height: 160px;
        left: 50%;
        top: 50%;
        margin-left: -80px;
        margin-top: -80px;
        border-radius: 50%;
        border: 4px solid transparent;
        border-bottom-color: #1f2937;
        border-left-color: #111827;
        animation: awesome-loader-rotate-counterclockwise 2s linear infinite;
    }

    /* 중앙의 펄스 효과 */
    .awesome-loader-pulse-circle {
        position: absolute;
        width: 80px;
        height: 80px;
        left: 50%;
        top: 50%;
        margin-left: -40px;
        margin-top: -40px;
        border-radius: 50%;
        background: linear-gradient(to right, #3b82f6, #1f2937);
        animation: awesome-loader-pulse 2s ease-in-out infinite;
    }

    /* 중심 빛나는 점 */
    .awesome-loader-center-dot {
        position: absolute;
        width: 40px;
        height: 40px;
        left: 50%;
        top: 50%;
        margin-left: -20px;
        margin-top: -20px;
        border-radius: 50%;
        background-color: #2563eb;
        box-shadow: 0 0 40px rgba(59, 130, 246, 0.8), 0 0 80px rgba(59, 130, 246, 0.5);
        animation: awesome-loader-glow 1.5s ease-in-out infinite;
    }

    /* 애니메이션 정의 */
    @keyframes awesome-loader-rotate-clockwise {
        from {
            transform: rotate(0deg);
        }
        to {
            transform: rotate(360deg);
        }
    }

    @keyframes awesome-loader-rotate-counterclockwise {
        from {
            transform: rotate(0deg);
        }
        to {
            transform: rotate(-360deg);
        }
    }

    @keyframes awesome-loader-pulse {
        0%, 100% {
            transform: scale(1);
            opacity: 0.5;
        }
        50% {
            transform: scale(1.2);
            opacity: 0.8;
        }
    }

    @keyframes awesome-loader-glow {
        0%, 100% {
            transform: scale(1);
        }
        50% {
            transform: scale(1.3);
        }
    }
</style>

<div id="awesome-loader-container">
    <div id="awesome-loader-wrapper">
        <div class="awesome-loader-outer-ring"></div>
        <div class="awesome-loader-inner-ring"></div>
        <div class="awesome-loader-pulse-circle"></div>
        <div class="awesome-loader-center-dot"></div>
    </div>
</div>
`;

/**
 * 로딩 화면 표시
 */
function showLoading() {
    // 이미 로딩 화면이 있으면 중복 생성 방지
    if (document.getElementById('awesome-loader-container')) {
        return;
    }

    // body에 로딩 화면 추가
    const loaderDiv = document.createElement('div');
    loaderDiv.innerHTML = loaderHTML;
    document.body.appendChild(loaderDiv);
}

/**
 * 로딩 화면 숨김
 */
function hideLoading() {
    const loader = document.getElementById('awesome-loader-container');
    if (loader) {
        // 부모 div도 함께 제거 (style 태그 포함)
        loader.parentElement.remove();
    }
}