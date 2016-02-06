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

(defn make-font [resource-key layout]
  {
   :font (make-font-description resource-key layout)
   :texture (r/get-texture resource-key :nearest)
   })

(defn load-pixel-font
  [pixel-font-name texture-key layout]
  (swap! pixel-fonts assoc pixel-font-name (make-font texture-key layout)))

(defn get-font [font-key]
  (font-key @pixel-fonts))

(defn make-text [font-key text kerning]
  (let [font (get-font font-key)
        batch
        (js/PIXI.ParticleContainer.)]
    (loop [[c & l] (seq text)
           xp 0 yp 0
           last-c nil]
      (let [char ((:font font) c)
            {:keys [texture pos size]} char
            [x y] pos
            [w h] size
            pair (str last-c c)
            kern (or kerning {})
            koff (kern pair)
            ]
        (if (nil? char)
          (when (seq l)
            (recur l (+ xp 5) yp c))

          (do
            (.addChild batch (s/make-sprite texture :x (+ xp koff) :y yp :xhandle 0 :yhandle 0))
            (if (seq l)
              (recur l (+ xp w 1.0 koff) yp c)
              (s/set-pivot! batch (/ (+ xp w koff) 2.0) 0))))))
    (s/set-scale! batch 4)
    (s/set-anchor! batch 0.5 0.5)
    batch))

(comment
  (infinitelives.pixi.pixelfont/load-pixel-font :test-font :test [["A" 146 89 140 97] ["B" 154 89 148 97]])

  )
