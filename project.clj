(defproject archelogic "0.1.2-SNAPSHOT"
  :dependencies 
  [[org.clojure/clojure "1.8.0"]
   [org.clojure/tools.trace "0.7.9"]
   [org.clojure/core.unify "0.5.7"]
   [org.clojure/math.combinatorics "0.1.4"]
   [org.clojure/data.json "0.2.6"]
  
  ; Stanford CoreNLP
   [edu.stanford.nlp/stanford-corenlp "3.7.0"]
   [edu.stanford.nlp/stanford-corenlp "3.7.0" :classifier "models"]]
  
  :jvm-opts  ["-server" "-Xmx1g"
              "-XX:+UnlockCommercialFeatures" "-XX:+FlightRecorder"])
      
