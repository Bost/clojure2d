(ns clojure2d.extra.segmentation
  "Segment image into parts.

  Currently contains only quadtree segmentation."
  (:require [clojure2d.core :as core]
            [fastmath.core :as m]
            [fastmath.random :as r]
            [clojure2d.pixels :as p])
  (:import [clojure2d.pixels Pixels]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
(m/use-primitive-operators)

(defn- calc-stdev
  "Calculate standard deviation of selection 10% of random channel values"
  ([^Pixels pixels ch sx sy w h]
   (let [sx (long sx)
         sy (long sy)
         limit (max (* 0.1 (double (* ^long w ^long h))) 4.0)]
     (loop [A 0.0
            Q 0.0
            k 1.0]
       (let [posx (r/irand w)
             posy (r/irand h)
             ^int xk (p/get-value pixels ch (+ sx posx) (+ sy posy))
             newA (+ A (/ (- xk A) k))
             newQ (+ Q (* (- xk A) (- xk newA)) )]
         (if (< k limit)
           (recur newA newQ (inc k))
           (m/sqrt (/ Q (dec limit)))))))))

(defn segment-pixels
  "Decompose channel `ch` from `Pixels` into square segments using quadtree decomposition.

  * `min-size` is minimum size of segment
  * `max-size` is maximum size
  * `threshold` is accuracy (minimum std dev of pixel values to make decision about subdivision.

  Returns list of of vectors containing [x y size] where [x y] is segment position and `size` is a side length."
  ([p ch] (segment-pixels p ch {}))
  ([^Pixels p ch {:keys [^long min-size ^long max-size ^double threshold]
                  :or {min-size 4 max-size 256 threshold 15.0}}]
   (let [ww (<< 1 (m/high-2-exp (.w p)))
         hh (<< 1 (m/high-2-exp (.h p)))
         mins (max 2 min-size)

         segmf (fn local-segmentation
                 [^long x ^long y ^long size res]
                 (if (or (>= x (.w p)) (>= y (.h p)))
                   res
                   (lazy-seq
                    (let [^double stdev (calc-stdev p ch x y size size)]
                      (if (or (> size max-size)
                              (and (> size mins)
                                   (> stdev threshold)))
                        (let [mid (>> size 1)]
                          (->> res
                               (local-segmentation (+ x mid) (+ y mid) mid)
                               (local-segmentation x (+ y mid) mid)
                               (local-segmentation (+ x mid) y mid)
                               (local-segmentation x y mid)))
                        (cons [x y size] res))))))]
     (segmf 0 0 (max ww hh) nil))))
