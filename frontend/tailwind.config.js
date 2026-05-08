/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,ts}"],
  theme: {
    extend: {
      colors: {
        primary:   "#0d6efd",
        secondary: "#6c757d",
        success:   "#16a34a",
        danger:    "#dc2626",
        warning:   "#f59e0b"
      }
    }
  },
  plugins: []
};
