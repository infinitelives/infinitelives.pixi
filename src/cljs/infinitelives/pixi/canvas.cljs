(ns
    ^{:doc "Functions for building and manipulating the canvas DOM element"}
  infinitelives.pixi.canvas
  (:require [cljs.core.async :refer [<!]]
            [infinitelives.utils.events :as events]
            [infinitelives.utils.dom :as dom]
            [infinitelives.utils.console :refer [log]]
            [cljsjs.pixi])

  (:require-macros [cljs.core.async.macros :refer [go]])
)

(def canvas-store (atom {}))

(defn add!
  ([key canvas]
   (swap! canvas-store assoc key canvas))
  ([canvas]
   (add! :default canvas)))

(defn set-default-once!
  [canvas]
  (when-not (:default @canvas-store)
    (add! canvas)))

(defn remove!
  ([key]
   (swap! canvas-store dissoc key)))

(defn get
  ([key]
   (key @canvas-store))
  ([]
   (:default @canvas-store)))

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
  [{:keys [expand x y width height canvas engine background]
    :or {expand false
         x 0
         y 0
         width 800
         height 600
         background 0x500000
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
                  "backgroundColor" background
                  }

        ;; make the renderer
        rend (case engine
               :webgl (js/PIXI.WebGLRenderer. wid hig opts)
               :canvas (js/PIXI.CanvasRenderer. wid hig opts)
               (js/PIXI.autoDetectRenderer. wid hig opts))

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
                        :position "absolute")
        (dom/append! (.-body js/document) actual-canvas)))

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

(defn make-stage
  "Layout the stage structure"
  [{:keys [layers origins
           ]
      :or {
           layers [:backdrop :below :world :above :ui :effect]
           origins {}}}]

  ;(.log js/console (str layers))
  (let [
        stage (js/PIXI.Container.)
        containers (map #(js/PIXI.Container.) layers)
        ;; _ (doall (map #(.addChild stage %) containers))
        ]
    {
     :stage stage
     :origins origins
     :layers layers
     :layer
     (into {}
           (for [[k v] (partition 2 (interleave layers containers))]
             [k v]))}))


(defn- center-container! [canvas layer edge]
  (let [canvas-width (.-width canvas)
        canvas-height (.-height canvas)
        middle-x (Math/round (/ canvas-width 2))
        middle-y (Math/round (/ canvas-height 2))]

    ;(log (str "w:" canvas-width " h:" canvas-height))

    (case edge
      :center
      (do
        ;; start with world centered
        (set! (.-position.x layer) middle-x)
        (set! (.-position.y layer) middle-y))

      :top
      (do
        (set! (.-position.x layer) middle-x)
        (set! (.-position.y layer) 0))

      :bottom
      (do
        (set! (.-position.x layer) middle-x)
        (set! (.-position.y layer) canvas-height))

      :left
      (do
        (set! (.-position.x layer) 0)
        (set! (.-position.y layer) middle-y))

      :right
      (do
        (set! (.-position.x layer) canvas-width)
        (set! (.-position.y layer) middle-y))

      :top-left
      (do
        (set! (.-position.x layer) 0)
        (set! (.-position.y layer) 0))

      :top-right
      (do
        (set! (.-position.x layer) canvas-width)
        (set! (.-position.y layer) 0))

      :bottom-left
      (do
        (set! (.-position.x layer) 0)
        (set! (.-position.y layer) canvas-height))

      :bottom-right
      (do
        (set! (.-position.x layer) canvas-width)
        (set! (.-position.y layer) canvas-height))

      ;;default
      (do
        ;; default layer is centered
        (set! (.-position.x layer) middle-x)
        (set! (.-position.y layer) middle-y)))))


(defn init
  "Initialise the canvas element. Pass in optional keys

  :background    background colour (default 0x000000)
  :expand        if true makes the canvas take the entire window
  :engine        can be :webgl :canvas or :auto (default :auto)
  :layers        A list of keywords to refer to layers, from bottom to top
  :origins       A mapping of layer names to their origin positions. Default
                 position is center. Positions can be :center :top :bottom
                 :left :right :top-left :top-right :bottom-left :bottom-right

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
                origins fullscreen-button] :or {origins {}} :as world}
        (into (make opts)
              (make-stage opts))]
    ;; add the stages to the canvas
    (doall
     (map
      (fn [[name layer-obj]]
                                        ;(log "adding to:" (str stage) " layer:" (str layer-obj))
        (.addChild stage layer-obj)
        (center-container! canvas layer-obj (or (origins name) :center)))
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
                                                       (or (origins name) :center)))
                                  layer)))
          expand-fn (fn [] (resize-fn (.-innerWidth js/window)
                                      (.-innerHeight js/window)))

          ;; remember: fullscreen call will only work when
          ;; its called from a gesture. Like an on-click.
          fullscreen-fn (fn [fullscreen]
                          (if fullscreen
                            (cond
                              (.-requestFullscreen canvas)
                              (.requestFullscreen canvas)

                              (.-webkitRequestFullscreen canvas)
                              (.webkitRequestFullscreen canvas)

                              (.-mozRequestFullScreen canvas)
                              (.mozRequestFullScreen canvas))

                            (cond
                              (.-exitFullscreen js/document)
                              (.exitFullscreen js/document)

                              (.-webkitExitFullscreen js/document)
                              (.webkitExitFullscreen js/document)

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

      (fullscreen-fn)

      (let [canvas (into
                    world
                    {
                     :render-fn render-fn
                     :resize-fn resize-fn
                     :fullscreen-fn fullscreen-fn
                     :expand-fn expand-fn})]
        (set-default-once! canvas)
        canvas))))

(defn add-fullscreen-button! [{:keys [fullscreen-fn]}]
  (let [div (dom/create-element :div)
        img (dom/create-element :img)]
    (dom/append! div img)
    (set! (.-src img) "http://runrunitshim.com/images/fullscreenIcon.png")
    (.setAttribute img "style" "bottom: 0; position: absolute; padding-bottom: 0px; padding-left: 0px; z-index: 200;")
    (.setAttribute div "style" "bottom: 0; position: absolute; padding-bottom: 0px; padding-left: 0px; z-index: 200;")
    (.addEventListener img "click" #(fullscreen-fn true))
    (dom/append! (.-body js/document) div)
    div
    ))
