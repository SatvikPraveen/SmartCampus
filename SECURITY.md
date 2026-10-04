# Security policy

## Supported versions

Only the latest commit on `main` receives fixes.

## Reporting a vulnerability

Please report vulnerabilities privately through
[GitHub security advisories](https://github.com/SatvikPraveen/SmartCampus/security/advisories/new)
rather than public issues. Include a description, reproduction steps and the affected commit.
You can expect an acknowledgement within seven days.

## Scope notes

- The REST service is stateless and unauthenticated by design; deploy it behind your own
  gateway if exposure beyond a trusted network is required.
- Request size is bounded (`TimetablingDtos.MAX_EVENTS`, `MAX_ROOMS`, `MAX_ITERATIONS`) to keep
  the CPU cost of a single request within seconds. Treat changes to these limits as
  security-relevant.
- The `security` package (password hashing, tokens, role-based access) is a domain library
  component and is not wired into the HTTP layer.
