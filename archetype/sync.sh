#!/usr/bin/env bash
# Regenerates src/main/resources/archetype-resources from the sample project in the parent directory.
# Run it after changing the sample, then `mvn install` here to build the archetype and test it.
set -euo pipefail

archetype_dir=$(cd "$(dirname "$0")" && pwd)
root=$(dirname "$archetype_dir")
out=$archetype_dir/src/main/resources/archetype-resources
metadata=$archetype_dir/src/main/resources/META-INF/maven/archetype-metadata.xml

# Velocity can't parse Maven properties like ${org.mockito:mockito-core:jar}; #[[...]]# passes them through as they are.
unparsed_pom_properties='s/\${[^}]*:[^}]*}/#[[&]]#/g'

# Turns the sample's coordinates into archetype parameters. com.wixia.library must go before com.wixia.
parameterise='s/com\.wixia\.library/${package}/g; s/com\.wixia/${groupId}/g; s/1\.0-SNAPSHOT/${version}/g'

# Every '$' and '#' in a filtered text file is escaped first, so Velocity leaves Markdown headings,
# JSON paths etc. alone; the parameters are inserted afterwards.
filter_text() {
    if grep -q '[$#]' "$1"; then
        printf '%s\n' "#set( \$symbol_pound = '#' )" "#set( \$symbol_dollar = '\$' )"
        sed -e 's/\$/${symbol_dollar}/g' -e 's/#/${symbol_pound}/g' "$1"
    else
        cat "$1"
    fi | sed -e "$parameterise" -e "${2:-}"
}

cd "$root"

# Every module of the sample must be declared in the descriptor, or its files would silently be left out.
missing=0
for pom in $(git ls-files --cached --others --exclude-standard -- '*/pom.xml' ':!archetype'); do
    module=$(basename "$(dirname "$pom")")
    if ! grep -q "dir=\"$module\"" "$metadata"; then
        echo "Module $module is not declared in $metadata" >&2
        missing=1
    fi
done
[ "$missing" -eq 0 ] || exit 1

rm -rf "$out"
for file in $(git ls-files --cached --others --exclude-standard -- ':!archetype' ':!docs'); do
    [ -f "$file" ] || continue    # deleted but not yet staged
    # Packaged sources: the archetype re-creates the package directories from the package parameter.
    target=$(sed -E 's#^(.*/src/(main|test)/java)/com/wixia/library/#\1/#' <<< "$file")
    # The archetype jar drops .gitignore files; __gitignore__ is renamed back via the gitignore property.
    target=${target/%.gitignore/__gitignore__}
    mkdir -p "$out/$(dirname "$target")"
    case "$file" in
        pom.xml)
            sed -e "$unparsed_pom_properties" -e "$parameterise" -e 's/hexagonal-architecture/${artifactId}/g' "$file" ;;
        */pom.xml)
            sed -e "$unparsed_pom_properties" -e "$parameterise" -e 's/hexagonal-architecture/${rootArtifactId}/g' "$file" ;;
        *.java)
            filter_text "$file" ;;
        README.md)
            # docs/ and archetype/ aren't part of the archetype.
            filter_text "$file" '/docs\/dev-log\.md\|archetype\//d' ;;
        CLAUDE.md)
            # The Archetype and History sections come last and are about docs/ and archetype/.
            filter_text "$file" '/^${symbol_pound}${symbol_pound} Archetype$/,$d' | sed '${/^$/d}' ;;
        *)
            cat "$file" ;;
    esac > "$out/$target"
done

echo "Generated $out"
