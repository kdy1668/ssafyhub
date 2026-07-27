/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{vue,js}"],
  theme: {
    extend: {
      colors: {
        background: "#F6F8FB",
        surface: "#FFFFFF",
        border: "#E6EAF0",
        // 삼성 딥 블루 계열 (브랜드 메인)
        primary: {
          DEFAULT: "#1428A0",
          hover: "#101F86",
          light: "#EAF0FF",
          soft: "#D6E2FF",
        },
        // 스카이/애저 블루 (강조·그라디언트)
        sky: {
          DEFAULT: "#1F8CE6",
          hover: "#1877C6",
          light: "#E7F3FE",
        },
        // 경고용 앰버
        accent: {
          DEFAULT: "#C77700",
          light: "#FBF1E1",
        },
        success: {
          DEFAULT: "#137333",
          light: "#E6F4EA",
        },
        danger: {
          DEFAULT: "#C5221F",
          light: "#FCE8E6",
        },
        ink: {
          900: "#0F172A",
          700: "#334155",
          500: "#64748B",
          300: "#94A3B8",
        },
      },
      borderRadius: {
        sm: "6px",
        DEFAULT: "8px",
        md: "10px",
        lg: "14px",
        xl: "20px",
      },
      boxShadow: {
        card: "0 1px 2px rgba(16, 24, 40, 0.04), 0 1px 3px rgba(16, 24, 40, 0.06)",
        cardHover: "0 6px 20px rgba(20, 40, 160, 0.12)",
        popover: "0 8px 24px rgba(16, 24, 40, 0.12)",
      },
      fontFamily: {
        sans: [
          "Pretendard",
          "Pretendard Variable",
          "-apple-system",
          "BlinkMacSystemFont",
          "Segoe UI",
          "Roboto",
          "Helvetica Neue",
          "Arial",
          "sans-serif",
        ],
      },
    },
  },
  plugins: [],
};
