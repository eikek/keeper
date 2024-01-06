/** @type {import('tailwindcss').Config} */
export default {
    content: [ "./js/src/main/scala/**/*.scala", "./*.html", "./*.js" ],
    theme: {
        extend: {
            zIndex: {
                '1500': '1500'
            },
            height: {
                '128': '32rem'
            },
            screens: {
                '3xl': '1792px',
                '4xl': '2048px',
                '5xl': '2560px',
                '6xl': '3072px',
                '7xl': '3584px'
            }

        },
    },
    variants: {
        extend: {
            backgroundOpacity: ['dark']
        }
    },    
    plugins: [],
}

