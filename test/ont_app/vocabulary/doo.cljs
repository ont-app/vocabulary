(ns ont-app.vocabulary.doo
  (:require [doo.runner :refer-macros [doo-tests]]
            [ont-app.vocabulary.lstr]
            [ont-app.vocabulary.core-test]
            ))

(doo-tests
 'ont-app.vocabulary.core-test
 )
