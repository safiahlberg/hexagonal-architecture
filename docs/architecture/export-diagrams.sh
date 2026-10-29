#!/usr/bin/env bash
# Exports the views in workspace.dsl as Mermaid and writes them into README.md, between the
# <!-- diagram:VIEW --> and <!-- /diagram:VIEW --> markers. Run it after changing the model.
# Needs Docker or Podman; the export runs in the structurizr/structurizr image.
set -euo pipefail

dir=$(cd "$(dirname "$0")" && pwd)
readme=$dir/README.md
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
cp "$dir/workspace.dsl" "$out/"

# The container must be able to read and write the temporary folder as the calling user. Rootless Podman
# maps that user to the image's own user (65532); Docker runs the container as the calling user.
if docker --version 2>/dev/null | grep -qi podman; then
    user=(--userns=keep-id:uid=65532,gid=65532)
else
    user=(--user "$(id -u):$(id -g)")
fi
docker run --rm "${user[@]}" -v "$out:/usr/local/structurizr" structurizr/structurizr \
    export -w /usr/local/structurizr/workspace.dsl -f mermaid -o /usr/local/structurizr/mermaid >/dev/null

# GitHub sanitises Mermaid labels, so the exporter's styled <div>s become plain <b>, <small> and <br/>.
# The white frame around each diagram goes too: it hides the arrows in GitHub's dark theme, and the
# Markdown heading already names the view.
for_github() {
    sed -e "s#<div style='font-weight: bold'>\([^<]*\)</div>#<b>\1</b>#g" \
        -e "s#<div style='font-size: 70%; margin-top: 0px'>\([^<]*\)</div>#<br/><small>\1</small>#g" \
        -e "s#<div style='font-size: 80%; margin-top:10px'>#<br/>#g" \
        -e "s#<div style='font-size: 70%'></div>##g" \
        -e "s#<div style='font-size: 70%'>\([^<]*\)</div>#<br/><small>\1</small>#g" \
        -e "s#</\?div>##g" \
        -e 's#fill:\#ffffff,#fill:none,#g' \
        -e '/^  linkStyle default /d' \
        -e '/^  subgraph diagram /d' \
        -e '/^    style diagram /d' \
        -e '/^  end$/d' \
        "$1" | cat -s
}

for mmd in "$out"/mermaid/structurizr-*.mmd; do
    view=$(basename "$mmd" .mmd)
    view=${view#structurizr-}
    if ! grep -q "^<!-- diagram:$view -->$" "$readme"; then
        echo "No <!-- diagram:$view --> marker in $readme" >&2
        exit 1
    fi
    for_github "$mmd" > "$out/$view.mmd"
    awk -v start="<!-- diagram:$view -->" -v stop="<!-- /diagram:$view -->" -v diagram="$out/$view.mmd" '
        $0 == start { print; print "```mermaid"; while ((getline line < diagram) > 0) print line; print "```"; skip = 1; next }
        $0 == stop  { skip = 0 }
        !skip
    ' "$readme" > "$out/README.md"
    cp "$out/README.md" "$readme"
done

echo "Updated $readme"
