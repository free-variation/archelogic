(ns archelogic.logic
  (:import 
    [archelogic MicroKanren]))

(defmacro === [a b]
  `(MicroKanren/equals ~a ~b))

(defmacro call-fn [[v] & body]
  `(reify archelogic.MicroKanren$LambdaGoal
     (call [this# ~v] ~@body)))  

(defmacro conj+ [& goals]
    `(MicroKanren/conjAllVector [~@goals]))

(defmacro disj+ [& goals]
   `(MicroKanren/disjAllVector  [~@goals]))

(def empty-state (MicroKanren/emptyState))

(def _ MicroKanren/joker)

(defmacro µlist [& values]
  `(MicroKanren/listVector [~@values]))

(defmacro µcons [a b]
  `(MicroKanren/cons ~a ~b))

(defmacro fresh [vars & body]
   (if (empty? vars) 
    `(conj+ ~@body)
    `(MicroKanren/callFresh 
       (call-fn [~(first vars)] 
         (fresh ~(rest vars) ~@body)))))

(defn reify-var [var]
  (keyword (.reifyName var)))

(defmacro pull [stream] 
  `(MicroKanren/pull ~stream))

(def *f MicroKanren/mzero)
(def succeed MicroKanren/succeed)
(def fail MicroKanren/fail)
(def else succeed)


(defmacro run* [vars & body] 
  (let [num-vars (count vars)]
    `(map #(vec (take ~num-vars 
                      (mapv (fn[x#] (if (MicroKanren/isVar x#) (reify-var x#) x#)) 
                      (MicroKanren/reify %))))
          (MicroKanren/pullAll (.run (fresh ~vars ~@body) empty-state)))))

(defmacro run [n vars & body]
  (let [num-vars (count vars)]
    `(map #(vec (take ~num-vars 
                        (mapv (fn[x#] (if (MicroKanren/isVar x#) (reify-var x#) x#)) 
                        (MicroKanren/reify %)))) 
          (MicroKanren/take ~n (.run (fresh ~vars ~@body) empty-state)))))

;; =============================================================================
;; Essential relations

(defmacro empty?o [a]
  `(MicroKanren/emptyo ~a))

(defmacro fails? [a]
  `(MicroKanren/not ~a))

(defmacro conso [a b c]
  `(MicroKanren/conso ~a ~b ~c))

(defmacro appendo [a b c]
  `(MicroKanren/appendo ~a ~b ~c))

(defmacro membero [a b]
  `(MicroKanren/membero ~a ~b))

(defmacro callo [f & args]
   `(reify archelogic.MicroKanren$Goal
      (run [this# state#]  
        (let [wf# (MicroKanren/walk ~f (.subst state#))]
          (MicroKanren/call (wf# ~@args) state#)))))


; non-relational
(defmacro ntho [a b c]
  `(MicroKanren/ntho ~a ~b ~c))

; non-relational
(defmacro counto [a b]
  `(MicroKanren/counto ~a ~b))

(defmacro conde [& cases]    
  `(MicroKanren/condeVector ~(mapv vec cases)))

(defmacro condu [& cases] 
  (let [conj-goals (mapv (fn [case] 
                           `(into-array 
                              archelogic.MicroKanren$Goal 
                              ~(cons 'vector case))) 
                         cases)]
    `(MicroKanren/condu (into-array ~conj-goals))))

(defmacro par [goal] 
  `(MicroKanren/par ~goal))

;; =============================================================================
;; Arithmetic

(defmacro µ+ [a b c]
  `(MicroKanren/pluso ~a ~b ~c))

(defmacro µ- [a b c]
  `(MicroKanren/minuso ~a ~b ~c))

(defmacro µ* [a b c]
  `(MicroKanren/multo ~a ~b ~c))

(defmacro µdiv [a b c]
  `(MicroKanren/divo ~a ~b ~c))

(defmacro µ= [a b]
  `(MicroKanren/eqo ~a ~b))

(defmacro µ> [a b]
  `(MicroKanren/gto ~a ~b))

(defmacro µ< [a b]
  `(MicroKanren/lto ~a ~b))

(defmacro µ>= [a b]
  `(MicroKanren/gtEqo ~a ~b))

(defmacro µ<= [a b]
  `(MicroKanren/ltEqo ~a ~b))

;; =============================================================================
;; utilities

(defmacro printlno [& args]
   `(reify archelogic.MicroKanren$Goal
      (run [this# state#]  
        (let [wx# (map (fn [x#] (MicroKanren/deepWalk x# (.subst state#))) (list ~@args))] 
          (apply println wx#)
          (MicroKanren/unit state#)))))

;; =============================================================================
;; facts and relations

(defmacro deffact [fact-name fields]
  `(let [fact# (MicroKanren/fact (into-array (map str (quote ~fields))))]
     (defn ~fact-name [op# & terms#]
       (cond
         (= op# :assert) (.assertFact fact# (into-array java.lang.Object terms#))
         (= op# :query)  (MicroKanren/queryoVect fact# (vec terms#))
         :else (MicroKanren/queryoVect fact# (vec (cons op# terms#)))))))


(defmacro assert-fact [fact & terms]
  `(~fact :assert ~@terms))


(defmacro defrelation [rel-name arglist & clauses]
  `(defn ~rel-name ~arglist
     (disj+ 
       ~@(map (fn [clause]
                `(conj+ ~@(map (fn [a b] `(=== ~a ~b)) arglist clause)))
              clauses))))




