(ns ont-app.vocabulary.browser
  (:require [doo.runner :refer-macros [doo-tests]]
            [ont-app.vocabulary.core-test]))

(doo-tests 'ont-app.vocabulary.core-test)
