(ns archelogic.start)

(in-ns 'user)

(import archelogic.MicroKanren)

(use 'archelogic.logic :reload)
(use 'archelogic.logic-test :reload)
(use 'clojure.stacktrace)
(use 'clojure.test)
(use 'clojure.tools.trace)
(use 'clojure.pprint)
(use 'clojure.walk)

(set! *warn-on-reflection* true)

(use 'wordnet.facts :reload)
(use 'wordnet.natlog :reload)

