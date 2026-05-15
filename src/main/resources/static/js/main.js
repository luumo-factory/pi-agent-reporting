    const THEME_CHANGE_EVENT = 'reports-theme-change';

    class NotificationManager {
      constructor() {
        this.storageKey = 'reports-notifications';
        this.button = document.getElementById('bell-toggle');
        this.mode = 'bell'; // 'silence', 'bell', or 'tts'
        this.audioContext = null;
        this.speechSynthesis = window.speechSynthesis;
        this.voicesLoaded = false;

        // Load voices for TTS (they may load asynchronously)
        if (this.speechSynthesis) {
          // Chrome/Edge loads voices asynchronously
          if (this.speechSynthesis.onvoiceschanged !== undefined) {
            this.speechSynthesis.onvoiceschanged = () => {
              this.voicesLoaded = true;
            };
          }
          // Trigger voice loading
          this.speechSynthesis.getVoices();
        }

        // Load saved preference (localStorage takes precedence over server state)
        try {
          const saved = localStorage.getItem(this.storageKey);
          // Accept any of the three valid modes
          if (saved === 'silence' || saved === 'bell' || saved === 'tts') {
            this.mode = saved;
          } else if (this.button && this.button.dataset.mode) {
            // Fallback to server-provided initial mode
            const serverMode = this.button.dataset.mode;
            if (serverMode === 'silence' || serverMode === 'bell' || serverMode === 'tts') {
              this.mode = serverMode;
            }
          }
        } catch (e) {
          console.warn('Unable to load notification preference:', e);
        }

        this.updateButton();
        this.button?.addEventListener('click', () => this.toggle());
      }

      toggle() {
        // Cycle through: silence → bell → tts → silence
        if (this.mode === 'silence') {
          this.mode = 'bell';
        } else if (this.mode === 'bell') {
          this.mode = 'tts';
        } else {
          this.mode = 'silence';
        }

        this.updateButton();

        try {
          localStorage.setItem(this.storageKey, this.mode);
        } catch (e) {
          console.warn('Unable to save notification preference:', e);
        }

        // Send mode to server
        fetch('/api/state/notification-mode', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ mode: this.mode })
        }).catch(err => console.warn('Failed to sync notification mode:', err));

        // Play test notification when enabling bell or tts
        if (this.mode === 'bell') {
          this.playBell();
        } else if (this.mode === 'tts') {
          this.playTTS('Voice notification enabled');
        }
      }

      updateButton() {
        if (!this.button) return;
        
        // Set active class for bell and tts modes
        this.button.classList.toggle('active', this.mode === 'bell' || this.mode === 'tts');
        
        // Update title based on mode
        const titles = {
          'silence': 'Notifications off (click to enable bell)',
          'bell': 'Bell notifications (click for text-to-speech)',
          'tts': 'Text-to-speech notifications (click to disable)'
        };
        this.button.title = titles[this.mode] || titles.bell;
        
        // Update icon based on mode
        const svg = this.button.querySelector('svg');
        if (!svg) return;
        
        if (this.mode === 'silence') {
          // Bell icon with slash
          svg.innerHTML = `
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
            <line class="slash-line" x1="3" y1="3" x2="21" y2="21" stroke="currentColor" stroke-width="2" stroke-linecap="round"></line>
          `;
        } else if (this.mode === 'bell') {
          // Bell icon without slash
          svg.innerHTML = `
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
          `;
        } else if (this.mode === 'tts') {
          // Megaphone icon
          svg.innerHTML = `
            <path d="M3 11v3a1 1 0 0 0 1 1h1l4 3V8L5 11H4a1 1 0 0 0-1 1z"></path>
            <path d="M15.54 8.46a5 5 0 0 1 0 7.07"></path>
            <path d="M18.36 5.64a9 9 0 0 1 0 12.72"></path>
          `;
        }
      }

      playBell() {
        try {
          // Create audio context lazily (requires user interaction)
          if (!this.audioContext) {
            this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
          }

          const ctx = this.audioContext;
          const now = ctx.currentTime;

          // Create a pleasant notification sound (two-tone chime)
          const playTone = (frequency, startTime, duration, volume = 0.15) => {
            const oscillator = ctx.createOscillator();
            const gainNode = ctx.createGain();

            oscillator.connect(gainNode);
            gainNode.connect(ctx.destination);

            oscillator.frequency.value = frequency;
            oscillator.type = 'sine';

            // Envelope for smooth sound
            gainNode.gain.setValueAtTime(0, startTime);
            gainNode.gain.linearRampToValueAtTime(volume, startTime + 0.01);
            gainNode.gain.exponentialRampToValueAtTime(0.01, startTime + duration);

            oscillator.start(startTime);
            oscillator.stop(startTime + duration);
          };

          // Two-tone chime: E5 -> A5
          playTone(659.25, now, 0.15, 0.12);  // E5
          playTone(880.00, now + 0.1, 0.25, 0.1);  // A5

        } catch (error) {
          console.warn('Unable to play notification sound:', error);
        }
      }

      warmupTTS() {
        // Warm up the TTS engine with a silent utterance
        // This helps ensure it responds quickly when actually needed
        if (!this.speechSynthesis) return;
        
        try {
          // Use robust pattern even for warmup
          this.speechSynthesis.cancel();
          this.speechSynthesis.resume();
          
          const utterance = new SpeechSynthesisUtterance(' ');
          utterance.volume = 0;
          this.speechSynthesis.speak(utterance);
        } catch (error) {
          console.debug('TTS warmup failed (not critical):', error);
        }
      }

      playTTS(message) {
        if (!this.speechSynthesis) {
          console.warn('Text-to-speech not supported in this browser');
          // Fallback to bell
          this.playBell();
          return;
        }

        try {
          // Robust speech synthesis pattern:
          // Cancel any ongoing speech and ensure the engine is resumed
          this.speechSynthesis.cancel();
          this.speechSynthesis.resume();

          // Create utterance
          const utterance = new SpeechSynthesisUtterance(message);
          
          // Set speech properties for chirpy female voice at high rate
          utterance.rate = 1.1; // Reasonably high speech rate
          utterance.pitch = 1.2; // Slightly higher pitch
          
          // Try to select a female voice
          const voices = this.speechSynthesis.getVoices();
          const femaleVoice = voices.find(voice => 
            voice.name.toLowerCase().includes('female') ||
            voice.name.toLowerCase().includes('zira') ||
            voice.name.toLowerCase().includes('samantha') ||
            voice.name.toLowerCase().includes('victoria') ||
            voice.name.toLowerCase().includes('google uk english female') ||
            voice.name.toLowerCase().includes('google us english') && voice.name.includes('2')
          );
          
          if (femaleVoice) {
            utterance.voice = femaleVoice;
          }
          
          // Speak the message
          this.speechSynthesis.speak(utterance);
          
        } catch (error) {
          console.warn('Unable to play text-to-speech:', error);
          // Fallback to bell
          this.playBell();
        }
      }

      play(reportInfo) {
        // reportInfo should be { count: number, titles: string[] }
        if (this.mode === 'silence') {
          return; // Do nothing
        }

        if (this.mode === 'bell') {
          this.playBell();
          return;
        }

        if (this.mode === 'tts') {
          let message;
          if (reportInfo && reportInfo.count === 1 && reportInfo.titles && reportInfo.titles.length > 0) {
            // Single report: say the title
            message = `New Pi report - ${reportInfo.titles[0]}`;
          } else if (reportInfo && reportInfo.count > 1) {
            // Multiple reports: just say the count
            message = `${reportInfo.count} new Pi reports`;
          } else {
            // Fallback
            message = 'New Pi report';
          }
          this.playTTS(message);
          return;
        }
      }
    }

    class ThemeController {
      constructor() {
        this.storageKey = 'reports-theme';
        this.button = document.getElementById('theme-toggle');
        this.prefQuery = window.matchMedia('(prefers-color-scheme: dark)');
        this.storageAvailable = this.checkStorage();
        this.hasExplicitPreference = false;
        this.currentTheme = null;

        const saved = this.storageAvailable ? localStorage.getItem(this.storageKey) : null;
        this.hasExplicitPreference = Boolean(saved);
        const initialTheme = saved || (this.prefQuery.matches ? 'dark' : 'light');
        this.apply(initialTheme, { persist: false });

        this.button?.addEventListener('click', () => this.toggle());

        const handlePrefChange = (event) => {
          if (this.hasExplicitPreference) return;
          this.apply(event.matches ? 'dark' : 'light', { persist: false });
        };

        if (typeof this.prefQuery.addEventListener === 'function') {
          this.prefQuery.addEventListener('change', handlePrefChange);
        } else if (typeof this.prefQuery.addListener === 'function') {
          this.prefQuery.addListener(handlePrefChange);
        }
      }

      checkStorage() {
        try {
          const testKey = '__theme_test__';
          localStorage.setItem(testKey, '1');
          localStorage.removeItem(testKey);
          return true;
        } catch (error) {
          console.warn('Theme storage unavailable:', error);
          return false;
        }
      }

      toggle() {
        const next = document.body.dataset.theme === 'dark' ? 'light' : 'dark';
        this.hasExplicitPreference = true;
        this.apply(next);
      }

      apply(theme, { persist = true } = {}) {
        document.body.dataset.theme = theme;
        document.documentElement.dataset.theme = theme;
        this.currentTheme = theme;
        if (this.button) {
          this.button.title = theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode';
        }
        if (persist && this.storageAvailable) {
          localStorage.setItem(this.storageKey, theme);
        }
        window.dispatchEvent(new CustomEvent(THEME_CHANGE_EVENT, { detail: theme }));
      }

      getCurrentTheme() {
        return this.currentTheme || document.body.dataset.theme || document.documentElement.dataset.theme || 'light';
      }
    }

    class ReportViewer {
      constructor(options) {
        this.pollInterval = options.pollInterval || 3000;
        this.reportListEl = document.getElementById('report-list');
        this.mainHeaderEl = document.getElementById('main-header');
        this.renderedContentEl = document.getElementById('rendered-content');
        this.rawContentEl = document.getElementById('raw-content');
        this.toggleRawBtn = document.getElementById('toggle-raw');
        this.activeReportEl = document.getElementById('active-report');
        this.errorEl = document.getElementById('error-message');
        this.iconUnread = document.getElementById('icon-unread');
        this.iconRead = document.getElementById('icon-read');
        this.notificationSound = options.notificationSound;
        this.projectSelectEl = document.getElementById('project-select');
        this.filterClearBtn = document.getElementById('filter-clear');
        this.mobileCloseBtn = document.getElementById('mobile-close-viewer');
        this.mobileViewport = typeof window.matchMedia === 'function' ? window.matchMedia('(max-width: 768px)') : null;

        this.mobileViewportListener = (event) => {
          if (!event.matches) {
            this.closeMobileViewer();
          }
        };

        if (this.mobileViewport) {
          if (typeof this.mobileViewport.addEventListener === 'function') {
            this.mobileViewport.addEventListener('change', this.mobileViewportListener);
          } else if (typeof this.mobileViewport.addListener === 'function') {
            this.mobileViewport.addListener(this.mobileViewportListener);
          }
        }

        this.mobileCloseBtn?.addEventListener('click', () => this.closeMobileViewer());

        this.timer = null;
        this.knownReports = new Set(); // Set of relative paths
        this.readReports = new Set(); // Set of relative paths
        this.flaggedReports = new Set(); // Set of relative paths
        this.selectedReport = null; // Currently selected relative path
        this.initialLoad = true;
        this.cachedContent = new Map(); // Cache for rendered and raw content
        this.reportVersions = new Map(); // Track last-known timestamps per report
        this.newReports = new Set(); // Track newly added reports
        this.currentFilter = 'all'; // Current project filter
        this.allProjects = new Set(); // Track all available projects
        this.showingRaw = false; // Track whether we're showing raw markdown

        // Initialize with server-rendered reports
        const initialReports = this.reportListEl.querySelectorAll('.report');
        initialReports.forEach((el) => {
          const path = el.dataset.report; // Now contains relative path
          const project = el.dataset.project || 'global';
          this.knownReports.add(path);
          this.readReports.add(path);
          this.allProjects.add(project);
          
          // Check if flagged from server state
          if (el.classList.contains('flagged')) {
            this.flaggedReports.add(path);
          }
        });
        
        // Use event delegation for report list interactions
        this.reportListEl.addEventListener('click', (e) => {
          const report = e.target.closest('.report');
          if (!report) return;
          
          const path = report.dataset.report; // Relative path
          
          // Handle read toggle button
          if (e.target.closest('.read-toggle')) {
            e.stopPropagation();
            this.toggleReadState(path);
            return;
          }
          
          // Handle flag toggle button
          if (e.target.closest('.flag-toggle')) {
            e.stopPropagation();
            this.toggleFlag(path);
            return;
          }
          
          // Handle project tag click
          if (e.target.closest('.report-project')) {
            e.stopPropagation();
            const project = e.target.closest('.report-project').dataset.project;
            this.filterByProject(project);
            return;
          }
          
          // Default: select the report
          this.selectReport(path);
        });

        this.updateProjectFilter();

        // Set up event listeners for filter
        if (this.projectSelectEl) {
          this.projectSelectEl.addEventListener('change', (e) => {
            this.filterByProject(e.target.value);
          });
        }

        if (this.filterClearBtn) {
          this.filterClearBtn.addEventListener('click', () => {
            this.filterByProject('all');
          });
        }

        if (initialReports.length > 0) {
          this.selectedReport = initialReports[0].dataset.report; // Relative path
          this.loadReport(this.selectedReport);
          this.initialLoad = false;
        }

        // Register toggle raw button
        if (this.toggleRawBtn) {
          this.toggleRawBtn.addEventListener('click', () => this.toggleRawView());
        }

        // Update relative times initially and set up interval
        this.updateAllRelativeTimes();
        setInterval(() => this.updateAllRelativeTimes(), 5000);

        // Fetch reports immediately on page load
        this.pollReports();
      }

      updateProjectFilter() {
        if (!this.projectSelectEl) return;

        // Clear existing options (except 'All')
        const existingOptions = this.projectSelectEl.querySelectorAll('option:not([value="all"])');
        existingOptions.forEach(opt => opt.remove());

        const sortedProjects = Array.from(this.allProjects).sort();
        sortedProjects.forEach(project => {
          const option = document.createElement('option');
          option.value = project;
          option.textContent = project;
          this.projectSelectEl.appendChild(option);
        });

        if (this.currentFilter !== 'all' && !this.allProjects.has(this.currentFilter)) {
          this.currentFilter = 'all';
        }

        this.projectSelectEl.value = this.currentFilter;
        if (this.filterClearBtn) {
          this.filterClearBtn.disabled = (this.currentFilter === 'all');
        }
      }

      filterByProject(project) {
        if (project !== 'all' && !this.allProjects.has(project)) {
          project = 'all';
        }

        this.currentFilter = project;

        if (this.projectSelectEl) {
          this.projectSelectEl.value = project;
        }

        if (this.filterClearBtn) {
          this.filterClearBtn.disabled = (project === 'all');
        }

        const reports = this.reportListEl.querySelectorAll('.report');
        reports.forEach(report => {
          const reportProject = report.dataset.project || 'global';
          if (project === 'all' || reportProject === project) {
            report.style.display = '';
          } else {
            report.style.display = 'none';
          }
        });
      }

      slugifyProject(project) {
        return (project || 'global')
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, '-')
          .replace(/^-+|-+$/g, '') || 'global';
      }

      async toggleReadState(path) {
        const isRead = this.readReports.has(path);
        // Encode each path segment separately to preserve forward slashes
        const encodedPath = path.split('/').map(segment => encodeURIComponent(segment)).join('/');
        const endpoint = isRead ? `/api/state/unread/${encodedPath}` : `/api/state/read/${encodedPath}`;
        
        try {
          const response = await fetch(endpoint, { method: 'POST' });
          if (!response.ok) throw new Error('Failed to update read state');
          
          // Update local state
          if (isRead) {
            this.readReports.delete(path);
          } else {
            this.readReports.add(path);
          }
          
          // Update UI
          const node = this.reportListEl.querySelector(`.report[data-report="${CSS.escape(path)}"]`);
          if (node) {
            this.applyReadState(node, path);
            this.updateReadButton(node, path);
          }
        } catch (error) {
          console.error('Error toggling read state:', error);
        }
      }
      
      async toggleFlag(path) {
        // Encode each path segment separately to preserve forward slashes
        const encodedPath = path.split('/').map(segment => encodeURIComponent(segment)).join('/');
        try {
          const response = await fetch(`/api/state/flag/${encodedPath}`, { method: 'POST' });
          if (!response.ok) throw new Error('Failed to toggle flag');
          
          // Update local state
          if (this.flaggedReports.has(path)) {
            this.flaggedReports.delete(path);
          } else {
            this.flaggedReports.add(path);
          }
          
          // Update UI
          const node = this.reportListEl.querySelector(`.report[data-report="${CSS.escape(path)}"]`);
          if (node) {
            this.applyFlagState(node, path);
          }
        } catch (error) {
          console.error('Error toggling flag:', error);
        }
      }
      
      updateReadButton(node, path) {
        const readBtn = node.querySelector('.read-toggle span');
        if (readBtn) {
          readBtn.textContent = this.readReports.has(path) ? '✓' : '•';
        }
      }
      
      applyFlagState(node, path) {
        const isFlagged = this.flaggedReports.has(path);
        node.classList.toggle('flagged', isFlagged);
        
        const flagBtn = node.querySelector('.flag-toggle');
        if (flagBtn) {
          flagBtn.classList.toggle('flagged', isFlagged);
        }
      }

      scheduleNextPoll() {
        clearTimeout(this.timer);
        this.timer = setTimeout(() => this.pollReports(), this.pollInterval);
      }

      async pollReports() {
        try {
          this.errorEl.hidden = true;
          const response = await fetch('/api/reports');
          if (!response.ok) throw new Error(`HTTP ${response.status}`);

          const reports = await response.json();
          this.updateUI(reports);
        } catch (error) {
          console.error('Poll error:', error);
          this.errorEl.textContent = 'Unable to refresh reports: ' + error.message;
          this.errorEl.hidden = false;
        } finally {
          this.scheduleNextPoll();
        }
      }

      updateUI(reports) {
        if (!reports.length) {
          this.renderEmptyState();
          this.renderedContentEl.innerHTML = '<div class="empty-state">No reports available</div>';
          this.knownReports = new Set();
          this.readReports = new Set();
          this.flaggedReports = new Set();
          this.cachedContent.clear();
          this.reportVersions.clear();
          this.newReports.clear();
          this.allProjects = new Set();
          this.updateProjectFilter();
          this.filterByProject('all');
          this.selectedReport = null;
          this.initialLoad = true;
          return;
        }
        
        this.clearEmptyState();

        // Only sync server state on initial load
        const isInitialLoad = this.initialLoad;
        if (isInitialLoad) {
          // Apply server state for read/flagged
          // Note: r.filename now contains full relative path (e.g., "project/report.md")
          reports.forEach(r => {
            if (r.read) {
              this.readReports.add(r.filename);
            }
            if (r.flagged) {
              this.flaggedReports.add(r.filename);
            }
          });
          this.initialLoad = false;
        }
        // After initial load, client state is source of truth
        // Server state changes only via explicit user actions (toggleReadState, toggleFlag)

        const existingNodes = new Map();
        this.reportListEl.querySelectorAll('.report').forEach((el) => {
          existingNodes.set(el.dataset.report, el);
        });

        let hasNewReport = false;
        let anchor = null;
        const newReportTitles = [];

        reports.forEach((report) => {
          // report.filename now contains full relative path
          const isNew = !this.knownReports.has(report.filename);
          if (isNew && !isInitialLoad) {
            hasNewReport = true;
            this.newReports.add(report.filename);
            newReportTitles.push(report.title);
          }

          const project = report.project || 'global';
          this.reportVersions.set(report.filename, report.timestampISO);

          let node = existingNodes.get(report.filename);
          if (!node) {
            node = this.createReportNode(report);
          } else {
            this.updateReportNode(node, report);
          }
          this.applyReadState(node, report.filename);
          this.applyFlagState(node, report.filename);
          this.updateReadButton(node, report.filename);

          if (this.newReports.has(report.filename)) {
            node.classList.add('new-highlight');
            setTimeout(() => {
              node.classList.remove('new-highlight');
              this.newReports.delete(report.filename);
            }, 30000);
          }

          if (this.currentFilter !== 'all' && project !== this.currentFilter) {
            node.style.display = 'none';
          } else {
            node.style.display = '';
          }

          const desiredNext = anchor ? anchor.nextSibling : this.reportListEl.firstChild;
          if (node !== desiredNext) {
            this.reportListEl.insertBefore(node, desiredNext);
          }

          anchor = node;
          existingNodes.delete(report.filename);
        });

        this.allProjects = new Set(reports.map((r) => r.project || 'global'));
        this.updateProjectFilter();
        this.filterByProject(this.currentFilter);

        existingNodes.forEach((node, key) => {
          node.remove();
          this.cachedContent.delete(key);
          this.reportVersions.delete(key);
        });

        this.knownReports = new Set(reports.map((r) => r.filename));

        if (hasNewReport) {
          this.triggerHeaderPulse();
          this.notificationSound?.play({
            count: newReportTitles.length,
            titles: newReportTitles
          });
        }

        if (!this.selectedReport) {
          this.selectReport(reports[0].filename, { auto: true });
        }
      }

      createReportNode(report) {
        const article = document.createElement('article');
        article.className = 'report read';
        const projectLabel = report.project || 'global';
        article.dataset.report = report.filename;
        article.dataset.timestamp = report.timestampISO;
        article.dataset.project = projectLabel;

        const wrap = document.createElement('div');
        wrap.className = 'envelope-wrap';
        wrap.dataset.state = 'read';
        wrap.appendChild(this.iconRead.content.cloneNode(true));

        const info = document.createElement('div');
        info.className = 'report-info';

        const title = document.createElement('p');
        title.className = 'report-title';
        title.textContent = report.title;

        const meta = document.createElement('p');
        meta.className = 'report-meta';
        meta.dataset.timestamp = report.timestampISO;
        meta.textContent = this.formatRelativeTime(report.timestampISO);

        const projectTag = document.createElement('span');
        projectTag.className = 'report-project ' + this.slugifyProject(projectLabel);
        projectTag.textContent = projectLabel;
        projectTag.dataset.project = projectLabel;

        info.appendChild(title);
        info.appendChild(meta);
        info.appendChild(projectTag);

        // Create action buttons
        const actions = document.createElement('div');
        actions.className = 'report-actions';
        
        const readBtn = document.createElement('button');
        readBtn.className = 'action-btn read-toggle';
        readBtn.type = 'button';
        readBtn.title = 'Toggle read/unread';
        const readSpan = document.createElement('span');
        readSpan.textContent = '•';
        readBtn.appendChild(readSpan);
        
        const flagBtn = document.createElement('button');
        flagBtn.className = 'action-btn flag-toggle';
        flagBtn.type = 'button';
        flagBtn.title = 'Toggle flag';
        const flagSpan = document.createElement('span');
        flagSpan.textContent = '🚩';
        flagBtn.appendChild(flagSpan);
        
        actions.appendChild(readBtn);
        actions.appendChild(flagBtn);

        article.appendChild(wrap);
        article.appendChild(info);
        article.appendChild(actions);

        return article;
      }

      updateReportNode(node, report) {
        const title = node.querySelector('.report-title');
        const meta = node.querySelector('.report-meta');
        const projectLabel = report.project || 'global';
        if (title) title.textContent = report.title;
        if (meta) {
          meta.dataset.timestamp = report.timestampISO;
          meta.textContent = this.formatRelativeTime(report.timestampISO);
        }
        if (node.dataset) {
          node.dataset.timestamp = report.timestampISO;
          node.dataset.project = projectLabel;
        }
        const projectTag = node.querySelector('.report-project');
        if (projectTag) {
          projectTag.dataset.project = projectLabel;
          projectTag.textContent = projectLabel;
          projectTag.className = 'report-project ' + this.slugifyProject(projectLabel);
        }
      }

      async markAsRead(path) {
        if (this.readReports.has(path)) return;
        
        // Update server state
        // Encode each path segment separately to preserve forward slashes
        const encodedPath = path.split('/').map(segment => encodeURIComponent(segment)).join('/');
        try {
          const response = await fetch(`/api/state/read/${encodedPath}`, { method: 'POST' });
          if (!response.ok) {
            console.error('Failed to mark as read on server');
          }
        } catch (error) {
          console.error('Error marking as read:', error);
        }
        
        // Update local state
        this.readReports.add(path);
        const node = this.reportListEl.querySelector(`.report[data-report="${CSS.escape(path)}"]`);
        if (node) this.applyReadState(node, path);
      }

      applyReadState(node, path) {
        const isRead = this.readReports.has(path);
        node.classList.toggle('read', isRead);
        node.classList.toggle('unread', !isRead);

        const wrap = node.querySelector('.envelope-wrap');
        if (!wrap) return;

        const currentState = wrap.dataset.state;
        const nextState = isRead ? 'read' : 'unread';
        if (currentState === nextState) return;

        wrap.dataset.state = nextState;
        wrap.innerHTML = '';
        const tpl = isRead ? this.iconRead : this.iconUnread;
        wrap.appendChild(tpl.content.cloneNode(true));
      }

      async selectReport(path, { auto = false } = {}) {
        this.selectedReport = path; // Store full relative path

        if (!auto) {
          await this.markAsRead(path);
        }

        this.reportListEl.querySelectorAll('.report').forEach((el) => {
          el.classList.toggle('active', el.dataset.report === path);
        });

        this.loadReport(path);

        if (!auto && this.isMobileView()) {
          this.openMobileViewer();
        }
      }

      async loadReport(path) {
        // Update active report label - show just the filename for cleaner display
        if (this.activeReportEl) {
          // Extract filename from path for display
          const displayName = path.includes('/') ? path.substring(path.lastIndexOf('/') + 1) : path;
          this.activeReportEl.textContent = displayName;
          // Store full path as data attribute for debugging
          this.activeReportEl.dataset.fullPath = path;
        }
        
        // Show toggle button
        if (this.toggleRawBtn) {
          this.toggleRawBtn.style.display = '';
        }
        
        // Reset to rendered view
        this.showingRaw = false;
        this.renderedContentEl.classList.remove('hidden');
        this.rawContentEl.classList.add('hidden');
        if (this.toggleRawBtn) {
          this.toggleRawBtn.textContent = 'Show Markdown';
        }
        
        // Show loading state
        this.renderedContentEl.classList.add('loading');

        try {
          const latestTimestamp = this.reportVersions.get(path) || null;
          const cacheEntry = this.cachedContent.get(path);
          const cacheTimestamp = cacheEntry?.timestamp ?? null;
          const needsRefresh = !cacheEntry || latestTimestamp !== cacheTimestamp;

          if (needsRefresh) {
            // Load both rendered and raw in parallel
            // Encode each path segment separately to preserve forward slashes
            const encodedPath = path.split('/').map(segment => encodeURIComponent(segment)).join('/');
            const [renderedResp, rawResp] = await Promise.all([
              fetch(`/api/reports/html/${encodedPath}`),
              fetch(`/api/reports/raw/${encodedPath}`)
            ]);

            if (!renderedResp.ok || !rawResp.ok) {
              throw new Error('Failed to load report');
            }

            const rendered = await renderedResp.text();
            const raw = await rawResp.text();

            this.cachedContent.set(path, { rendered, raw, timestamp: latestTimestamp });
          }

          const content = this.cachedContent.get(path);

          // Update content
          setTimeout(() => {
            this.renderedContentEl.innerHTML = content.rendered;
            this.rawContentEl.textContent = content.raw;

            // Show with animation
            this.renderedContentEl.classList.remove('loading');
          }, 150); // Small delay for smooth transition

        } catch (error) {
          console.error('Error loading report:', error);
          this.renderedContentEl.innerHTML = `<div class="empty-state" style="color: var(--danger);">Error loading report: ${error.message}</div>`;
          this.renderedContentEl.classList.remove('loading');
        }
      }
      
      toggleRawView() {
        this.showingRaw = !this.showingRaw;
        
        if (this.showingRaw) {
          this.renderedContentEl.classList.add('hidden');
          this.rawContentEl.classList.remove('hidden');
          if (this.toggleRawBtn) {
            this.toggleRawBtn.textContent = 'Show HTML';
          }
        } else {
          this.renderedContentEl.classList.remove('hidden');
          this.rawContentEl.classList.add('hidden');
          if (this.toggleRawBtn) {
            this.toggleRawBtn.textContent = 'Show Markdown';
          }
        }
      }

      isMobileView() {
        if (this.mobileViewport) {
          return this.mobileViewport.matches;
        }
        return window.innerWidth <= 768;
      }

      openMobileViewer() {
        if (!this.isMobileView()) {
          return;
        }
        document.body.classList.add('viewer-open');
      }

      closeMobileViewer() {
        document.body.classList.remove('viewer-open');
      }


      triggerHeaderPulse() {
        const header = this.mainHeaderEl;
        if (!header) return;
        header.classList.remove('pulsing');
        void header.offsetWidth;
        header.classList.add('pulsing');
        header.addEventListener('animationend', () => {
          header.classList.remove('pulsing');
        }, { once: true });
      }

      renderEmptyState() {
        this.reportListEl.innerHTML = '<div class="empty-state">No reports yet. Generate one and it will appear here.</div>';
      }

      clearEmptyState() {
        const empty = this.reportListEl.querySelector('.empty-state');
        if (empty) empty.remove();
      }

      formatRelativeTime(value) {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;

        const diffSeconds = Math.floor((Date.now() - date.getTime()) / 1000);
        const isPast = diffSeconds >= 0;
        const seconds = Math.abs(diffSeconds);
        if (seconds < 5) return 'just now';

        const fmt = (amount, unit) => {
          const plural = amount === 1 ? '' : 's';
          return isPast ? `${amount} ${unit}${plural} ago` : `in ${amount} ${unit}${plural}`;
        };

        if (seconds < 60) return fmt(seconds, 'second');
        const minutes = Math.floor(seconds / 60);
        if (minutes < 60) return fmt(minutes, 'minute');
        const hours = Math.floor(minutes / 60);
        if (hours < 24) return fmt(hours, 'hour');
        const days = Math.floor(hours / 24);
        if (days < 7) return fmt(days, 'day');
        const weeks = Math.floor(days / 7);
        if (weeks < 4) return fmt(weeks, 'week');
        return date.toLocaleDateString();
      }

      updateAllRelativeTimes() {
        const reports = this.reportListEl.querySelectorAll('.report');
        reports.forEach((report) => {
          const timestamp = report.dataset.timestamp;
          if (!timestamp) return;

          const meta = report.querySelector('.report-meta');
          if (!meta) return;

          const relativeTime = this.formatRelativeTime(timestamp);
          meta.textContent = relativeTime;
        });
      }
    }

    class ResizeController {
      constructor() {
        this.handle = document.getElementById('resize-handle');
        this.sidebar = document.querySelector('aside');
        this.isResizing = false;
        this.startX = 0;
        this.startWidth = 0;
        this.storageAvailable = this.checkStorage();
        
        // Load saved width from localStorage
        const savedWidth = this.storageAvailable ? this.safeGetWidth() : null;
        if (savedWidth) {
          this.sidebar.style.width = savedWidth + 'px';
        }
        
        this.initEventListeners();
      }
      
      checkStorage() {
        try {
          const key = '__resize_test__';
          localStorage.setItem(key, '1');
          localStorage.removeItem(key);
          return true;
        } catch (error) {
          console.warn('Resize storage unavailable:', error);
          return false;
        }
      }
      
      safeGetWidth() {
        try {
          return localStorage.getItem('sidebar-width');
        } catch (error) {
          console.warn('Unable to read sidebar width from storage:', error);
          return null;
        }
      }
      
      safeSetWidth(width) {
        if (!this.storageAvailable) return;
        try {
          localStorage.setItem('sidebar-width', width);
        } catch (error) {
          console.warn('Unable to persist sidebar width:', error);
        }
      }
      
      initEventListeners() {
        this.handle.addEventListener('mousedown', (e) => this.startResize(e));
        document.addEventListener('mousemove', (e) => this.resize(e));
        document.addEventListener('mouseup', () => this.stopResize());
      }
      
      startResize(e) {
        this.isResizing = true;
        this.startX = e.clientX;
        this.startWidth = this.sidebar.offsetWidth;
        this.handle.classList.add('resizing');
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';
        e.preventDefault();
      }
      
      resize(e) {
        if (!this.isResizing) return;
        
        const delta = e.clientX - this.startX;
        const newWidth = this.startWidth + delta;
        
        // Enforce min and max widths
        const minWidth = parseInt(getComputedStyle(this.sidebar).minWidth) || 240;
        const maxWidth = parseInt(getComputedStyle(this.sidebar).maxWidth) || 600;
        
        if (newWidth >= minWidth && newWidth <= maxWidth) {
          this.sidebar.style.width = newWidth + 'px';
        }
      }
      
      stopResize() {
        if (!this.isResizing) return;
        
        this.isResizing = false;
        this.handle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        
        this.safeSetWidth(this.sidebar.offsetWidth);
      }
    }

    document.addEventListener('DOMContentLoaded', () => {
      const themeController = new ThemeController();
      const notificationManager = new NotificationManager();
      const resizeController = new ResizeController();
      new ReportViewer({
        pollInterval: 3000,
        notificationSound: notificationManager,
      });
      
      // Warm up TTS after page load (with a delay to avoid blocking)
      setTimeout(() => {
        notificationManager.warmupTTS();
      }, 1500);
      
      // Register service worker for PWA support
      if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('/sw.js')
          .then(registration => {
            console.log('Service Worker registered:', registration.scope);
          })
          .catch(error => {
            console.log('Service Worker registration failed:', error);
          });
      }
    });
