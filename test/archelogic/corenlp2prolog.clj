(ns archelogic.corenlp2prolog
  (:require
    [archelogic.corenlp :as nlp]
    [archelogic.util :as u]
    
    [clojure.java.io :as io]))

(defn parse-file [file]
  (with-open [r (io/reader file)]
    