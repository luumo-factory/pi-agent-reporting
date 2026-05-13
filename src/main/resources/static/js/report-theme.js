(() => {
  const MESSAGE_TYPE = 'reports-theme';
  const THEME_DATA_KEY = 'theme';
  let manualOverride = false;

  const init = () => {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)');

    const applyTheme = (nextTheme) => {
      if (!nextTheme) return;
      document.documentElement.dataset.theme = nextTheme;
      if (document.body) {
        document.body.dataset.theme = nextTheme;
      }
    };

    const readParentTheme = () => {
      try {
        if (window.parent && window.parent !== window) {
          const parentDoc = window.parent.document;
          return (
            parentDoc.body?.dataset?.[THEME_DATA_KEY] ||
            parentDoc.documentElement?.dataset?.[THEME_DATA_KEY]
          );
        }
      } catch (error) {
        // Accessing parent document can fail when served from different origins.
      }
      return null;
    };

    const initialTheme =
      document.documentElement.dataset[THEME_DATA_KEY] ||
      document.body?.dataset?.[THEME_DATA_KEY] ||
      readParentTheme() ||
      (prefersDark.matches ? 'dark' : 'light');

    applyTheme(initialTheme);

    window.addEventListener('message', (event) => {
      if (!event.data || event.data.type !== MESSAGE_TYPE) return;
      manualOverride = false;
      applyTheme(event.data.theme);
    });

    document.addEventListener('visibilitychange', () => {
      if (document.hidden) return;
      if (manualOverride) return;
      const parentTheme = readParentTheme();
      if (parentTheme) {
        applyTheme(parentTheme);
      }
    });

    const handlePrefChange = (event) => {
      if (manualOverride) return;
      if (readParentTheme()) return;
      applyTheme(event.matches ? 'dark' : 'light');
    };

    if (typeof prefersDark.addEventListener === 'function') {
      prefersDark.addEventListener('change', handlePrefChange);
    } else if (typeof prefersDark.addListener === 'function') {
      prefersDark.addListener(handlePrefChange);
    }

    const registerToggle = (btn) => {
      btn.addEventListener('click', () => {
        manualOverride = true;
        const current = document.documentElement.dataset[THEME_DATA_KEY] || 'light';
        applyTheme(current === 'dark' ? 'light' : 'dark');
      });
    };

    document.querySelectorAll('[data-report-theme-toggle]').forEach(registerToggle);

    window.reportTheme = {
      apply: applyTheme,
      syncFromParent: () => {
        const parentTheme = readParentTheme();
        if (parentTheme) {
          manualOverride = false;
          applyTheme(parentTheme);
        }
      },
    };
  };

  if (document.readyState === 'complete' || document.readyState === 'interactive') {
    init();
  } else {
    document.addEventListener('DOMContentLoaded', init, { once: true });
  }
})();
