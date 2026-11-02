# Ack: ChatGPT :-( :-)

# Sort keys numerically when possible, otherwise as strings
def sk: map(tonumber? // .);

. as [$A, $B]
| [ .[] | to_entries[] | .key as $o | .value | keys_unsorted[] | [$o, .] ]
| unique
| sort_by(sk)
| first(
    .[] | . as [$o, $i]
    | select($A[$o][$i] != $B[$o][$i])
    | {outer: $o, inner: $i, a: $A[$o][$i], b: $B[$o][$i]}
  )
// "no differences"