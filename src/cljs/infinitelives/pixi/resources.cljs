(ns infinitelives.pixi.resources
  (:require [cljsjs.pixi]
            [infinitelives.pixi.sprite :as sprite]
            [infinitelives.utils.string :as string]
            [infinitelives.utils.sound :as sound]
            [infinitelives.utils.events :as events]
            [infinitelives.utils.resources :as resources]
            [infinitelives.utils.console :refer [log]]
            [cljs.core.async :refer [chan put! <! >! timeout close!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; where we store all the loaded full textures keyed by name
;; The textures stored here are the full loaded image. Sub textures
;; as used in sprites are stored in the texture/texture-store atom.
;; This resources/texture-store atom stores the raw resource images.
;; each value is a hashmap with keps :linear and :nearest containing
;; textures set to those filtering modes
(defonce !texture-store (atom {}))

(defn progress-texture
  "Draws an empty box that can serve as a default progress bar for preloading images"
  [fraction {:keys [empty-colour full-colour
                    border-colour border-width draw-border
                    width height
                    highlight highlight-offset highlight-width
                    lowlight lowlight-offset lowlight-width]
             :or {empty-colour 0x000000
                  full-colour 0x808080
                  border-colour 0xffffff
                  border-width 2
                  draw-border false
                  width 600
                  height 40
                  highlight-offset 0
                  highlight-width 1
                  lowlight-offset 0
                  lowlight-width 1
                  }
             :as options}]
  (let [box (js/PIXI.Graphics.)]
    (doto box
      (.beginFill empty-colour)
      (.lineStyle 0 border-colour)
      (.drawRect 0 0 width height)
      (.lineStyle 0 border-colour)
      (.beginFill full-colour)
      (.drawRect border-width border-width (* (if (< fraction 1) fraction 1) (- width border-width border-width)) (- height border-width border-width ))
      .endFill)

    (let [bw (* (if (< fraction 1) fraction 1) width)
          x1 (+ border-width highlight-offset)
          x2 (- bw highlight-offset)
          y1 (+ border-width lowlight-offset)
          y2 (- height border-width lowlight-offset)]
      (when (> bw 0)
        (when highlight
          (doto box
              (.lineStyle highlight-width highlight)
              (.moveTo x1 y2)
              (.lineTo x1 y1)
              (.lineTo x2 y1)))
        (when lowlight
          (doto box
            (.lineStyle lowlight-width lowlight)
            (.moveTo x1 y2)
            (.lineTo x2 y2)
            (.lineTo x2 y1)))))

    (when draw-border
      (doto box
        (.lineStyle border-width border-colour)
        (.drawRect 0 0 width height)))

    (.generateTexture box false)))

(defn add-prog-bar [stage options]
  (let [s (sprite/make-sprite (progress-texture 0 options))]
    (set! (.-alpha s) 0)
    (.addChild stage s)
    s))

(defn get-texture [key scale]
  (scale (get @!texture-store key)))

;; setup a pixi texture cache keyed by the tail of its filename
(defn- register!
  [url texture-hash]
  (swap! !texture-store assoc (string/url-keyword url) texture-hash)
  texture-hash)

(defmethod resources/register! "png" [url texture-hash] (register! url texture-hash))
(defmethod resources/register! "gif" [url texture-hash] (register! url texture-hash))
(defmethod resources/register! "jpg" [url texture-hash] (register! url texture-hash))


(defn load [url]
  (let [c (chan)
        img (js/Image.)]
    (set! (.-crossOrigin img) "")
    (set! (.-onload img)
          #(put! c [url
                    {:linear (js/PIXI.Texture.
                              (js/PIXI.BaseTexture. img js/PIXI.SCALE_MODES.LINEAR))
                     :nearest (js/PIXI.Texture.
                               (js/PIXI.BaseTexture. img js/PIXI.SCALE_MODES.NEAREST))
                     :image img}
                    ]))
    (set! (.-onerror img) #(put! c [url nil]))
    (set! (.-onabort img) #(js/alert "abort"))

    ;; trigger load
    (set! (.-src img) url)
    c))

(defmethod resources/load "png" [url] (load url))
(defmethod resources/load "gif" [url] (load url))
(defmethod resources/load "jpg" [url] (load url))

(defn fadeout [spr & {:keys [duration start end]
                        :or {duration 1 start nil end 0}}]
  (let [start (if (nil? start) (.-alpha spr) start)
        ticks (* 60 duration)]
    (go
      (loop [i ticks]
        (<! (events/next-frame))
        (set! (.-alpha spr) (+ end (* (- start end) (/ i ticks))))
        (when (pos? i) (recur (dec i)))))))


(defn fadein [spr & {:keys [duration start end]
                      :or {duration 1 start 0 end 1}}]
  (let [ticks (* 60 duration)]
    (go
      (loop [i ticks]
        (<! (events/next-frame))
        (set! (.-alpha spr) (+ start (* (- end start) (/ (- ticks i) ticks))))
        (when (pos? i) (recur (dec i)))))))

(defn load-resources [canvas layer urls & {:keys [fade-in fade-out]
                      :or {fade-in 0.5 fade-out 0.5}
                      :as options}]
  (let [b (add-prog-bar (-> canvas :layer layer) options)]
    (go
      ;; fade in
      (<! (fadein b :duration fade-in))

      ;; load urls and show progress
      (let [coll (<! (resources/load-urls urls
                      (fn [i num-urls url obj]
                        (set! (.-texture b)
                              (progress-texture (/ i num-urls) options)))))]

        ;; fadeout
        (<! (fadeout b :duration fade-out))

        ;; remove progress bar sprite
        (.removeChild (-> canvas :layer layer) b)

        ;; return collection or urls -> resources
        coll))))
