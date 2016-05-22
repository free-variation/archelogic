(ns archelogic.logic-test
  (:import 
    [archelogic MicroKanren])
  (:use [clojure.test]
        [clojure.java.io :as io]
        [archelogic.logic]))

(deftest test-empty?o
  (testing "empty list"
           (is (= (run* [] (empty?o (µlist)))
                  '([]))))
  (testing "empty list as variable"
           (is (= (run* [q] (=== q MicroKanren/nil) (empty?o q))
                  (list [MicroKanren/nil]))))
  (testing "empty vector"
           (is (= (run* [] (empty?o (into-array [])))
                  '([]))))
  (testing "atom"
           (is (= (run* [] (empty?o 4))
                  '())))
  (testing "non-empty variable"
         (is (= (run* [q] (=== q (µlist 10)) (empty?o q))
                '())))
  (testing "non-empty collection"
           (is (= (run* [] (empty?o (into-array [1 2])))
                  '())))
  (testing "fresh var"
           (is (= (run* [q] (empty?o q))
                  (list [MicroKanren/nil]))))
  )

(deftest test-ntho
   (testing "var index"
            (is (= (run* [q r] (=== q 2) (ntho (into-array [1 2 3 4]) q r))
                   '([2 3]))))
   )

(deftest test-fails?
  (testing "failure"
           (is (= (run* [] (fails? fail))
                  '([]))))
  (testing "success"
           (is (= (run* [] (fails? succeed))
                  '())))
  (testing "failure"
           (is (= (run* [] (fails? (conde (succeed fail) (else fail))))
                  '([]))))
 
  )

(deftest test-equal
  (testing "simple value to value"
           (is (= (.toString (.run (fresh [q] (=== 5 5)) empty-state))
                  "[[] c: 1]")))
  
  (testing "unequal values"
           (is (= (.run (fresh [q] (=== 4 5)) empty-state)
                  nil)))
  (testing "default value left"
           (is (= (.toString (.run (fresh [q] (=== _ 5)) empty-state))
                  "[[] c: 1]")))
  (testing "default value right"
           (is (= (.toString (.run (fresh [q] (=== 5 _)) empty-state))
                  "[[] c: 1]")))
  (testing "equal a structure"
           (is (= (map str (run* [r] (fresh [x y](=== r (µcons x y)))))
                  '("[#<Cons (_1 . _2)>]"))))      
  (testing "simple var to value"
           (is (= (run* [q] (=== q 5))
                 '([5]))))
  (testing "simple right var to value"
           (is (= (run* [q] (=== 5 q))
                 '([5]))))
  (testing "simple var to var"
           (is (= (.toString (.run (fresh [q r] (=== q r)) empty-state))
                  "[[<1>] c: 2]")))
  (testing "two vars, two values"
           (is (= (.toString (.run (fresh [q r] (=== (µcons q r) (µcons 10 20))) empty-state))
                  "[[10, 20] c: 2]")))
  (testing "two vars, two values, right-to-left"
           (is (= (.toString (.run (fresh [q r] (===  (µcons 10 20) (µcons r q))) empty-state))
                  "[[20, 10] c: 2]")))
  (testing "destructuring more complex"
           (is (= (.toString (.run (fresh [q r]  (=== (µcons q r) (µcons 10 (µcons 20 30)))) empty-state))
                  "[[10, (20 . 30)] c: 2]")))
  (testing "life without occurs_check..."
           (is (= (run* [a] (=== (µlist a 2 3 4 5 6) (µlist 1 2 3 4 5 a)))
                  '()))))


(deftest test-conso
   (testing "complete destructuring"
           (is (= (.toString (first (run* [h t q] (conso h t q))))
                  "[:_0 :_1 #<Cons (_0 . _1)>]")))
    (testing "complete destructuring after assignment"
           (is (= (.toString (first (run* [q] (fresh [h t] (=== h 1) (=== t (µcons 2 3)) (conso h t q)))))
                  "[#<Cons (1 . (2 . 3))>]")))
    (testing "complete destructuring before assignment"
           (is (= (.toString (first (run* [q] (fresh [h t]  (conso h t q) (=== h 1) (=== t (µcons 2 3))))))
                  "[#<Cons (1 . (2 . 3))>]")))
    (testing "chained consos"
           (is (= (.toString (first (run* [t] (fresh [r s] (conso r s (µlist 1 2)) (conso s (µlist 3) t)))))
                  "[#<Cons ((2 . ()) . (3 . ()))>]")
           (is (= (.toString (first (run* [q r s t] (conso 1 q r) (conso 2 q (µlist 2 3)) (conso s t r)) ))
                 "[#<Cons (3 . ())> #<Cons (1 . (3 . ()))> 1 #<Cons (3 . ())>]")))))

(deftest test-conde ; using examples from The Reasoned Schemer
  (testing "simple failure"
           (is (= (pull (.run (conde (fail succeed) (else fail)) empty-state))
                  *f)))
  (testing "simple success"
           (is (= (pull (.run (conde (succeed succeed) (else fail)) empty-state))
                 (vector empty-state))))

  (testing "multiple clauses"
           (is (= (run* [r] (conde ((=== 1 1)) ((=== 3 3)) ((=== 5 5)))(=== 1 1))
                  '([:_0] [:_0] [:_0]))))
  (testing "1.47"
           (let [r (pull (.run (fresh [x] 
                                      (conde 
                                        ((=== 'olive x) succeed) 
                                        ((=== 'oil x) succeed) 
                                        (else fail)))  
                           empty-state))]
             (is (= (.toString r) "[[olive] c: 1, [oil] c: 1]")))) 

  (testing "1.50"
          (is (= (run* [x] 
                       (conde 
                         ((=== 'virgin x) fail)
                         ((=== 'olive x) succeed)
                         (succeed succeed)
                         ((=== 'oil x) succeed) 
                         (else fail)))
                '([olive] [:_0] [oil]))))
  
  (testing "1.54"
          (is (= (run* [r] 
                       (fresh [x y]
                              (conde 
                                ((=== 'split x) (=== 'pea y))
                                ((=== 'navy x) (=== 'bean y))
                                (else fail))
                              (=== (µlist x y ) r)))
                (list [(µlist 'split 'pea)] [(µlist 'navy 'bean)]))))
  (testing "1.55"
          (is (= (run* [r] 
                       (fresh [x y]
                              (conde 
                                ((=== 'split x) (=== 'pea y))
                                ((=== 'navy x) (=== 'bean y))
                                (else fail))
                              (=== (µlist x y 'soup) r)))
                 (list [(µlist 'split 'pea 'soup)] [(µlist 'navy 'bean 'soup)]))))
                   
  )

(deftest test-appendo
  (testing "straight match"
           (is (= (run* [] (appendo (µlist 1 2 3) (µlist 4 5 6) (µlist 1 2 3 4 5 6)))
                  '([])))
           (is (= (run* [] (appendo (µlist 1 2 3) (µlist 4 5 6) (µlist 1 2 3 4 5)))
                  '())))
  (testing "var antecedent"
           (is (= (run* [q] (appendo q (µlist 4) (µlist 2 4)))
                  (list [(µlist 2)]))))
  (testing "embedded var"
             (is (= (run* [q] (appendo (µlist 1 2 q) (µlist 4 5 6) (µlist 1 2 3 4 5 6)))
                    '([3])))
             (is (= (run* [q] (appendo (µlist 1 2 3) (µlist 4 5 6) (µlist 1 2 q 4 5 6)))
                    '([3]))))
  (testing "collection var"
             (is (= (run* [q] (appendo (µlist 1 2 3) q (µlist 1 2 3 4 5 6)))
                    (list [(µlist 4 5 6)])))
             (is (= (run* [q] (appendo (µlist 1 2 3) (µlist 4 5 6) q))
                    (list [(µlist 1 2 3 4 5 6)]))))
  (testing "first list empty, second list of lists"
           (is (= (run* [] (appendo (µlist ) (µlist (list 1 2 3)) (µlist (list 1 2 3))))
                  '([]))))
  (testing "append twice"
           (is (= (run* [r] (fresh [q] (appendo (µlist 1) (µlist 2) q) (appendo q (µlist 3) r)))
                  (list [(µlist 1 2 3)]))))
               
  )

(deftest test-membero
  (testing "no vars"
           (is (= (run* [] (membero 1 (µlist 1 2 3 4 5)))
                  '([])))
           (is (= (run* [] (membero 5 (µlist 1 2 3 4 5)))
                  '([])))
           (is (= (run* [] (membero 6 (µlist 1 2 3 4 5)))
                  '())))
  (testing "simple search variable"
           (is (= (run* [q] (membero q (µlist 1 2 3 4 5)))
                  '([1] [2] [3] [4] [5]))))
  )

(deftest test-condu
  (testing "simple failure"
           (is (= (run* [] (condu (fail)))
                  '())))
  (testing "simple success"
           (is (= (run* [] (condu (succeed)))
                  '([]))))
  (testing "success, once"
           (is (= (run* [] (condu (succeed) (else succeed)))
                  '([]))))
  (testing "success, once"
           (is (= (run* [] (condu (fail) (else succeed)))
                  '([]))))
  (testing "ensure single result"
           (is (= (run* [q] (condu (succeed (=== q 10)) (else (=== q 20))))
                  '([10]))))
  (testing "compare to conde"
           (is (= (run* [q] (conde (succeed (=== q 10)) (else (=== q 20))))
                  '([10] [20]))))
  
  )

(deftest test-arithmetic
  (testing "non-relational equals"
           (is (= (run* [q] (=== q 10) (µ= q 10))
                  '([10])))
           (is (= (run* [q] (=== 10 q) (µ= 10 q))
                  '([10])))
           (is (= (run* [q] (=== q 11) (µ= q 10))
                  '())))
  (testing "non-relational greater-than"
           (is (= (run* [q] (=== q 10) (µ> q 10))
                  '()))
           (is (= (run* [q] (=== q 11) (µ> q 10))
                  '([11]))))
  (testing "non-relational less-than"
           (is (= (run* [q] (=== q 10) (µ< q 10))
                  '()))
           (is (= (run* [q] (=== q 9) (µ< q 10))
                  '([9]))))
   (testing "non-relational less-than-or-equal"
           (is (= (run* [q] (=== q 10) (µ<= q 10))
                  '([10])))
           (is (= (run* [q] (=== q 11) (µ< q 10))
                  '()))
           (is (= (run* [q] (=== q 9) (µ<= q 10))
                  '([9]))))
   (testing "non-relational greater-than-or-equal"
           (is (= (run* [q] (=== q 10) (µ>= q 10))
                  '([10])))
           (is (= (run* [q] (=== q 9) (µ> q 10))
                  '()))
           (is (= (run* [q] (=== q 11) (µ>= q 10))
                  '([11]))))
   
   (testing "plus"
            (is (= (run* [q] (µ+ 1 1 q))
                   '([2.0]))))
   (testing "minus"
            (is (= (run* [q] (µ- 2 1 q))
                   '([1.0]))))
   (testing "multiply"
            (is (= (run* [q] (µ* 3 2 q))
                   '([6.0]))))
   (testing "divide"
            (is (= (run* [q] (µdiv 6 3 q))
                   '([2.0]))))
     )

(deffact friend [name1 name2])
(assert-fact friend "john" "jan")
(assert-fact friend "xara" "ella")
(assert-fact friend "django" "gabriel")
(assert-fact friend "django" "adrian")
(assert-fact friend "john" "sasha")
(assert-fact friend "xara" "julia")

(deftest test-fact
  (testing "simple query"
           (is (= (run* [] (friend "john" "jan"))
                  '([]))))
  )

(defrelation rel1 [x y] 
  ['a 1])

(defrelation rel2 [x y] 
  ['a 1]
  ['b 2]
  ['c 3])

(deftest test-relation
  (testing "simple single clause predicate"
           (is (= (run* [r] (rel1 r 1))
                  '([a]))))
  (testing "simple multi-clause predicate, single match"
           (is (= (run* [r] (rel2 r 1))
                  '([a]))))
  (testing "simple multi-clause predicate, multiple match"
           (is (= (run* [r] (rel2 r _))
                  '([a] [b] [c])))))

(deftest test-callo
  (testing "bare function"
           (is (= (mapv second (run* [q r] (=== q (fn [a b] (membero a b))) (callo q r (µlist 2 3))))
                  [2 3])))
  (testing "relation"
           (is (= (mapv second (run* [q r] (=== q rel2) (callo q r _)))
                  '[a b c])))
  )


(deffact syn6 [synsetid wordnum word pos n1 n2])

(defn load-wordnet-test [f filename]
  (with-open [rdr (io/reader filename)]
    (doseq [line (line-seq rdr)]
      (println "asserting: " (read-string line))
      (apply (partial f :assert) (read-string line)))))