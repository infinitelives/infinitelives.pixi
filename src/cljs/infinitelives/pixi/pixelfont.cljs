(ns infinitelives.pixi.pixelfont
  (:require [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.utils.console :refer [log]]
            [cljs.core.async :refer [<! chan put! timeout close!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

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

(defn clear-text! [batch]
  (.removeChildren batch))

(defn make-char-sprite-set [font-key text tint]
  (let [font (get-font font-key)]
    (loop [[c & l] (seq text)
           xp 0 yp 0
           last-c nil ;; remember last character for kerning
           sprite-set []]
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
            (recur l (+ xp (:space font)) yp c sprite-set))

          (let [sprite (s/make-sprite texture :x (+ xp koff) :y yp :xhandle 0 :yhandle 0 :tint tint :scale 1)]
            (if (seq l)
              (recur l (+ xp w 1.0 koff) yp c (conj sprite-set sprite))
              (conj sprite-set sprite))))))))

(defn add-text! [batch font-key text tint]
  (doseq [spr (make-char-sprite-set font-key text tint)]
    (.addChild batch spr)))

(defn change-text! [batch font-key text tint]
  (clear-text! batch)
  (add-text! batch font-key text tint))

(defn make-text [font-key text & {:keys [tint scale anchor rotation
                                         x y visible
                                         xhandle yhandle]
                                  :or {scale s/*default-scale*
                                       visible true
                                       xhandle 0.5
                                       yhandle 0.5}}]
  (s/make-container
   (make-char-sprite-set font-key text tint)
   :particle (not tint)
   :scale scale
   :rotation rotation
   :x x
   :y y
   :visible visible
   :xhandle xhandle
   :yhandle yhandle))

(defn appear-text [font-key text & {:keys [tint scale anchor rotation
                                           x y visible
                                           xhandle yhandle
                                           delay]
                                    :or {scale s/*default-scale*
                                         visible true
                                         xhandle 0.5
                                         yhandle 0.5
                                         delay 32}}]
  (let [chars (make-char-sprite-set font-key text tint)
        batch
        (s/make-container
         []
         :particle (not tint)
         :scale scale
         :rotation rotation
         :x x
         :y y
         :visible visible
         :xhandle xhandle
         :yhandle yhandle)]
    (go
      (loop [[c & r] chars]
        (.addChild batch c)
        (s/update-handle! batch xhandle yhandle)
        (<! (timeout delay))
        (when r (recur r))))
    batch))

(defn string-width
  "return the width of this text in this font (in unit pixels)."
  [font-key text]
  (let [{:keys [font kerning space]} (get-font font-key)]
    (+
     ;; char sizes
     (reduce + (for [c text]
                 (if (= " " c)
                   space
                   ((:size (font c)) 0))))

     ;; kerning adjustments
     (reduce + (map (comp kerning str) text (subs text 1)))

     ;; char spacing
     (dec (count text)))))

(defn char-fits? [font-key x left right char]
  (let [{:keys [font kerning space]} (get-font font-key)
        {:keys [size]} (font char)]
    (if (< (+ x size)) (+ x size) left)))

(defn regex-modifiers
  "Returns the modifiers of a regex, concatenated as a string."
  [re]
  (str (if (.-multiline re) "m")
       (if (.-ignoreCase re) "i")))

(defn re-pos
  "Returns a vector of vectors, each subvector containing in order:
   the position of the match, the matched string, and any groups
   extracted from the match."
  [re s]
  (let [re (js/RegExp. (.-source re) (str "g" (regex-modifiers re)))]
    (loop [res []]
      (if-let [m (.exec re s)]
        (recur (conj res (vec (cons (.-index m) m))))
        res))))


(defn word-beginnings-ends
  "given a string of words:

  'this is a string of words'

  return the slice position of the end of each word as a vector

  [4 7 9 16 19 25]
  "
  [s]
  (map
   (fn [[n t]] [n (+ n (count t))])
   (re-pos #"\S+" s)))

(defn how-many-words-fit [font-key text ends size]
  (loop [n 0
         [e & r] ends]
    (let [words (subs text 0 e)
          width (string-width font-key words)
          ]
      (log "words" words width r n size
           )
      (if (and (< width size) r)
        (recur (inc n) r)
        n))))




(comment
  (infinitelives.pixi.pixelfont/load-pixel-font :test-font :test [["A" 146 89 140 97] ["B" 154 89 148 97]])

  )
