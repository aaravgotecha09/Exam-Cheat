// Tiny service worker: on first install, open the Settings page so the student can check the backend URL.
importScripts('config.js');

chrome.runtime.onInstalled.addListener(async (details) => {
  const { apiBase } = await chrome.storage.sync.get('apiBase');
  if (!apiBase) await chrome.storage.sync.set({ apiBase: ECD_DEFAULT_API });
  if (details.reason === 'install') chrome.runtime.openOptionsPage();
});
