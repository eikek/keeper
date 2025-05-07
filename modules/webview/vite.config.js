import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
    plugins: [scalaJSPlugin({
        cwd: '../../',
        projectID: 'webviewJS'
    }), tailwindcss()],
});
