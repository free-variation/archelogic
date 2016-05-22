(ns wordnet.natlog
  (:use
    [archelogic.logic]
    [wordnet.facts]))


(defn find-path8 [syn1 word-num1 syn2 word-num2 
                  pos max-path-length path1 path2] 
  (conde
    ((=== syn1 syn2) (fresh [word]
                            (syn syn1 word-num2 word _, _, _)
                            (appendo path1 (µlist (µcons syn word)) path2)))
   
    ((fresh [l relation pos-list num-args word]
            (counto path1 l)
            (µ< l max-path-length)
            (semantic-relation relation pos-list num-args)
            (membero pos pos-list)
            (fresh [syn3 word-num3 path3]
                   (conde 
                     ((µ= 2 num-args) (callo relation syn1 syn3))
                     ((µ= 4 num-args) (callo relation syn1 word-num1 syn3 word-num3)))
                   (conde
                     ((fails? (empty?o path1))
                       (fresh [prev-pos last-entry prev-rel r1 r2]
                              (µ- l 1 prev-pos)
                              (ntho path1 prev-pos last-entry)
                              (ntho last-entry 0 prev-rel)
                              (relation-map prev-rel r1)
                              (relation-map relation r2)
                              (join r1 r2 _)))
                     ((empty?o path1) succeed))
                   
                   (appendo path1 (µlist (µcons relation syn3)) path3)
                  
                   (find-path8 syn3 word-num3 syn2 word-num2 pos max-path-length path3 path2))))
     ((fresh [l relation pos-list num-args word]
            (counto path1 l)
            (semantic-relation relation pos-list num-args)
            (membero pos pos-list)
            (conde 
              ((µ= 2 num-args) (callo relation syn1 syn2))
              ((µ= 4 num-args) (callo relation syn1 word-num1 syn2 word-num2)))        
            (conde
              ((fails? (empty?o path1))
                (fresh [prev-pos last-entry prev-rel r1 r2]
                       (µ- l 1 prev-pos)
                       (ntho path1 prev-pos last-entry)
                       (ntho last-entry 0 prev-rel)
                       (relation-map prev-rel r1)
                       (relation-map relation r2)
                       (join r1 r2 _)))
              ((empty?o path1) succeed))
     
            (appendo path1 (µlist (µcons relation syn2)) path2)))
    (else fail)))

(defn find-path5 [word1 word2 pos max-path-length path]
  (fresh [syn1 syn2 word-num1 word-num2]
         (syn syn1 word-num1 word1 pos _ _)
         (syn syn2 word-num2 word2 pos _ _)
         (par (find-path8 syn1 word-num1 syn2 word-num2 
                          pos max-path-length (µlist) path))))




