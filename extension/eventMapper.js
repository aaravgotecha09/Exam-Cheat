/*
 * Pure event-mapping + de-duplication logic (no browser APIs, so it can be unit tested with Node).
 * Rules:
 *  - TAB_SWITCH   : once per "page hidden" period (visibilitychange -> hidden). Reset when visible again.
 *  - WINDOW_EXIT  : once per focus loss (window blur while the page is still visible). Reset on focus.
 *                   Ignored when the blur is just the side effect of a tab switch (within TAB_BLUR_GRACE_MS).
 *  - COPY_PASTE   : each copy / cut / paste, but identical actions within CLIPBOARD_DEDUPE_MS count once
 *                   (a single Ctrl+C can fire more than one DOM event).
 * Limits: Chrome gives no reliable signal for activity in OTHER applications; these are indicators only.
 */
(function (root) {
  const TAB_BLUR_GRACE_MS = 1500;
  const CLIPBOARD_DEDUPE_MS = 400;

  function createMapper() {
    let hiddenReported = false;
    let blurReported = false;
    let lastHiddenAt = -Infinity;
    let lastClipboardAt = -Infinity;

    return {
      visibility(hidden, now) {
        if (hidden) {
          if (hiddenReported) return [];
          hiddenReported = true;
          lastHiddenAt = now;
          return [{ type: 'TAB_SWITCH', details: 'Page became hidden' }];
        }
        hiddenReported = false;
        return [];
      },
      /** Call ~300 ms after a window blur, passing document.hidden at that moment. */
      blur(documentHidden, now) {
        if (documentHidden) return [];
        if (now - lastHiddenAt < TAB_BLUR_GRACE_MS) return [];
        if (blurReported) return [];
        blurReported = true;
        return [{ type: 'WINDOW_EXIT', details: 'Exam window lost focus' }];
      },
      focus() {
        blurReported = false;
        return [];
      },
      clipboard(kind, now) {
        if (now - lastClipboardAt < CLIPBOARD_DEDUPE_MS) return [];
        lastClipboardAt = now;
        return [{ type: 'COPY_PASTE', details: 'Clipboard action: ' + kind }];
      },
    };
  }

  /** Unique id so a retried request can never be scored twice by the backend. */
  function makeEventId(type, rand) {
    const r = (rand || Math.random)().toString(36).slice(2, 10);
    return 'ecd-' + type.toLowerCase().replace(/_/g, '') + '-' + Date.now().toString(36) + '-' + r;
  }

  const api = { createMapper, makeEventId, TAB_BLUR_GRACE_MS, CLIPBOARD_DEDUPE_MS };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.EcdEventMapper = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
