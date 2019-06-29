(ns
    ^{:doc "Functions for building and manipulating the canvas DOM element"}
  infinitelives.pixi.canvas
  (:require [cljs.core.async :refer [<!]]
            [infinitelives.utils.events :as events]
            [infinitelives.utils.dom :as dom]
            [infinitelives.utils.console :refer [log]]
            [goog.events :as ev]
            [goog.events.EventType :as event-type]
            [goog.dom.fullscreen.EventType :as fullscreen-event-type]
            [cljsjs.pixi])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [infinitelives.pixi.canvas :refer
                    [get-layer
                     with-sprite* with-sprite
                     with-layered-sprite
                     with-sprite-set* with-sprite-set
                     while-let]]))

(def ^:dynamic *default-canvas* nil)
(defn get-default-canvas [] *default-canvas*)
(defn set-default-canvas! [canvas] (set! *default-canvas* canvas))

(def ^:dynamic *default-layer* nil)
(defn get-default-layer [] *default-layer*)
(defn set-default-layer! [layer] (set! *default-layer* layer))

(defn make
  "make a new pixi canvas, or initialise pixi with an existing canvas.

  Pass in...

  :expand        if true makes the canvas take the entire window
  :default       true or false to set the default canvas to this
                 created one. defaults to true.

  and either:

  :canvas        a DOM element to use as the canvas

  or:

  :x             x position for the new canvas
  :y             y position for the new canvas
  :width         width of new canvas
  :height        height of new canvas"
  [{:keys [expand x y width height canvas engine background default]
    :or {expand false
         x 0
         y 0
         width 800
         height 600
         background 0x500000
         engine :auto
         default true}}]
  (let [fswidth (.-innerWidth js/window)
        fsheight (.-innerHeight js/window)

        ;; arguments for renderer
        wid (if expand fswidth width)
        hig (if expand fsheight height)
        opts #js {
                  "width" wid
                  "height" hig
                  "view" canvas
                  "transparent" false
                  "antialias" false
                  "preserveDrawingBuffer" false
                  "resolution" 1
                  "clearBeforeRender" true
                  "autoResize" false
                  "imageSmoothingEnabled" false
                  "backgroundColor" background
                  }

        ;; make the renderer
        rend (js/PIXI.Renderer. opts)

        ;; details of the generated renderer
        actual-canvas (.-view rend)
        canvas-width (.-width actual-canvas)
        canvas-height (.-height actual-canvas)]

    (when-not canvas
      ;; custom canvas was generated. we should position it
      ;; and add it to the DOM
      (do
        (dom/set-style! actual-canvas
                        :left (if expand 0 x)
                        :top (if expand 0 y)
                        :position "absolute"
                        :-webkit-user-select "none"
                        :-moz-user-select "none"
                        :user-select "none"
                        )
        (dom/append! (.-body js/document) actual-canvas)))

    (let [wind-width (if expand fswidth canvas-width)
          wind-height (if expand fsheight canvas-height)
          middle-x (Math/round (/ wind-width 2))
          middle-y (Math/round (/ wind-height 2))]

      (.resize rend wind-width wind-height))

    ;; return canvas and pixi renderer
    {
     :renderer rend
     :canvas (or canvas actual-canvas)}))

