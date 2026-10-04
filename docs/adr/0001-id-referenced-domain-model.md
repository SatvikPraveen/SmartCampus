# ADR 0001: Domain entities reference each other by ID

- Status: accepted
- Date: 2026-10-04

## Context

The models in `models/` store relationships as identifiers (`Course.getProfessorId()`,
`Enrollment.getCourseId()`, `Student.getDepartmentId()`). Much of the consumer code was
written against an object graph (`enrollment.getCourse().getDepartment()`) that never
existed, and the code base did not compile as a result (about 830 errors).

## Decision

Treat the ID-referenced models as the source of truth and adapt consumers to them. Where a
consumer depended on data that no model stores (course delivery mode, honours status,
office hours), remove that feature instead of fabricating values.

## Consequences

- Entities serialise without cycles and map directly onto relational rows.
- Navigating a relationship needs a repository or service lookup, which makes the data
  access explicit.
- A few convenience predicates and report fields were removed. Each removal is recorded in
  the commit history.
