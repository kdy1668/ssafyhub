/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{vue,js}"],
  theme: {
    extend: {
      colors: {
        background: "#F2F3F3",
        surface: "#FFFFFF",
        border: "#D5DBDB",
        primary: {
          DEFAULT: "#0972D3",
          hover: "#075A9E",
          light: "#EAF3FB",
        },
        accent: {
          DEFAULT: "#EC7211",
          light: "#FCEEE2",
        },
        success: {
          DEFAULT: "#1D8102",
          light: "#E9F5E4",
        },
        danger: {
          DEFAULT: "#D91515",
          light: "#FDEBEC",
        },
        ink: {
          900: "#16191F",
          700: "#414D5C",
          500: "#5F6B7A",
          300: "#879596",
        },
      },
      borderRadius: {
        sm: "4px",
        DEFAULT: "6px",
        md: "6px",
        lg: "8px",
      },
      boxShadow: {
        card: "0 1px 2px rgba(0, 28, 36, 0.08)",
        popover: "0 2px 8px rgba(0, 28, 36, 0.16)",
      },
      fontFamily: {
        sans: [
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
