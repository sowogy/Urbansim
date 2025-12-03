/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    // 아래 경로가 가장 중요합니다. templates 폴더 안의 모든 html 파일을 감시하라는 의미입니다.
    './src/main/resources/templates/**/*.html',
    './src/main/resources/templates/*.html',
    "./src/main/resources/static/js/*.js"
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}

