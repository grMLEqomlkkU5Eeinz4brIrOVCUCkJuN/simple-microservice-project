## Collection of notable mistakes made when learning

1) Supressing json types. When working on this project, I .stringify() the models initially as I thought it would be good for returning responses, but then I realized that there were moments I did that just to parse it again, which was silly. TLDR: keep it in JsValue and strinfigy when crossing boundaries

## Things I am unclear about
1) Either
2) Null, null and Options, I know what each one of them are on their own, the first is the type, the second is the literal value and the last is the container for the literal value, just a little confusing knowing when to use which
3) .fold and match (a little confused and actually have no idea how it works, ended up using .fold from IDE recommendation)
4) trait sealing, kinda like rust, but i think i need more understanding of uses: https://www.handsonscala.com/chapter-5-notable-scala-features.html#:~:text=Normal%20trait%20s%20are%20open,:%20Int%20=%203%205.7.scala
5) WHAT IS EITHER