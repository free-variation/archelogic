(ns archelogic.corenlp
   (:import 
    (java.util Properties List)
    (edu.stanford.nlp.util StringUtils)
    (edu.stanford.nlp.pipeline StanfordCoreNLP Annotation)
    (edu.stanford.nlp.ling CoreAnnotations$SentencesAnnotation) 
    (edu.stanford.nlp.semgraph 
      SemanticGraphCoreAnnotations$BasicDependenciesAnnotation 
      SemanticGraphCoreAnnotations$CollapsedCCProcessedDependenciesAnnotation)))

(defn make-pipeline
  "Creates and returns Stanford CoreNLP (pipeline) object with specified annotators"
  [annotators]
  (StanfordCoreNLP. (doto (Properties.) (.put "annotators" annotators))))

(defn make-depparse-pipeline []
  "Creates a Stanford CoreNLP pipeline set up to compute dependency parses
using the NN shift-reduce parser"
  (make-pipeline "tokenize,ssplit,pos,lemma,depparse"))

(defn parse-text
  "Takes in a string, processes the string using a pipeline, returns a 'document' with all relevant information"
  [pipeline text]
  (let [document (Annotation. text)]
 	  (.annotate pipeline document)
 	  document))

(def parse-text-memo (memoize parse-text))

(defn get-dependency-graphs
  "Takes a CoreNLP 'document' and returns a list of dependency graphs, one per sentence.  
  Option :collapse returns collapsed dependencies "
  [corenlp-doc & options]
  (for [sentence (.get corenlp-doc CoreAnnotations$SentencesAnnotation)]
    (if (some #{:collapse} options)
      (.get sentence SemanticGraphCoreAnnotations$CollapsedCCProcessedDependenciesAnnotation)
      (.get sentence SemanticGraphCoreAnnotations$BasicDependenciesAnnotation))))

(defn flatten-indexed-word
  "Converts an IndexedWord to a simple tuple"
  [word]
  (list 'word
        (.index word)
        (.word word)
        (.lemma word)
        (.tag word)))

(defn flatten-deppgraph
  "Takes a CoreNLP dependency graph, returning a lazy list of relations."
  [dep-graph]
  (map
    (fn [dep-rel]
      (let [gov (flatten-indexed-word (.gov dep-rel))
            dep (flatten-indexed-word (.dep dep-rel))
            reln (.reln dep-rel)]
        (list 'rel
              (.getShortName reln)
              gov
              dep)))
    (.typedDependencies dep-graph)))
