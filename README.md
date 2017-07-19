# infinitelives.pixi

[![Clojars Project](https://img.shields.io/clojars/v/infinitelives/infinitelives.pixi.svg)](https://clojars.org/infinitelives/infinitelives.pixi)

A 2D ClojureScript Game Engine. For building webgames. It's not really an engine, it's just a library. Just a bunch of 2D Game functionality that depends apon pixi.js. Pixi.js comes bundled (via cljsjs dependency package).

# Complete Example Games

https://github.com/retrogradeorbit/moonhenge (entry for Global Game Jam 2016)

https://github.com/retrogradeorbit/biscuit-switch (entry for Ludum Dare 35)

## Setup

To start a standalone, client-side only game, a good basis is the figwheel template. `lein new figwheel myproj`.

Add the following to the dependencies section of your `project.clj` file:

```clojure
[infinitelives/infinitelives.pixi "0.1.0"]
```

## Initialising a canvas

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

This should set up an autosizing canvas with the layers created and laid out like specified.

## Loading Assets

One approach to start a game-jam game architecture is one entry go-thread that pre-loads all the assets for the game and registers them in the resources atoms.

resources/load-resources initiates the pre loading of assets. It immediately returns a channel. Pull once from the channel to wait for complete fade out of the loading bar.

```clojure
(resources/load-resources
    canvas :ui          ;; which layer I want the loading bar on
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

## Registering Textures or SubTextures

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

## Adding Sprites

Now you can use these registered textures in sprites:

```clojure

(canvas/with-sprite canvas :world    ;; the canvas and layer the sprite should be on
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

## Pixel Fonts

You can use pixel fonts by rendering all the characters you want to
use into a spritesheet and then building a font from that section with
the `pixel-font` macro.

The path to the sprite sheet is specified as a file path relative
to `resources/public` as the macro analyses the image at compile
time and calculates all the glyph positions. The client side operation
uses this font definition to find it's glyphs.

Create a pixel font with:

```clojure
(pf/pixel-font :big "img/fonts.png" [127 84] [500 128]
               :chars ["ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                       "abcdefghijklmnopqrstuvwxyz"
                       "0123456789!?#`'.,"]
               :kerning {"fo" -2  "ro" -1 "la" -1 }
               :space 5)
```

The font is given the font-name `:big`, and is built from the img/fonts.png
spritesheet from position (127, 84) to position (500, 128). The characters
are laid out as shows in the :chars field. This can either be a single string,
or a vector. This is just to improve readability, allowing you to put line
breaks in the character definitions source. Kerning optionally gives a list
of character pairs that need their distance adjusted. Space is how many pixels
to jump for a space character (or any character not found in the font).

You make text from a font just like a sprite:

```clojure
(c/with-sprite canvas :bg
   [text (pf/make-text :big "The quick brown fox jumped over the lazy sequence!"
                       :tint 0xb0c0ff
                       :scale 3
                       :rotation 0)]
   ... code here ...
)
```

## Complete example

Here is the first intro example to pixi.js:

https://pixijs.github.io/examples/index.html?s=basics&f=basic.js&title=Basics

Here is a complete port of the spinning bunny from the intro example to pixi.js for comparison.

```clojure
(ns basic.core
    (:require [infinitelives.pixi.canvas :as c]
              [infinitelives.pixi.events :as e]
              [infinitelives.pixi.resources :as r]
              [infinitelives.pixi.texture :as t]
              [infinitelives.pixi.sprite :as s]
              [cljs.core.async :refer [<!]])
    (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce canvas
  (c/init {:layers [:bg] :background 0x1099bb}))

(defonce main-thread
  (go
    (<! (r/load-resources canvas :bg ["img/bunny.png"]))

    (t/set-texture! :rabbit (r/get-texture :bunny :nearest))

    (c/with-sprite canvas :bg
      [rabbit (s/make-sprite :rabbit)]
      (loop [angle 0]
        (s/set-rotation! rabbit angle)
        (<! (e/next-frame))
        (recur (+ 0.1 angle))))))
```

Compare and contrast the two approaches.

Even with the large namespace declaration, it's about the same length as the pixi.js one. But we also get an asset loader bar, and a csp based system for controlling state that isn't mutatey, unlike the event driven JS version. That is, the angle of the bunny is completely dependent on the value of `angle` within the `loop`/`recur` block, and not dependent in any way on the _existing_ angle of rotation of the sprite.

This example is in the `examples/basic` folder. After installing infinitelives.pixi, go into that directory and issue a `lein figwheel`. Then when the server has started, point your browser at http://localhost:3449/

## You're on your own

So that should get you started. Read the source and the doc strings for more. Or use doc on the repl to query the docstrings.

Most recent full game:

https://github.com/retrogradeorbit/moonhenge

For other, older examples, look at:

https://github.com/retrogradeorbit/ld34
https://github.com/retrogradeorbit/splash
https://github.com/retrogradeorbit/ludumdare33

Older examples using older versions of the library may not run, but will give you hints as to how it works.

## Running Tests
```bash
$ lein cljsbuild test
```

## License

Copyright © 2015 - 2016 Crispin Wellington

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
