(ns archelogic.match
  (:require
    [clojure.set :as set]
    [clojure.core.unify :refer [lvar? unify]]))

(defn extractor- [f acc y]
  (if (coll? y)
    (if (empty? y)
      acc
      (map (partial extractor- f acc) y))
    (if (f y)
      (cons y acc)
      acc)))

(defn extract-all-lvars [y]
  (set (flatten (extractor- lvar? '() y))))
    
(defmacro match [matchee & clauses]
  (let [matchee-vars (extract-all-lvars matchee)]
    (letfn [(match-maker [rest-clauses]
              (if (empty? rest-clauses)
                nil
                (let [matcher (first rest-clauses)
                      body (second rest-clauses)]
                   (if (= :else matcher)
                     body
                     (let [vars (set/union matchee-vars (extract-all-lvars matcher))
                           lvar-map (gensym)]
                       `(let [~lvar-map (unify ~matchee ~matcher)]
                          (if ~lvar-map
                            (let [ ~@(apply concat (map (fn [var] `(~var (~lvar-map '~var))) vars)) ]
                              ~body)
                            ~(match-maker (rest (rest rest-clauses))))))))))]
      (match-maker clauses))))
                
  
  



