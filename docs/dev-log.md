# Dev log

## 2026-10-06 — restructure-by-direction

**Goal:** Replace the `domain` / `application` / `framework` layout with one grouped by direction (`domain`, `core`, `inbound`, `outbound`), so inbound and outbound code can't be mixed by accident. The dependency rules are enforced by the build rather than by convention.

**Steps**
1. Moved modules: `application/core` → `core/`, `application/port-inbound` → `inbound/port/`, `framework/adapter-inbound` → `inbound/adapter-rest/`, `application/port-outbound` → `outbound/port/`, `framework/adapter-outbound` → `outbound/adapter-persistence/`. Removed the `application` and `framework` aggregators.
2. Rewrote the POMs. Each side has its own groupId (`com.wixia.inbound`, `com.wixia.outbound`), and every module declares its own dependencies with `${project.version}`. The parent POMs now carry no dependencies.
3. Added `maven-enforcer-plugin` 3.6.2. Each side's parent bans the other side's groupId and `com.wixia:core`. Each port bans its own side's groupId, so a port can't depend on its own side's adapters.
4. Verified that `mvn validate` passes. Also checked in a scratch copy that it fails for: inbound adapter → outbound port, outbound adapter → core, inbound port → inbound adapter, and inbound adapter → core (which would bring in outbound-port transitively).

**Decisions / deviations**
- Ports are flat siblings of the adapters (`inbound/port`), not nested under them. A `jar` module can't have children, and a nested port would inherit the adapter's parent configuration.
- The port artifactIds are `inbound-port` and `outbound-port`, not just `port`. Separate groupIds already keep the coordinates unique, but two jars both named `port-1.0-SNAPSHOT.jar` would clash in a shared `lib/` directory or a fat jar.
- The adapter names `adapter-rest` and `adapter-persistence` are placeholders for the eventual technology.
- Follow-up the same day: added `core-isolation`, which bans both sides' groupIds but allows `inbound-port` and `outbound-port` back in through `<includes>`. Also added `domain-isolation`, which bans `com.wixia`, `com.wixia.inbound` and `com.wixia.outbound`. Verified that both fail on a violation. For any module that already depends on `domain`, a domain → module dependency is caught first by Maven's cycle detection. The domain rule matters for future modules that don't depend on `domain` (e.g. a shared `common`).
- Known, not caused by this change: the archetype's `AppTest` placeholders use JUnit 3 while the root POM provides JUnit 5, so `test-compile` fails.
