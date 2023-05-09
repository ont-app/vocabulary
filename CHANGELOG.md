# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

v 0.3.0
  - Adding `Resource` protocol with method `resource-class`
  - Addding methods `as-uri-string`, `as-kwi`, `as-qname` dispatched on `resource-class`
  - Adding and enforcing specs for uri strings, kwis, qnames, and namespace metadata
v 0.2.2
  - Adding uri-str-for to infer URI strings
  - Tweak to uri-for
v 0.2.1
- Tweaks to README
- Refinement on print-method implementation for LangStr

v 0.2.0
- Moving from lein to deps.edn
- Adding shadow-cljs.edn file defining a `:node-test` build
- Custom tagged literal #lstr deprecated in favor of #voc/lstr
- Fix for issue 12: #voc/lstr now works in clojurescript source
- Fix for issue 19: Support for urns and arns.
- Fix for issue 20: Improvements to char-escaping behavior for keywords and URIs
- Fix for issue 21: vann metadata can be attached to vars as well as namespaces

v 0.1.7
- Tweaks to issue 15 fix.

v 0.1.6
- Fix for issue 15: Adding support for new lines in #lstr tags

v 0.1.5
- Minor tweaks

v 0.1.4
- Fix for issue 10: Removing unneeded dependencies

v 0.1.3
- cljs.compiler requirement conditional on :cljs (bug fix)

v 0.1.2
- (Breaking) Changed LangStr from record to type
- There's now a format module to handle en/de-coding of URIs and KWIs for clj(s)
- cljc-(get/put)-ns-meta! is deprecated in favor of just (get/put)-ns-meta!
- duplicate prefix mappings throw an error

v 0.1.1
- keyword-for now allows you to respond to the case where no ns
  metadata could be found for the given uri.
- Added the lstr module for language-tagged strings
- cljs-put-ns-meta! has alias put-ns-meta!
- cljs-get-ns-meta has alias get-ns-meta
- iri-for has alias uri-for
- there's an optional on-no-ns argument in keyword-for
