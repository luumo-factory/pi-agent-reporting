# Pi Agent Reporting

Pi Agent Reporting is a Spring Boot + Thymeleaf dashboard for monitoring Markdown reports produced by Pi coding agents. It watches a directory on disk, extracts metadata, renders the Markdown into safe HTML, and serves a responsive web UI with live updates, notifications, and basic triage tools (read/flag state, project filters, etc.).

## Highlights

- **Filesystem-based ingestion** – recursively scans a configurable directory (defaults to `./reports` or `REPORTS_DIR`) and caches metadata for every `*.md` file.
- **CommonMark rendering with sanitisation** – Markdown is parsed with CommonMark plus GFM tables/autolink, and raw HTML is escaped to avoid stored XSS.
- **Live browser UI** – a modern vanilla JS frontend polls the API, tracks read/flag status, filters by project, toggles dark/light mode, and caches report content while automatically invalidating when files change.
- **Shared application state** – read/flag/current selections are persisted to `state.json` (configurable) via a thread-safe `StateService` that autosaves every 5 seconds without blocking report scans.
- **Auditable REST API** – exposes JSON/HTML/raw endpoints for reports and a state API used by the UI (see below).
- **PWA niceties** – manifest, icons, theme toggle, resizeable sidebar, notification sound / text-to-speech controls.

## Project structure

```
pi-agent-reporting/
├── pom.xml
├── src/
│   ├── main/java/ai/luumo/tools/picodingagent/reporting/
│   │   ├── PiAgentReportingApplication.java
│   │   ├── config/
│   │   │   ├── SchedulingConfig.java
│   │   │   └── WebConfig.java
│   │   ├── controller/
│   │   │   ├── HomeController.java
│   │   │   ├── ReportController.java
│   │   │   └── StateController.java
│   │   ├── model/
│   │   │   ├── ApplicationState.java
│   │   │   ├── Report.java
│   │   │   └── ReportWithState.java
│   │   └── service/
│   │       ├── MarkdownService.java
│   │       ├── ReportScannerService.java
│   │       └── StateService.java
│   └── main/resources/
│       ├── application.properties
│       ├── templates/
│       │   ├── index.html
│       │   └── report-wrapper.html
│       └── static/
│           ├── css/main.css
│           ├── js/main.js
│           ├── img/*.png
│           ├── favicon.*
│           ├── manifest.json
│           └── sw.js
└── .gitignore
```

## Configuration

Key properties live in `src/main/resources/application.properties`:

```properties
# Server
server.port=9000

# Reports directory + scan cadence (ms)
app.reports.directory=${REPORTS_DIR:reports}
app.reports.scan-interval=2000

# Scheduled task pool size
app.scheduler.pool-size=4

# Thymeleaf
spring.thymeleaf.cache=false
```

- Override the reports directory with the `REPORTS_DIR` environment variable or by editing `app.reports.directory`.
- `state.json` (or whatever `app.state.file` is set to) will be created automatically; ensure the process has write access.
- The scheduler pool size separates long-running scans from the auto-save task.

## API surface

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/reports` | Returns metadata plus read/flag state for every report. |
| `GET` | `/api/reports/html/{path}` | Renders a Markdown report as HTML wrapped in the viewer template. Validated relative paths only. |
| `GET` | `/api/reports/raw/{path}` | Returns the raw Markdown. Shares the same security/normalisation as the HTML endpoint. |
| `GET` | `/api/state` | Returns a snapshot of the application state. |
| `POST` | `/api/state/read/{path}` | Mark report as read. |
| `POST` | `/api/state/unread/{path}` | Mark report as unread. |
| `POST` | `/api/state/flag/{path}` | Toggle the flagged state. |
| `POST` | `/api/state/current` | Set the currently viewed report (`{"path":"..."}`). |
| `POST` | `/api/state/auto-read` | Enable/disable auto-read (`{"enabled": true}`). |
| `POST` | `/api/state/notification-mode` | Set notification mode to `bell`, `tts`, or `silence`. |

All `path` parameters are URL-decoded, normalised, and must refer to files inside the configured reports directory; traversal attempts are rejected.

## Building & running

Requirements: **Java 25** and **Maven 3.9+**.

```bash
mvn clean package
java -jar target/pi-agent-reporting-1.0.0-SNAPSHOT.jar
```

For iterative work you can rely on Spring Boot DevTools:

```bash
mvn spring-boot:run
```

Then open [http://localhost:9000](http://localhost:9000) to view the dashboard.

## Generating reports with Pi agents

Add something like the following to your agent instructions so they write Markdown reports into the watched directory:

```markdown
When you complete a non-trivial task, write a Markdown report to the reports
folder (default: ~/reports, override via REPORTS_DIR). Use filenames like
`YYYY-MM-DD_summary.md` and create subfolders per project when helpful.
```

The scanner understands the `YYYY-MM-DD_description.md` convention, but also extracts `# Heading` titles from the file when available.

## Development tips

- Runtime artefacts (e.g. `state.json`, `app.log`, `target/`) are ignored by Git—keep them that way.
- Report metadata is cached in-memory; if you need to reset it during development call `ReportScannerService.scanReports()` via the actuator or restart the app.
- When adjusting the frontend, remember it relies entirely on the `/api/reports` payload—`index.html` intentionally renders an empty list so the JS bootstraps itself.
- The service worker only caches static assets; API calls always go to the network with an offline fallback message.

## License

Copyright © 2026 Luumo Factory.
