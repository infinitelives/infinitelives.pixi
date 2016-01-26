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
    :background 0x505050
    }))
```

This should set up an autosizing canvas with the layers created and laid out like specified. However the canvas includes no rendering code, so you will have to render your canvas every frame, maybe with something like:

```clojure
(:require [infinitelives.utils.events :as events])
```
...
```clojure
(defonce render-thread
  (go
    (while true
      (<! (events/next-frame))
      ((:render-fn canvas)))))
```

# Loading Assets

One approach to start a game-jam game architecture is one entry go-thread that pre-loads all the assets for the game and registers them in the resources atoms.

resources/load-resources initiates the pre loading of assets. It immediately returns a channel. Pull once from the channel to wait for complete fade out of the loading bar.

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

# Registering Textures or SubTextures

Often, graphics assets are comprised of many individual image frames laid out on a larger `spritesheet`. You can register any region of any loaded image asset as a tagged texture. You specify the tag as a keyword. For example:

```clojure
(:require [infinitelives.pixi.texture :as texture]
	  [infinitelives.pixi.resources :as resources])

(texture/load-sprite-sheet!
  (resources/get-texture :sprites :nearest)   ;; :sprites refers to the filename sprites.png.
                                              ;; as the loader loads them, it registers them
                                              ;; with their filename's base as a keyword.
  {:player-standing {:pos [0 0] :size [16 16]}})
```

You can load many assets by defining an asset data structure:

```clojure
(def sprites-assets
  {:ground-1
   {:pos [0 0]
    :size [32 24]}
   :ground-2
   {:pos [32 0]
    :size [16 16]}
   :grass-1
   {:pos [48 0]
    :size [16 16]}
   :grass-2
   {:pos [64 0]
    :size [16 16]}
   :tree-1
   {:pos [31 56]
    :size [9 16]}
   :tree-2
   {:pos [48 56]
    :size [8 16]}})

(texture/load-sprite-sheet!
  (resource/get-texture :sprites :nearest)
  sprites-assets)
```

# Adding Sprites

Now you can use these registered textures in sprites:

```clojure

(macros/with-sprite canvas :world    ;; the canvas and layer the sprite should be on
  ;; now name -> sprite binding pairs in a vector
  [ground (sprite/make-sprite
  	    :ground-1
	    :scale 3
	    :xhandle 0.5 :yhandle 0.5
	    :alpha 1.0)
   tree (sprite/make-sprite
   	  :tree-2
	  :x 100 :y 100
	  :y-handle 1.0)]
    ;; inside the scope of the macro, the sprite is on the layer
    ;; apon exit from the scope, the sprite is removed and disappears
    ;; So, loop forever, flipping the frames of the grass and tree
    (loop [frame 0]
      (<! (events/next-frame!))
      (sprite/set-texture! ground (if (> (mod frame 60) 30)
                                    :ground-1 :ground-2))
      (sprite/set-texture! tree (if (> (mod frame 200) 100)
                                    :tree-1 :tree-2))
      (recur (inc frame))))
```

# Complete example

Here is a complete example of the spinning bunny from the intro example to pixi.js for comparison:

```clojure
(ns demo.core
    (:require [infinitelives.pixi.canvas :as c]
              [infinitelives.pixi.events :as e]
              [infinitelives.pixi.resource :as r]
              [infinitelives.pixi.texture :as t]
              [infinitelives.pixi.sprite :as s]
              [cljs.core.async :refer [<!]])
    (:require-macros [cljs.core.async.macros :refer [go]]
                     [infinitelives.pixi.macros :as m]))

(defonce canvas
  (c/init {:background 0x1099bb}))

(defonce render-thread
  (go
    (while true
      (<! (e/next-frame))
      ((:render-fn canvas)))))

(defonce main-thread
  (go
    (<!
      (r/load-resources
  	(-> canvas :layer :bg)
      	["https://pixijs.github.io/examples/_assets/basics/bunny.png"]))

    (t/load-sprite-sheet!
      (r/get-texture :bunny :nearest)
      {:rabbit {:pos [0 0] :size [16 16]}})

    (m/with-sprite canvas :bg
      [rabbit (s/make-sprite :rabbit)]
      (loop [angle 0]
        (s/set-rotation! rabbit angle)
        (<! (e/next-frame))
        (recur (+ 0.1 angle)))))
```

Compare and contrast with the original here:

https://pixijs.github.io/examples/index.html?s=basics&f=basic.js&title=Basics

# You're on your own

So that should get you started. Read the source and the doc strings for more. Or use doc on the repl to query the docstrings.

For examples, look at:

https://github.com/retrogradeorbit/ld34

Older examples using older versions of the library (may not run, but will give you good ideas):

https://github.com/retrogradeorbit/splash
https://github.com/retrogradeorbit/ludumdare33

# Running Tests
```bash
$ lein cljsbuild test
```

## License

Copyright © 2015 - 2016 Crispin Wellington

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
