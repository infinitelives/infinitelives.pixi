(ns
    ^{:doc "Functions for building and manipulating the canvas DOM element"}
  infinitelives.pixi.canvas
  (:require [dommy.core :as dommy :refer-macros [sel1]]
            [PIXI]))

(defn make
  "make a new pixi canvas, or initialise pixi with an existing canvas.

  Pass in...

  :expand        if true makes the canvas take the entire window
  :engine        can be :webgl :canvas or :auto (default :auto)

  and either:

  :canvas        a DOM element to use as the canvas

  or:

  :x             x position for the new canvas
  :y             y position for the new canvas
  :width         width of new canvas
  :height        height of new canvas"
  [& {:keys [expand x y width height canvas engine]
      :or {expand false
           x 0
           y 0
           width 800
           height 600
           engine :auto}}]
  (let [fswidth (.-innerWidth js/window)
        fsheight (.-innerHeight js/window)

        ;; arguments for renderer
        wid (if expand fswidth width)
        hig (if expand fsheight height)
        opts #js {"view" canvas
                  "transparent" false
                  "antialias" false
                  "preserveDrawingBuffer" false
                  "resolution" 1
                  "clearBeforeRender" true
                  "autoResize" false
                  "imageSmoothingEnabled" false
                  }

        ;; make the renderer
        rend (case engine
               :webgl (js/PIXI.WebGLRenderer. wid hig canvas opts)
               :canvas (js/PIXI.CanvasRenderer. wid hig canvas opts)
               (js/PIXI.autoDetectRenderer. wid hig canvas opts))

        ;; details of the generated renderer
        actual-canvas (.-view rend)
        canvas-width (.-width actual-canvas)
        canvas-height (.-height actual-canvas)]

    (when-not canvas
      ;; custom canvas was generated. we should position it
      ;; and add it to the DOM
      (do
        (dommy/set-style! actual-canvas
                          :left (if expand 0 x)
                          :top (if expand 0 y)
                          :position "absolute")
        (dommy/append! (sel1 :body) actual-canvas)))

    (let [wind-width (if expand fswidth canvas-width)
          wind-height (if expand fsheight canvas-height)
          middle-x (Math/round (/ wind-width 2))
          middle-y (Math/round (/ wind-height 2))]

      (.resize rend wind-width wind-height))

    ;; return canvas and pixi renderer
    {
     :renderer rend
     :canvas (or canvas actual-canvas)
     }))
