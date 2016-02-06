(ns infinitelives.pixi.pixelfont
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [javax.imageio ImageIO]))

(defn horizontal-strip [y x1 x2]
  (for [x (range x1 x2)] [x y]))

(defn vertical-strip [x y1 y2]
  (for [y (range y1 y2)] [x y]))

(defn alpha-at [image [x y]]
  (-> image
      (.getRGB x y)
      (bit-shift-right 24)
      (bit-and 0xff)))

(defn alphas [image xy-seq]
  (map (partial alpha-at image) xy-seq))

(defn image-set-all-transparent? [image xy-seq]
  (not (some (comp not zero?) (alphas image xy-seq))))

(defn transparent-hlines [image x1 x2 y1 y2]
  (for [y (range y1 y2)]
    (image-set-all-transparent?
     image (horizontal-strip y x1 x2))))

(defn transparent-vlines [image x1 x2 y1 y2]
  (for [x (range x1 x2)]
    (image-set-all-transparent?
     image (vertical-strip x y1 y2))))

(defn strips [strip-fn dim image x1 x2 y1 y2]
  (let [strip-sizes
        (-> image
            (strip-fn x1 x2 y1 y2)
            (->> (map-indexed vector)
                 (partition-by second)
                 (map count))
            )]
    (partition 2 (for [n (range (count strip-sizes))]
                   (apply + (case dim :y y1 :x x1) (take (inc n) strip-sizes))))))

(def horizontal-strips (partial strips transparent-hlines :y))
(def vertical-strips (partial strips transparent-vlines :x))

(defn char-dimensions
  "returns a sequence of maps (with keys :x1
  :x2 :y1 :y2 :row :pos :char)"
  [image xi1 xi2 yi1 yi2 chars]
  (let [hstrips (horizontal-strips image xi1 xi2 yi1 yi2)
        vsize (apply max (map #(Math/abs (apply - %)) hstrips))]
    (map
     #(assoc %1 :char %2)
     (for [[row [y1 y2]] (map-indexed vector hstrips)
           [pos [x1 x2]] (map-indexed vector (vertical-strips image xi1 xi2 y1 y2))]
       {:x1 x1 :y1 y1 :x2 x2 :y2 (+ y1 vsize) :row row :pos pos})
     chars)))

(defn offset-dimensions [dimensions key update-fn & args]
  (map #(apply update % key update-fn args) dimensions))

(defn process-row
  "for every char in dimensions that lie in row,
  run function update-fn on it with args."
  [dimensions row key update-fn & args]
  (->> dimensions
       (map #(if (= (:row %) row)
               (apply update % key update-fn args)
               %))))

(defn filename->keyword [fname]
  (-> fname
      (string/split #"/")
      last
      (string/split #"\.")
      first
      keyword))

(def default-chars "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"#$%&'()*+,-./0123456789:;<=>?@[\\]^_`{|}~")

(def ascii-chars "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~")

(defmacro pixel-font[font-name filename [x1 y1] [x2 y2] &
                     {:keys [chars processors]
                      :or {chars default-chars
                           processors []}}]
  (let [image (->> filename
                   (io/file "resources/public")
                   ImageIO/read)
        chars (apply str chars)
        dimensions (char-dimensions image x1 x2 y1 y2 chars)
        final-dims (reduce
                    (fn [acc [func & args]]
                      (eval (concat [func (vec acc)] args)))
                    dimensions
                    processors)]
    `(load-pixel-font
      ~font-name
      ~(filename->keyword filename)
      ~(vec (for [{:keys [char x1 y1 x2 y2]} final-dims]
              [(str char) x1 y1 x2 y2])))))

(comment
  (macroexpand '(pixel-font :test-font "test.png" [127 84] [350 128]
                            :processors [
                                         (offset-dimensions :x2 dec)
                                         (process-row 0 :x1 + 10)
                                         ]
                            :chars "AB"))
)



(comment
  (def a (-> "test.png"
             io/file
             ImageIO/read
                                        ;(horizontal-strips 0 200 0 200)
                                        ;(process-line 0 1000 89 96)





             (char-dimensions 127 350 84 128 "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
             (offset-dimensions :x2 dec)
             (process-row 0 :x1 + 10)
             (process-row 1 :x2 number?))

    )

  )
