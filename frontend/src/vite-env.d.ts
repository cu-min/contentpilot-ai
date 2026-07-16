/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_NOVNC_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
