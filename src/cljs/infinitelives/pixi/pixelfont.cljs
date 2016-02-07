(ns infinitelives.pixi.pixelfont
  (:require [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.utils.console :refer [log]]))

(defonce pixel-fonts
  (atom {}))

(defn make-font-description [resource-key layout]
  (let [texture (r/get-texture resource-key :nearest)]
    (into {}
          (map
           (fn [[c x1 y1 x2 y2]]
             (let [size [(- x2 x1) (- y2 y1)]]
               [c
                {:char c
                 :pos [x1 y1]
                 :to [x2 y2]
                 :size size
                 :texture (t/sub-texture
                           texture
                           [x1 y1]
                           size)}]))
           layout))))

(defn make-font [resource-key layout kerning space]
  {
   :font (make-font-description resource-key layout)
   :texture (r/get-texture resource-key :nearest)
   :kerning kerning
   :space space
   })

(defn load-pixel-font
  [pixel-font-name texture-key layout kerning space]
  (swap! pixel-fonts assoc pixel-font-name
         (make-font texture-key layout kerning space)))

(defn get-font [font-key]
  (font-key @pixel-fonts))

(defn make-text [font-key text & {:keys [tint scale anchor rotation]}]
  (let [font (get-font font-key)
        batch
        (if tint
          (js/PIXI.Container.)
          (js/PIXI.ParticleContainer.))]
    (loop [[c & l] (seq text)
           xp 0 yp 0
           last-c nil]
      (let [char ((:font font) c)
            {:keys [texture pos size]} char
            [x y] pos
            [w h] size
            pair (str last-c c)
            koff ((:kerning font) pair)
            ]
        (if (nil? char)
          ;; if character is not present in font map, put a space
          (when (seq l)
            (recur l (+ xp (:space font)) yp c))

          (do
            ;character is present, add the sprite to the container
            (.addChild batch (s/make-sprite texture :x (+ xp koff) :y yp :xhandle 0 :yhandle 0 :tint tint))
            (if (seq l)
              (recur l (+ xp w 1.0 koff) yp c)
              (s/set-pivot! batch (/ (+ xp w koff) 2.0) 0))))))

    ;; set setup details
    (when scale
      (s/set-scale! batch scale))
    (when rotation
      (s/set-rotation! batch scale))
    (when anchor
      (s/set-anchor! batch (anchor 0) (anchor 1)))
    batch))

(comment
  (infinitelives.pixi.pixelfont/load-pixel-font :test-font :test [["A" 146 89 140 97] ["B" 154 89 148 97]])

  )
