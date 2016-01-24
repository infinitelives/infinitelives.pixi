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

;; (defn -main
;;   "I don't do a whole lot ... yet."
;;   [& args]
;;   (println "Hello, World!"))

(comment
  (def a (-> "test.png"
             io/file
             ImageIO/read
             (transparent-hlines 0 200 0 200)
             (->> (map-indexed vector)
                  (partition-by second)
                  (map count))

             ))


  (for [n (range (count a))] (apply + (take (inc n) a))))
