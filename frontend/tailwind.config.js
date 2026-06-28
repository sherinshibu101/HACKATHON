export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        civic: {
          50: '#f0fffb',
          100: '#c9fff4',
          200: '#8dfbea',
          300: '#49ead7',
          400: '#18c9bb',
          500: '#0fa79d',
          600: '#0b827d',
          700: '#0d6461',
          800: '#104f4e',
          900: '#123f3f'
        }
      },
      boxShadow: {
        civic: '0 24px 80px rgba(0, 0, 0, 0.34)',
        glow: '0 0 55px rgba(20, 184, 166, 0.24)'
      },
      fontFamily: {
        display: ['"Aptos Display"', '"Segoe UI Variable Display"', '"Space Grotesk"', 'sans-serif'],
        sans: ['"Aptos"', '"Segoe UI Variable Text"', 'system-ui', 'sans-serif']
      }
    }
  },
  plugins: []
}
