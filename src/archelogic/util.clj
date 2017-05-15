(ns archelogic.util
  (:require
    [clojure.string :as s]))


(defn sexpr2pred
  "Converts an s-expr to predicate form"
  [s-expr]
  (if (seq? s-expr)
    (let [head (first s-expr)
          args (map sexpr2pred (rest s-expr))]
      (str head "(" (s/join "," args) ")"))
    (str "\"" s-expr "\"")))
  