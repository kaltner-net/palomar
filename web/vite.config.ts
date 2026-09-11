import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";
import { loadPalomarBuildMetadata } from "./build-metadata";

const repositoryRoot = fileURLToPath(new URL("..", import.meta.url));
const buildMetadata = loadPalomarBuildMetadata(repositoryRoot);

export default defineConfig({
  plugins: [react()],
  define: {
    __PALOMAR_CLIENT_VERSION__: JSON.stringify(buildMetadata.version),
    __PALOMAR_CLIENT_COMMIT__: JSON.stringify(buildMetadata.commit),
    __PALOMAR_RELEASE_BUILD__: JSON.stringify(buildMetadata.releaseBuild),
  },
  server: {
    host: "0.0.0.0",
    port: 5173,
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test-setup.ts",
  },
});
