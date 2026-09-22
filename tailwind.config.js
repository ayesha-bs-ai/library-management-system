/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/main/resources/templates/**/*.html',
    './src/main/java/**/*.java'
  ],
  theme: {
    extend: {
      colors: {
        canvas: '#f8fafd',
        ink: '#1f1f1f',
        muted: '#5f6368',
        primary: {
          50: '#eef4ff',
          100: '#dbe8ff',
          200: '#b7d1ff',
          500: '#1a73e8',
          600: '#0b57d0',
          700: '#0842a0'
        },
        google: {
          blue: '#4285f4',
          red: '#ea4335',
          yellow: '#fbbc04',
          green: '#34a853'
        }
      },
      borderRadius: {
        '4xl': '2rem'
      },
      boxShadow: {
        'soft': '0 1px 2px rgba(60,64,67,.10), 0 2px 8px rgba(60,64,67,.08)',
        'lifted': '0 8px 28px rgba(60,64,67,.14)',
        'search': '0 1px 6px rgba(32,33,36,.20)'
      },
      fontFamily: {
        sans: ['Inter', 'Google Sans', 'Roboto', 'Arial', 'system-ui', 'sans-serif']
      }
    }
  },
  plugins: []
};
