(ns vocabulary.core-test
  (:require [clojure.test :refer [testing]]
            [vocabulary.core :refer [ns-to-namespace iri-for ns-to-prefix
                                     qname-for keyword-for sparql-prefixes-for
                                     prepend-prefix-declarations]
             ]))


(deftest attr-map-test
  (testing "Tests public attr-map tests"
    (is (= (test #'ns-to-namespace)
           :ok))
    (is (= (test #'iri-for)
           :ok))
    (is (= (test #'ns-to-prefix)
           :ok))
    (is (= (test #'qname-for)
           :ok))
    (is (= (test #'keyword-for)
           :ok))
    (is (= (test #'sparql-prefixes-for)
           :ok))
    (is (= (test #'prepend-prefix-declarations )
           :ok))))

