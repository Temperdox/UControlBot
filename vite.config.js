import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

export default defineConfig({
    plugins: [
        react({
            jsxRuntime: 'automatic',
            include: ['**/*.jsx', '**/*.js']
        })
    ],
    root: '.',
    build: {
        outDir: 'target/classes/public',
        emptyOutDir: true,
    },
    esbuild: {
        loader: 'jsx',
        include: /src\/.*\.jsx?$/,
        exclude: []
    },
    optimizeDeps: {
        esbuildOptions: {
            loader: {
                '.js': 'jsx'
            }
        }
    },
    resolve: {
        alias: {
            'webapp': resolve(__dirname, 'src/main/resources/webapp')
        },
        extensions: ['.js', '.jsx']
    }
});