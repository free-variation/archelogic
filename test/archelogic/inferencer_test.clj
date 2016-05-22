(ns archelogic.inferencer_test
  (:use [clojure.test])
  (:require [datasets.snli :as snli]))

(defn test-random [set]
  (let [choices ["entailment" "neutral" "contradiction"]
        pairs (snli/get-pairs set)]
    (map #(= (nth choices (rand-int 2))
             (% "gold_label"))
         pairs)))
    

