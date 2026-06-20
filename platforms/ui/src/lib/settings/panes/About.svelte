<script lang="ts">
  import { onMount } from "svelte";
  import Pane from "../Pane.svelte";
  import GlassCard from "../../components/GlassCard.svelte";
  import { getAppVersion } from "../../api";

  // Version comes from the app itself (Tauri reads it from tauri.conf.json, which CI
  // sets from the git tag) — single source of truth, no per-file drift.
  let version = $state("");
  onMount(async () => {
    version = await getAppVersion();
  });

  // Logo lives in platforms/ui/public/logo.png (served at /logo.png). Fall back to
  // the "FU" badge until the file is added.
  let hasLogo = $state(true);
</script>

<Pane title="Giới thiệu">
  <GlassCard>
    <div class="about">
      {#if hasLogo}
        <img class="logo" src="/logo.png" alt="Funput" onerror={() => (hasLogo = false)} />
      {:else}
        <div class="logo fallback">FU</div>
      {/if}
      <h2>Funput</h2>
      <p>Bộ gõ tiếng Việt — miễn phí, mã nguồn mở.</p>
      <p class="ver">Phiên bản {version}</p>
    </div>
  </GlassCard>
</Pane>

<style>
  .about {
    text-align: center;
    padding: var(--space-md) 0;
  }
  .logo {
    width: 72px;
    height: 72px;
    margin: 0 auto var(--space-md);
    border-radius: 16px;
    display: block;
    object-fit: contain;
  }
  .logo.fallback {
    background: var(--accent);
    color: var(--accent-contrast);
    font-weight: 800;
    font-size: 28px;
    display: grid;
    place-items: center;
  }
  h2 {
    margin: 0 0 var(--space-xs);
    font-size: 18px;
  }
  p {
    margin: 0;
    color: var(--text-secondary);
    font-size: 13px;
  }
  .ver {
    margin-top: var(--space-sm);
  }
</style>
