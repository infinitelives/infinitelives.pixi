# infinitelives.pixi
2D Game functionality that depends apon pixi.js. Uses Pixi via :foreign-libs as a first class namespace.

# Setup

Infinitelives is not ready for clojars. And so to use it, you need to install it. This involves checking the clojure source out. Then issuing `lein install` in their root.

Before installing infinitelives.pixi, you will need to install infinitelives.utils. Check out the infinitelives.utils source and `lein install`. Then repeat again with the infinitelives.pixi source. Then you are ready to use in your project.

# Starting a project

To start a standalone, client-side only game, a good basis is the figwheel template. `lein new figwheel myproj`.

To use the installed libraries, add the following to dependencies in project.clj:

```clojure
[infinitelives.pixi/infinitelives.pixi "0.1.0-SNAPSHOT"]
```

# Initialising a canvas

Near the start of your core.cljs, defonce a canvas like:

```clojure
(:require [infinitelives.pixi.canvas :as c])
```
...
```clojure
(defonce canvas
  (c/init
   {:expand true
    :engine :auto
    :layers [:bg :world :float :ui]
    :background 0xa2d000 ;0x505050
    }))
```

This should set up an autosizing canvas with the layers created and laid out like specified. However the canvas includes no rendering code, so you will have to render your canvas every frame, maybe with something like:

```
(:require [infinitelives.utils.events :as events])
```
...
```
(defonce render-thread
  (go
    (while true
      (<! (events/next-frame))
      ((:render-fn canvas)))))
```

# Loading Assets

One approach to start a game-jam game architecture is one entry go-thread that pre-loads all the assets for the game and registers them in the resources atoms.

resources/load-resources initiates the pre loading of assets. It immediately returns a channel. Two events will be sent down it. The first one is at the end of all the assets loaded. However, the bar will still be displayed. The second event will be once the loading bar has completely faded out. Pull once from the channel if you want to start as soon as possible (with the bar still fading out). Pull twice to wait for complete fade out.

```clojure
(resources/load-resources
  (-> canvas :layer :ui)    ;; which layer I want the loading bar on
    [
      "sfx/bloop.ogg"
      "sfx/bing.ogg"
      "sfx/music.ogg"
      "img/sprites.png"
      "img/backgrounds.png"
      "fonts/arial.woff2"
    ]
    :full-colour 0x807030
    :highlight 0xffff80
    :lowlight 0x303010
    :empty-colour 0x202020
    :debug-delay 0.2
    :width 400
    :height 32
    :fade-in 0.2
    :fade-out 0.5)
```

# Running Tests
```bash
$ lein cljsbuild test
```

## License

Copyright © 2015 Crispin Wellington

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
