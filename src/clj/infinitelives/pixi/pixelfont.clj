(ns infinitelives.pixi.pixelfont
  (:require [clojure.java.io :as io])
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
    ;strip-sizes
    (partition 2 (for [n (range (count strip-sizes))]
                   (apply + (case dim :y y1 :x x1) (take (inc n) strip-sizes))))))

(def horizontal-strips (partial strips transparent-hlines :y))
(def vertical-strips (partial strips transparent-vlines :x))

(defn char-dimensions
  "returns a sequence of vectors
  [x1 x2 y1 y2 char]"
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
