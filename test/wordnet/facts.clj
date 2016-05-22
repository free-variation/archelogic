(ns wordnet.facts
  (:require
    [clojure.java.io :as io])
  (:use
    [archelogic.logic])
   (:import 
    [archelogic MicroKanren]))

(deffact syn [synsetid wordNum word pos n1 n2])
(deffact ant [synsetid1 wordnum1 synsetid2 wordnum2])
(deffact vgp [synsetid1 wordnum1 synsetid2 wordnum2])
(deffact ent [synsetid1 synsetid2])
(deffact sim [synsetid1 synsetid2])

(deffact hyp [synsetid1 synsetid2])
(defn hypo [synsetid1 synsetid2]
  (hyp synsetid2 synsetid1))

(deffact ins [synsetid1 synsetid2])
(defn has-instance [synsetid1 synsetid2]
  (ins synsetid2 synsetid1))

(deffact mm [synsetid1 synsetid2])
(defn holo-mem [synsetid1 synsetid2]
  (mm synsetid2 synsetid1))

(deffact ms [synsetid1 synsetid2])
(defn holo-subs [synsetid1 synsetid2]
  (ms synsetid2 synsetid1))

(deffact mp [synsetid1 synsetid2])
(defn holo-part [synsetid1 synsetid2]
  (mp synsetid2 synsetid1))

(deffact cs [synsetid1 synsetid2])
(defn causee [synsetid1 synsetid2]
  (cs synsetid2 synsetid1))

(defrelation semantic-relation [relation pos-list num-args]
  [hyp (µlist :n :v) 2]
  [hypo (µlist :n :v) 2]
  [ins (µlist :n) 2]
  [has-instance (µlist :n) 2]
  [ent (µlist :v) 2]
  [sim (µlist :a) 2]
  [mm (µlist :n) 2]
  [holo-mem (µlist :n) 2]
  [ms (µlist :n) 2]
  [holo-subs (µlist :n) 2]
  [mp (µlist :n) 2]
  [holo-part (µlist :n) 2]
  [cs (µlist :v) 2]
  [causee (µlist :v) 2]
  [vgp (µlist :v) 4]
  [ant (µlist :v :n :a :r :s) 4]
  )



(defrelation relation-map [rel1 rel2]
  [hyp  :forward]
  [hypo :reverse]
  [mm   :forward]
	[holo-mem :reverse]  
	[ms :forward]
	[holo-subs :reverse]
	[mp :forward]
	[holo-part :reverse]
	[ant :negation]
	[vgp ::equivalence]
	[sim :equivalence]
	[ins :forward]
	[has-instance :reverse]
	[ent :forward]
	[syn :equivalence])

(defrelation join [rel1 rel2 rel3]
  [:equivalence rel2 rel2]
  [rel1 :equivalence rel1]
  [:forward :forward :forward]
	[:forward :negation :alternation]
	[:forward :alternation :alternation]
	[:reverse :reverse :reverse]
	[:reverse :negation :cover]
	[:reverse :cover :cover]
	[:negation :forward :cover]
	[:negation :reverse :alternation]
	[:negation :negation :equivalence]
	[:negation :alternation :reverse]
	[:negation :cover :forward]
	[:alternation :reverse :alternation]
	[:alternation :negation :forward]
	[:alternation :cover :forward]
	[:cover :forward :cover]
	[:cover :negation :reverse])

(defn load-wordnet [f filename]
  (with-open [rdr (io/reader filename)]
    (doseq [line (line-seq rdr)]
      (println (read-string line))
      (apply (partial f :assert) (read-string line)))))

(defn load-all-wordnet []
  (doseq [[f file] (map list 
                        (list ant vgp hyp syn
                              ins ent sim mm ms mp cs)
                        (map #(str "test/data/wordnet/" % ".clj") 
                             (list "ant" "vgp" "hyp" "syn" 
                             "ins" "ent" "sim" "mm" "ms" "mp" "cs")))]
    (load-wordnet f file)))