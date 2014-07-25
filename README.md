# Textik [![Travis CI status](https://secure.travis-ci.org/astashov/tixi.png)](http://travis-ci.org/#!/astashov/tixi/builds)

Web based ASCII diagrams editor, written in ClojureScript.

Basically, the main goal of this project initially was to try out ClojureScript, React, and the approach of
having one global state for the whole relatively complex app and having fully synchronous flow instead of
reacting to changes via observers.

As a side effect, it appears to be nice and hopefully useful tool. :)

## Features

* You can draw rectangles and lines (items) on the canvas, and write text
* You can write text inside the items
* You can select items, move them around, resize
* You can "lock" lines to rectangles, so if you move/resize rectangle, the line will be moved too
* You can add arrows to the lines
* There is undo and redo.
* There is copy and paste (with regular Cmd+C/Ctrl+C and Cmd+V/Ctrl+V)
* You can copy the final result to the clipboard, and paste it somewhere in email/docs/comments/etc.

## Libraries and Frameworks

It is written in
[ClojureScript](https://github.com/clojure/clojurescript), using
[ReactJS](http://facebook.github.io/react/) as a view layer,
[Quiescent](https://github.com/levand/quiescent) as a thin functional wrapper around React,
[CodeMirror](http://codemirror.net/) as a text editor, and a bunch of other dependencies you can find in
[project.clj](https://github.com/astashov/tixi/blob/master/project.clj)

## Architecture

All starts from `tixi.core`, where we do initial assigning of auxiliary event listeners (like, keypress
events or resize events), create a core.async channel listener, where forward all the messages from the channel
to `tixi.dispatcher`. And then - it does initial rendering of the view via `tixi.view/render`, passing our
global state into it, which is just one large atom value.

In `tixi.view`, we assign different event handlers via React's event system, and when these events
happen, we create a appropriate payload and send it to `core.async` channel, which then will be dispatched
to `tixi.dispatcher`.

`tixi.dispatcher` tries to figure out what's really going on (e.g., if this is just mouse move or mouse drag),
and calls an appropriate function in `tixi.controller`. After that, `tixi.controller` depending on the
received payload calls various `tixi.mutators`, which change the global state (our atom), and then 
`tixi.controller` rerenders the app via `tixi.view/render` again, passing the new value of the atom to it
(from `tixi.data`)

Like this:

```
    +----------------+      +-------------------+      +-------------------+ 
    |                |      |                   |      |                   | 
    |   tixi.core    +----->+     tixi.view     +----->+  tixi.dispatcher  | 
    |                |      |                   |      |                   | 
    +----------------+      +---------+---------+      +---------+---------+ 
                                      ^                          |           
                                      |                          v           
                                      |                +---------+---------+ 
                                      +----------------+                   | 
                                                       |  tixi.controller  | 
                                      +--------------->+                   | 
                                      |                +---------+---------+ 
                                      |                          |           
                                      |                          v           
                            +---------+---------+      +---------+---------+ 
                            |                   |      |                   | 
                            |     tixi.data     +<-----+   tixi.mutators   | 
                            |                   |      |                   | 
                            +-------------------+      +-------------------+ 
    
```

And that's it. Very simple, no observers at all, very clear flow.

Pros:

* Simple architecture - less bugs, easier to track them, we can easily restore the whole state of the app
  just by assigning one value to `tixi.data/data`.
* Easy to add new features - you almost always just add new `tixi.mutators`, which do something new with
  the data.
* Clear separate of responsibilities of the namespaces
* It's easy to test business logic
* There is basically only one place where the data is being changed - in `tixi.mutators`. All other
  functions are pure.
* Purely React thing, but so cool - React tracks and cleans up all the event handlers you create there.
  It is a huge deal, from my experience there is always a lot of hard-tracking errors and memory leaks when
  you somehow forget to remove event handlers.

Cons:

* It's a bit harder to make REALLY reusable widgets - they should know something about the global data
  structure to keep their data there. Not a big deal for this app though.

## Data and rendering

The application data is kind of split to 3 parts - canvas state (with undo/redo stack), cache and all other stuff.
The canvas state contains only the data, which describe the data on the canvas in the shortest way.

For example, if we have a line and a rectangle on the canvas, we will describe them as:

```clj
{:completed {0 {:input R[1 1][5 5], :type :rect, :z 0}
             1 {:input R[10 10][20 20], :type :line, :z 0}}}
```

Which means the rectangle will be with the coordinates - left-top corner is (1,1), and right-bottom corner is (5,5),
and the line will be with coordinates (10, 10) and (20, 20).

We use that information to build :cache - a set of points and text, which we are going to show on the screen.
The code for that is in `tixi.items` and `resources/tixi/js/drawer.js`. I had to write generating points and ASCII text
in JavaScript, because it shows significantly better performance, and this is the only part of the app where this
performance is crucial for smooth UX.

After that, we end up with the list of points, which we could use to track "locks", "hits" (to track when mouse is
over some character), and also with the ASCII text we are going to render on the screen.

## Undo / Redo stack

Undo/Redo stack is a tree. So, it actually could support "Undo Tree", like in Vim or Emacs, when you don't lose
your history even if you accidentally Undoed something, and then added a new change.

So far, it is not implemented in UI though, in UI it is "flat" for now, so I always select the rightmost branch
of the tree when do "Redo".

## Running on your machine

Like any other ClojureScript app:

```bash
$ lein cljsbuild auto dev
```

Will compile a JS file, the 'dev' build. Then you'll need to run some server, from the repo root, e.g. like this:

```bash
$ python -m SimpleHTTPServer 8000
```

Now, open http://localhost:8000/index_dev.html in your browser, and you should be able to see it working.

There is also brepl running on 9000 port in dev build, so you could connect to it from REPL.

## Contributing

If you want to help to the project, and fix some bug or add some feature - feel free to create a pull request,
that's the whole point of the open source software, right? :)

If you found a bug, and want to create a ticket - please do that in [Github Issues](https://github.com/astashov/tixi/issues).

If you just want to help and add some code, but have no idea what to work on, there is
[TODO](https://github.com/astashov/tixi/blob/master/TODO), you can get something from there.

Right now the UI of the app looks awful, and some designer's help would be very appreciated.
If you want to help with the design, that would be very appreciated.
