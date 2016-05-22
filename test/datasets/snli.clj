(ns datasets.snli
  (:require 
    [clojure.data.json :as json]
    [clojure.java.io :as io]))

(defn get-pairs [set]
  "returns a lazy stream of SNLI sentence pairs"
  (let [r (io/reader (str "test/data/snli/snli_1.0_" set ".jsonl"))]
    (map #(select-keys (json/read-str %) ["pairID" "sentence1" "sentence2" "gold_label"])
         (line-seq r))))

