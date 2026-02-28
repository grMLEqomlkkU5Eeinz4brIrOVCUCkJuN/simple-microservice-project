## Collection of notable mistakes made when learning

1) Supressing json types. When working on this project, I .stringify() the models initially as I thought it would be good for returning responses, but then I realized that there were moments I did that just to parse it again, which was silly. TLDR: keep it in JsValue and strinfigy when crossing boundaries