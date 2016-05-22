(defproject archelogic "0.1.1-SNAPSHOT"
  :dependencies 
  [[org.clojure/clojure "1.6.0"]
   [org.clojure/tools.trace "0.7.8"]
   [org.clojure/core.unify "0.5.6"]
   [org.clojure/math.combinatorics "0.1.1"]
   [org.clojure/data.json "0.2.6"]]
  
  :jvm-opts 
  ^:replace ["-server" "-Xmx1g"
             "-Dhazelcast.logging.type=none"
             "-XX:+UnlockCommercialFeatures" "-XX:+FlightRecorder"])
      