(defn make-stage
  "Layout the stage structure"
  [{:keys [layers origins translate
           ]
      :or {
           layers [:backdrop :below :world :above :ui :effect]
           origins {}
           translate {}}}]

  ;(.log js/console (str layers))
  (let [
        stage (js/PIXI.Container.)
        containers (map #(js/PIXI.Container.) layers)
        ;; _ (doall (map #(.addChild stage %) containers))
        ]
    {
     :stage stage
     :origins origins
     :translate translate
     :layers layers
     :layer
     (into {}
           (for [[k v] (partition 2 (interleave layers containers))]
             [k v]))}))


(defn- center-container! [canvas layer edge [x y]]
  (let [canvas-width (.-width canvas)
        canvas-height (.-height canvas)
        middle-x (Math/round (/ canvas-width 2))
        middle-y (Math/round (/ canvas-height 2))]

    ;(log (str "w:" canvas-width " h:" canvas-height))

    (case edge
      :center
      (do
        ;; start with world centered
        (set! (.-position.x layer) (+ x middle-x))
        (set! (.-position.y layer) (+ y middle-y)))

      :top
      (do
        (set! (.-position.x layer) (+ x middle-x))
        (set! (.-position.y layer) y))

      :bottom
      (do
        (set! (.-position.x layer) (+ x middle-x))
        (set! (.-position.y layer) (+ y canvas-height)))

      :left
      (do
        (set! (.-position.x layer) x)
        (set! (.-position.y layer) (+ y middle-y)))

      :right
      (do
        (set! (.-position.x layer) (+ x canvas-width))
        (set! (.-position.y layer) (+ y middle-y)))

      :top-left
      (do
        (set! (.-position.x layer) x)
        (set! (.-position.y layer) y))

      :top-right
      (do
        (set! (.-position.x layer) (+ x canvas-width))
        (set! (.-position.y layer) y))

      :bottom-left
      (do
        (set! (.-position.x layer) x)
        (set! (.-position.y layer) (+ y canvas-height)))

      :bottom-right
      (do
        (set! (.-position.x layer) (+ x canvas-width))
        (set! (.-position.y layer) (+ y canvas-height)))

      ;;default
      (do
        ;; default layer is centered
        (set! (.-position.x layer) (+ x middle-x))
        (set! (.-position.y layer) (+ y middle-y))))))

(defn init
  "Initialise the canvas element. Pass in optional keys

  :background    background colour (default 0x000000)
  :expand        if true makes the canvas take the entire window
  :engine        can be :webgl :canvas or :auto (default :auto)
  :layers        A list of keywords to refer to layers, from bottom to top
  :origins       A mapping of layer names to their origin positions. Default
                 position is center. Positions can be :center :top :bottom
                 :left :right :top-left :top-right :bottom-left :bottom-right
  :default       if true this becomes the default canvas
  :default-layer specify a default layer. If unspecified, is the top layer

  and either:

  :canvas        a DOM element to use as the canvas

  or:

  :x             x position for the new canvas
  :y             y position for the new canvas
  :width         width of new canvas
  :height        height of new canvas
  "
  [opts]
  (let [{:keys [renderer canvas layer layers stage
                origins translate fullscreen-button default
                default-layer] :or {origins {}
                                    translate {}
                                    default true} :as world}
        (into (make opts)
              (make-stage opts))]
    ;; add the stages to the canvas
    (doall
     (map
      (fn [[name layer-obj]]
                                        ;(log "adding to:" (str stage) " layer:" (str layer-obj))
        (.addChild stage layer-obj)
        (center-container! canvas layer-obj
                           (or (origins name) :center)
                           (or (translate name) [0 0])))
      layer))

    ;; do the first render
                                        ;(doall (for [l layers] (.render renderer (l layer))))
    (.render renderer stage)

    (let [
          render-fn #(.render renderer stage)
          resize-fn (fn [width height]
                      (.resize renderer width height)
                      (doall (map (fn [[name layer-obj]]
                                    (center-container! canvas layer-obj
                                                       (or (origins name) :center)
                                                       (or (translate name) [0 0])))
                                  layer)))
          expand-fn (fn [] (resize-fn (.-innerWidth js/window)
                                      (.-innerHeight js/window)))

          ;; remember: fullscreen call will only work when
          ;; its called from a gesture. Like an on-click.
          fullscreen-fn (fn [fullscreen]
                          (js/console.log "FULLSCREEN" fullscreen)
                          (if fullscreen
                            (cond
                              (.-requestFullscreen (.-body js/document))
                              (.requestFullscreen (.-body js/document))

                              (.-webkitRequestFullscreen (.-body js/document))
                              (.webkitRequestFullscreen (.-body js/document))

                              (.-mozRequestFullScreen (.-body js/document))
                              (.mozRequestFullScreen (.-body js/document)))

                            (.webkitCancelFullScreen js/document)
                            #_ (cond
                              (.-exitFullscreen js/document)
                              (.exitFullscreen js/document)

                              (.-webkitCancelFullscreen js/document)
                              (.webkitCancelFullscreen js/document)

                              (.-msExitFullscreen js/document)
                              (.msExitFullscreen js/document)

                              (.-mozCancelFullScreen js/document)
                              (.mozCancelFullScreen js/document))))

          resizer-loop
          (when (:expand opts) (let [c (events/new-resize-chan)]
                                 (go (while true
                                       (let [[width height] (<! c)]
                                         (resize-fn width height)
                                         (render-fn))))))]

      ;; setup the render loop
      (defn render []
        (events/request-animation-frame render)
        (render-fn))

      (render)

      ;(fullscreen-fn)

      (let [canvas (into
                    world
                    {
                     :render-fn render-fn
                     :resize-fn resize-fn
                     :fullscreen-fn fullscreen-fn
                     :expand-fn expand-fn})]
        (when default
          (set-default-canvas! canvas))

        (if default-layer
          (set-default-layer! default-layer)
          (set-default-layer! (last layers)))

        canvas))))

(defn add-fullscreen-button!
  [{:keys [fullscreen-fn]} &
   {:keys [expand-image
           contract-image
           border
           corner
           fullscreen-callback]
    :or {expand-image "http://runrunitshim.com/images/fullscreenIcon.png"
         border [0 0]
         corner :bottom-right
         }}]

  (let [div (dom/create-element :div)
        img (dom/create-element :img)
        [x y] border]
    (dom/append! div img)
    (set! (.-src img) expand-image)
    (let [style (case corner
                  :bottom-left
                  (str "bottom: 0; position: absolute; padding-bottom: "
                       y "px; padding-left: " x
                       "px; z-index: 200; user-select: none;")

                  :bottom-right
                  (str "bottom: 0; right: 0; position: absolute; padding-bottom: "
                       y "px; padding-right: " x
                       "px; z-index: 200;"
                       "user-drag: none; user-select: none; -moz-user-select: none;"
                       "-webkit-user-drag: none; -webkit-user-select: none; -ms-user-select: none;")
                  )]
      (.setAttribute img "style" style)
      (.setAttribute div "style" style)
      )
    (.addEventListener img "click"
                       #(do
                          (js/console.log "FSCH"  (.-webkitIsFullScreen js/document)
                                          (.-mozFullScreen js/document)
                                          (.-msFullscreenElement js/document))
                          (if (not (or (.-webkitIsFullScreen js/document)
                                       (.-mozFullScreen js/document)
                                       (.-msFullscreenElement js/document)))
                            (do
                              (js/console.log "EXPAND")
                              (fullscreen-fn true)
                              (set! (.-src img) contract-image))

                            (do
                              (js/console.log "CONTRACT")
                              (fullscreen-fn false)
                              (set! (.-src img) expand-image))
                            )

                          (when fullscreen-callback
                            (fullscreen-callback))

                          (.stopPropagation %)
                          ))
    (ev/listen js/window fullscreen-event-type/CHANGE
               (fn [ev]
                 (if  (or (.-webkitIsFullScreen js/document)
                          (.-mozFullScreen js/document)
                          (.-msFullscreenElement js/document))
                   (set! (.-src img) contract-image)
                   (set! (.-src img) expand-image)

                   ))
               )

    (dom/append! (.-body js/document) div)
    div))
