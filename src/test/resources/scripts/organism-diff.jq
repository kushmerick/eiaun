# Reduce each entry to just its id, e.g. {"k":"M","j":"{\"id\":40582,...}"} -> 40582
# If the entry has no parseable j/id, it becomes the marker string "<no id>".
def ids:
  map_values(map_values(
    try (.j | fromjson | .id) catch "<no id>"
  ));

# Sort keys numerically when possible, otherwise as strings
def sk: map(tonumber? // .);

(map(ids)) as [$A, $B]
| [ ($A, $B) | to_entries[] | .key as $o | .value | keys_unsorted[] | [$o, .] ]
| unique
| sort_by(sk)
| first(
    .[] | . as [$o, $i]
    | select($A[$o][$i] != $B[$o][$i])
    | {outer: $o, inner: $i, a: $A[$o][$i], b: $B[$o][$i]}
  )
// "no differences"