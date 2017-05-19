(ns archelogic.corenlp2prolog
  (:require
    [archelogic.corenlp :as nlp]
    [archelogic.util :as u]
    
    [clojure.java.io :as io]))

(defn parse-file [in-file, out-file]
  (let [pipe (nlp/make-depparse-pipeline)]
    (with-open [r (io/reader in-file)]
      (with-open [w (io/writer out-file)]
        (doall (map-indexed
                 (fn [idx sentence]
                      (let [graph (first (nlp/get-dependency-graphs (nlp/parse-text pipe sentence)))
                            relations (nlp/flatten-deppgraph graph)]
                        (.write w "parse(")
                        (.write w (str idx))
                        (.write w ",\"")
                        (.write w sentence)
                        (.write w "\", [")
                        (doseq [rel relations]
                          (.write w "\n\t")
                          (.write w (u/sexpr2pred rel))
                          (.write w ","))
                        (.write w (str idx))
                        (.write w "]).\n")))
                 (line-seq r)))))))
      