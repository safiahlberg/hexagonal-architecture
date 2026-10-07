# Hexagonal architecture archetype

A Maven archetype that generates a new project from the library-lending sample in the parent directory: the same modules, enforcer rules, Spring Boot wiring, sample code and tests, under your own coordinates and package.

## Generating a project

Install the archetype into your local repository once:

```bash
cd archetype
mvn install
```

Then, from the directory where the new project should go:

```bash
mvn archetype:generate \
    -DarchetypeGroupId=com.wixia \
    -DarchetypeArtifactId=hexagonal-architecture-archetype \
    -DarchetypeVersion=1.0-SNAPSHOT \
    -DgroupId=org.example.shop \
    -DartifactId=lending-service \
    -Dversion=0.1-SNAPSHOT \
    -Dpackage=org.example.shop.lending \
    -B
```

| Parameter    | Becomes                                                                                       |
|--------------|-----------------------------------------------------------------------------------------------|
| `groupId`    | groupId of the root, `domain`, `core` and `bootstrap`; `<groupId>.inbound` and `<groupId>.outbound` for the two sides, including in the enforcer rules |
| `artifactId` | artifactId and directory of the root project                                                  |
| `version`    | version of every module                                                                       |
| `package`    | root package; each module gets its own sub-package (`<package>.domain`, `<package>.inbound.rest`, ...). Defaults to `groupId` |

Without `-B`, Maven also asks to confirm a `gitignore` property. Accept its default: it exists only to restore the name of `.gitignore`, which an archetype jar can't hold.

The module directories and artifactIds (`domain`, `inbound-port`, `adapter-rest`, ...) and the class names are fixed. The generated project builds and runs straight away; the lending code is there as a worked example to replace with your own domain.

## Keeping it in sync with the sample

The files under `src/main/resources/archetype-resources` are generated. Don't edit them by hand. After changing the sample, run:

```bash
./sync.sh
mvn install
```

`sync.sh` copies every file Git knows about in the parent directory (tracked or untracked but not ignored), except `archetype/` and `docs/`. On the way it:

- replaces `com.wixia.library` with `${package}`, `com.wixia` with `${groupId}` and `1.0-SNAPSHOT` with `${version}`, and the root artifactId with `${artifactId}` / `${rootArtifactId}` in POMs;
- moves Java sources out of `com/wixia/library/` so the archetype can re-create the package directories;
- escapes every `$` and `#` in Java and Markdown files, so Velocity leaves JSON paths and Markdown headings alone, and wraps Maven properties such as `${org.mockito:mockito-core:jar}`, which Velocity can't parse, in `#[[...]]#`;
- leaves out the parts of `README.md` and `CLAUDE.md` that point to `docs/` or `archetype/`.

If `git status` shows changes under `archetype/` after running `sync.sh`, the archetype was out of date.

`sync.sh` fails if a module of the sample isn't declared in `src/main/resources/META-INF/maven/archetype-metadata.xml`. A new module has to be added there by hand.

`mvn install` runs the integration test in `src/test/resources/projects/basic`: it generates a project with coordinates that differ from the sample's and runs `mvn verify` on it, which checks the enforcer rules and runs all tests. The generated project and its `build.log` are in `target/test-classes/projects/basic/project/`.
