# <img src="http://ericdscott.com/NaturalLexiconLogo.png" alt="NaturalLexicon logo" :width=100 height=100/> ont-app/vocabulary

Integration between Clojure keywords and URIs, plus support for
RDF-style language-tagged literals.

This library should work under both clojure and clojurescript.

## Contents
- [Installation](#h2-installation)
- [A brief synopsis](#h2-a-brief-synopsis)
- [Motivation](#h2-motivation)
- [Defining keyword Identifiers (KWIs) mapped to URI namespaces](#h2-defining-kwis)
  - [Basic namespace metadata](#h3-basic-metadata)
  - [Adding vann metadata to a Clojure Var](h3-adding-vann-metadata-to-a-clojure-var)
  - [Working with KWIs](#h3-working-with-kwis)
    - [`uri-for`](#h4-iri-for)
      - [URI syntax](#uri-syntax)
    - [`keyword-for`](#h4-keyword-for)
    - [`qname-for`](#h4-qname-for)
  - [Accessing-namespace-metadata](#h3-accessing-namespace-metadata)
    - [`put-ns-meta!` and `get-ns-meta`](#h4-put-ns-meta)
    - [`prefix-to-ns`](#h4-prefix-to-ns)
    - [`ns-to-namespace`](#h4-ns-to-namespace)
    - [`namespace-to-ns`](#h4-namespace-to-ns)
    - [`ns-to-prefix`](#h4-ns-to-prefix)
    - [`clear-caches!`](#h4-clear-caches)
  - [Support for SPARQL queries](#h3-support-for-sparql)
    - [`sparql-prefixes-for`](#h4-sparql-prefixes-for)
    - [`prepend-prefix-declarations`](#h4-prepend-prefix-declarations)
  - [Common Linked Data namespaces](#h3-linked-data)
    - [Imported with_ont-app.vocabulary.core](#h4-imported-with-voc)
    - [Imported with ont-app.vocabulary.wikidata](#h4-imported-with-wd)
    - [Imported with ont-app.vocabulary.linguistics](#h4-imported-with-ling)
- [Language-tagged strings](#h2-language-tagged-strings)
- [License](#h2-license)

<a name="h2-installation"></a>
## Installation

Available at [clojars](https://clojars.org/ont-app/vocabulary).

[![Clojars Project](https://img.shields.io/clojars/v/ont-app/vocabulary.svg)](https://clojars.org/ont-app/vocabulary)


```clj
 (defproject .....
  :dependencies 
  [...
   [ont-app/vocabulary "<this version>"]
   ...
   ])
```   

Or in a `deps.edn` file:

`{:deps {ont-app/vocabulary {:mvn/version "RELEASE"}}}`


<a name="h2-a-brief-synopsis"></a>
## A brief synopis


```clj
(ns ...
 (:require
   [ont-app.vocabulary.core :as voc] 
   ))

```

```clj
> (voc/uri-for :rdfs/subClassOf)
"http://www.w3.org/2000/01/rdf-schema#subClassOf"
```

```clj
> (voc/keyword-for "http://www.w3.org/2000/01/rdf-schema#subClassOf")
:rdfs/subClassOf
```

```clj
> (voc/qname-for :rdfs/subClassOf
"rdfs:subClassOf"
```

```clj
> (voc/keyword-for "rdfs:subClassOf")
:rdfs/subClassOf
```

<a name="h2-motivation"></a>
## Motivation
Clojure provides for the definition of
[keywords](https://clojure.org/reference/data_structures#Keywords),
which function as identifiers within Clojure code, and serve many
useful purposes.  These keywords can be interned within specific
namespaces to avoid collisions. The role played by these keywords is
very similar to the role played by URIs within the [Linked Open
Data](https://www.wikidata.org/wiki/Q515701) (LOD) community, which also has a regime
for providing namespaces.

Ont-app/vocabulary provides mappings between [Clojure
namespaces](https://clojure.org/reference/namespaces) and
[URI](https://en.wikipedia.org/wiki/Uniform_Resource_Identifier)-based
namespaces using declarations within Clojure metadata on those namespaces.

It also lets you attach the same metadata to [Clojure
vars](https://clojure.org/reference/vars) with the same effect.

There is support for a similar arrangement within
[Clojurescript](https://clojurescript.org/), though some things are
done a little differently given the fact that Clojurescript does not
implement metadata in the same way.

These mappings set the stage for using Keyword Identifiers (KWIs)
mappable between Clojure code and the wider world through a
correspondence with URIs.

Another construct from RDF that may have application more generally in
graph-based data is that of a [language-tagged
literal](#h2-language-tagged-strings), which tags strings of natural
language with their associated language. For example we could use such
tags to express the differing orthographies of `"gaol"@en-GB`
vs. `"jail"@en-US`. This library defines a custom reader tag
`voc/lstr` for declaring similar language-tagged strings,
e.g. `#voc/lstr "gaol@en-GB"` and `#voc/lstr "jail@en-US"`.

<a name="h2-defining-kwis"></a>
## Defining Keyword Identifiers (KWIs) mapped to URI namespaces

```clj
(ns ...
 (:require
   ...
   [ont-app.vocabulary.core :as voc] 
   ...))
```

This will load function definitions interned in the vocabulary.core
namespace, and also [a number of other `ns`
declarations](#h4-imported-with-voc), each dedicated to a commonly
occurring namespace in the world of LOD.

<a name="h3-basic-metadata"></a>
### Basic namespace metadata 
Within standard (JVM-based) clojure, the minimal specification to support ont-app/vocabulary functionality for a given namespace requires metadata specification as follows:

```clj
(ns org.example
  {
    :vann/preferredNamespacePrefix "eg"
    :vann/preferredNamespaceUri "http://example.org/"
  }
  (:require 
  [ont-app.vocabulary.core :as voc]
  ...))
```

This expresses an equivalence between the clojure keyword...

```clj
  :eg/example-var
```
... and the URI ...
```
 <http://example.org/example-var>
```

The `vann` prefix refers to [an existing public
vocabulary](http://vocab.org/vann/) which will be explained in more detail
[below](#h3-accessing-namespace-metadata).

Unfortunately, Clojurescript does not implement namespaces as
first-class objects, and so there is no `ns` object to which we can
attach metadata. So _ont-app/vocabulary_ provides this idiom to
achieve the same effect in both clj and cljs environments:

```clj
(voc/put-ns-meta!
 'org.example
  {
    :vann/preferredNamespacePrefix "eg"
    :vann/preferredNamespaceUri "http://example.org/"
  })
```

In Clojure, it simply updates the metadata of the named namespace. If
the namespace does not already exist, it will be automatically created
with [_create-ns_](https://clojuredocs.org/clojure.core/create-ns). In
Clojurescript, this updates a dedicated map from _org.example_ to
'pseudo-metadata' in a global atom called _cljs-ns-metadata_.

<a name="h3-adding-vann-metadata-to-a-clojure-var"></a>
### Adding `vann` metadata to a Clojure Var

On the JVM, You also have the option of assigning the `vann` metadata
described above to a [Clojure
Var](https://clojure.org/reference/vars).

```clj
(def 
  ^{
      :vann/preferredNamespacePrefix "myVar"
      :vann/preferredNamespaceUri "http://example.org/myVar/"
    }
   my-var nil)
```

This metadata is attached to the var.

```clj
(meta #'my.namespace/my-var)
->
{:vann/preferredNamespacePrefix "myVar",
 :vann/preferredNamespaceUri "http://example.org/myVar/",
 ...
 :name my-var,
 :ns #namespace[my.namespace]}}
```

All the same behaviors described herein for namespace metadata will apply.


<a name="h3-working-with-kwis"></a>
### Working with KWIs

<a name="h4-iri-for"></a>
#### `uri-for`

We can get the URI string associated with a keyword:

```clj
> (voc/uri-for :eg/Example)
"http://example.org/Example"
>
```

This function is called `uri-for` to reflect common usage, but because
any UTF-8 characters can be used, these are actually
[IRIs](https://en.wikipedia.org/wiki/Internationalized_Resource_Identifier). The
function _iri-for_ function is also defined as an alias of _uri-for_.

##### URI syntax

There are two dynamic variables defined to recognize and partially
parse URI strings under _ont-app/vocabulary_.

- `voc/ordinary-iri-str-re` by default is defined as `"^(http:|https:|file:).*"`
- `voc/exceptional-iri-str-re` by default is defined as `#"^(urn:|arn:).*"`

These can be [rebound](https://clojuredocs.org/clojure.core/binding)
as needed to match against URIs for your specific use case.

<a name="h4-keyword-for"></a>
#### `keyword-for`

We can get a keyword for a URI string...

```clj
> (voc/keyword-for "http://xmlns.com/foaf/0.1/homepage")
:foaf/homepage
>
```

If the namespace does not have sufficient metadata to create a
namespaced keyword, the keyword will be interned as an unqualified
keyword, escaped to conform with proper keyword syntax:

```clj
> (voc/keyword-for "http://example.com/my/stuff")
:http:%2F%2Fexample.com%2Fmy%2Fstuff
>
```

Characters which would choke the reader will be %-escaped. These characters differ depending on whether we're using the jvm or cljs platforms.

There is an optional arity-2 version whose first argument is called
when no ns could be resolved:

```clj
> (voc/keyword-for (fn [u k] 
                     (log/warn "No namespace metadata found for " u) 
                     (keyword-for u))
                  "http://example.com/my/stuff)

WARN: No namespace metadata found for "http://example.com/my/stuff"
:http:%2F%2Fexample.com%2Fmy%2Fstuff
>          
```

<a name="h4-qname-for"></a>
#### `qname-for`

We can get the [qname](https://en.wikipedia.org/wiki/QName) for a
keyword, suitable for insertion into RDF or SPARQL source:

```clj
> (voc/qname-for :foaf/homepage)
"foaf:homepage"
>
```


<a name="h3-accessing-namespace-metadata"></a>
### Accessing namespace metadata

<a name="h4-put-ns-meta"></a>
#### `put-ns-meta!` and `get-ns-meta`
Let's take another look at the metadata we used above to declare mappings between clojure namespaces and RDF namespaces:

```clj
(voc/put-ns-meta!
 'org.example
  {
    :vann/preferredNamespacePrefix "eg"
    :vann/preferredNamespaceUri "http://example.org/"
  })
```

Note that the metadata for this module includes some qualified
keywords in this format:

```clj
:<prefix>/<name>
```

The relations
[preferredNamespaceUri](http://vocab.org/vann/#preferredNamespacePrefix)
and
[preferredNamespacePrefix](http://vocab.org/vann/#preferredNamespaceUri)
are part of the public VANN vocabulary, with well-defined usage and
semantics.


The namespace for `vann` is also declared as _ont-app.vocabulary.vann_ in the
`ont_app/vocabulary/core.cljc` file, with this declaration:

```clj
(voc/put-ns-meta!
 'ont-app.vocabulary.vann
 {
   :rdfs/label "VANN"
   :dc/description "A vocabulary for annotating vocabulary descriptions"
   :vann/preferredNamespaceUri "http://purl.org/vocab/vann"
   :vann/preferredNamespacePrefix "vann"
   :foaf/homepage "http://vocab.org/vann/"
 })
```

Using the `put-ns-meta!` function ensures that this metadata works on
both clojure and clojurescript.

There is an inverse of _put-ns-meta!_ called _get-ns-meta_:

```clj
> (voc/get-ns-metadata 'ont-app.vocabulary.foaf)
{
 :dc/title "Friend of a Friend (FOAF) vocabulary"
 :dc/description "The Friend of a Friend (FOAF) RDF vocabulary,
 described using W3C RDF Schema and the Web Ontology Language."
 :vann/preferredNamespaceUri "http://xmlns.com/foaf/0.1/"
 :vann/preferredNamespacePrefix "foaf"
 :foaf/homepage "http://xmlns.com/foaf/spec/"
 :dcat/downloadURL "http://xmlns.com/foaf/spec/index.rdf"
 :voc/appendix [["http://xmlns.com/foaf/spec/index.rdf"
                 :dcat:mediaType "application/rdf+xml"]]
 })
>
```

These are much richer descriptions than the minimal example in the
previous section, with metadata encoded using several different public
vocabularies, described [below](#h4-linked-data).

Note that these are all simple key/value declarations except the
`:voc/appendix` declaration which is in the form

```clj
:voc/appendix [[<subject> <predicate> <object>]....], 
```

This includes triples which elaborate on constructs mentioned in the
key-value pairs in the rest of the metadata, in this case describing
the media types of files describing the vocabulary which are available
for download at the URLs given. This vector-of-triples format is
readable by one of ont-app/vocabulary's siblings,
[ont-app/igraph](https://github.com/ont-app/igraph).

<a name="h4-prefix-to-ns"></a>
#### `prefix-to-ns`
We can get a map of all the prefixes of namespaces declared within the
current lexical environment:

```clj
> (voc/prefix-to-ns)
{"dc" #namespace[ont-app.vocabulary.dc],
 "owl" #namespace[ont-app.vocabulary.owl],
 "ontolex" #namespace[ont-app.vocabulary.ontolex],
 "foaf" #namespace[ont-app.vocabulary.foaf],
 ...
 }
 >
 ```
 
In Clojurescript, since there's no _ns_ object, the results would look like this:

```clj
> (voc/prefix-to-ns)
{"dc" ont-app.vocabulary.dc,
 "owl" ont-app.vocabulary..owl,
 "ontolex" ont-app.vocabulary.ontolex,
 "foaf" ont-app.vocabulary.foaf,
 ...
 }
 >
```

<a name="h4-ns-to-namespace"></a>
#### `ns-to-namespace`
We can get the URI namespace associated with an `ns`

In Clojure:

```clj
> (voc/ns-to-namespace (find-ns 'ont-app.vocabulary.foaf))
"http://xmlns.com/foaf/0.1/"
>
```

In both Clojure and ClojureScript:

```clj
> (voc/ns-to-namespace 'ont-app.vocabulary.foaf)
"http://xmlns.com/foaf/0.1/"
>
```

<a name="h4-namespace-to-ns"></a>
#### `namespace-to-ns`
We can get a map from namespace URIs to their associated clojure namespaces:

```clj
> (voc/namespace-to-ns)
{
 "http://www.w3.org/2002/07/owl#"
 #namespace[org.naturallexicon.lod.owl],
 "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#"
 #namespace[org.naturallexicon.lod.nif],
 "http://purl.org/dc/elements/1.1/"
 #namespace[org.naturallexicon.lod.dc],
 "http://www.w3.org/ns/dcat#"
 #namespace[org.naturallexicon.lod.dcat],
 ...
 }
>
```

With the usual allowance for clojurescript described above.

<a name="h4-ns-to-prefix"></a>
#### `ns-to-prefix`
We can get the prefix associated with an `ns`:
```clj
> (voc/ns-to-prefix (voc/cljc-find-ns 'org.naturallexicon.lod.foaf))
"foaf"
>
```

<a name="h4-clear-caches"></a>
#### `clear-caches!`
For performance reasons, these metadata values are all cached. If
you're making changes to the metadata and it's not 'taking', you may
need to clear the caches:

```clj
> (voc/clear-caches!)
```

<a name="h3-support-for-sparql"></a>
### Support for SPARQL queries

RDF is explicitly constructed from URIs, and there is an intimate
relationship between [SPARQL](https://en.wikipedia.org/wiki/SPARQL)
queries and RDF namespaces. `ont-app/vocabulary` provides facilities
for extracting SPARQL prefix declarations from queries containing
qnames.

<a name="h4-sparql-prefixes-for"></a>
#### `sparql-prefixes-for`
We can infer the PREFIX declarations appropriate to a SPARQL query:
```clj
> (voc/sparql-prefixes-for
             "Select * Where{?s foaf:homepage ?homepage}")
("PREFIX foaf: <http://xmlns.com/foaf/0.1/>")
>
```

<a name="h4-prepend-prefix-declarations"></a>
#### `prepend-prefix-declarations`
Or we can just go ahead and prepend the prefixes...

```clj
> (voc/prepend-prefix-declarations
               "Select * Where {?s foaf:homepage ?homepage}")
"PREFIX foaf: <http://xmlns.com/foaf/0.1/>
Select * Where{?s foaf:homepage ?homepage}"
>
```

<a name="h3-linked-data"></a>
### Common Linked Data namespaces

Part of the vision of the `ont-app` project is to provide a medium for
expressing what adherents to Domain-driven Design and Behavior-driven
Design call a "[Ubiquitous
Vocabulary](https://martinfowler.com/bliki/UbiquitousLanguage.html)". It
also shares the vision of the Linked Data community that huge [network
effects](https://en.wikipedia.org/wiki/Network_effect) can emerge when
vocabularies emerge which are shared amongst a community of users
working in the same domain.

There are a large number of public vocabularies dedicated to various
application domains, some of which have gained a good deal of traction
in the Linked Data community. Ont-app/vocabulary includes declarations
of their associated namespaces, packaged within the core module, a
module dedicated to wikidata, and another dedicated to linguistics. 

<a name="h4-imported-with-voc"></a>
#### Imported with _ont-app.vocabulary.core_

Requiring the `ont-app.vocabulary.core` module also loads `ns`
declarations dedicated to some of the most commonly used RDF/Linked
Open Data prefixes: 


| PREFIX | URI | Comments |
| --- | --- | --- |
| [rdf](https://www.w3.org/2001/sw/wiki/RDF) | https://www.w3.org/2001/sw/wiki/RDF | the basic RDF constructs |
| [rdfs](https://www.w3.org/TR/rdf-schema/) | https://www.w3.org/TR/rdf-schema/ | expresses class relations, domain, range, etc. |
| [owl](https://www.w3.org/OWL/) | https://www.w3.org/OWL/ | for more elaborate ontologies |
| [vann](http://vocab.org/vann/) | https://vocab.org/vann/ | for annotating vocabulary descriptons |
| [dc](http://purl.org/dc/elements/1.1/) | http://purl.org/dc/elements/1.1/ | elements of [Dublin Core](http://dublincore.org/) metadata initiative |
| [dct](http://purl.org/dc/terms/) | http://purl.org/dc/terms/ | terms for the [Dublin Core](http://dublincore.org/) metadata initiative |
| [sh](https://www.w3.org/TR/shacl/) | https://www.w3.org/TR/shacl/ | for defining well-formedness constraints |
| [dcat](https://www.w3.org/TR/vocab-dcat/) | https://www.w3.org/TR/vocab-dcat/ | Data Catalog vocabulary |
| [foaf](http://xmlns.com/foaf/spec/) | http://xmlns.com/foaf/spec/ | the 'Friend of a Friend' vocabulary |
| [skos](https://www.w3.org/2009/08/skos-reference/skos.html) | http://www.w3.org/2004/02/skos/core# |for thesaurus-type taxonomies |
| [schema.org](https://schema.org/) | https://schema.org/ |  mostly commercial topics, with web-page metadata and search-engine indexes in mind |

<a name="h4-imported-with-wd"></a>
### Imported with _ont-app.vocabulary.wikidata_

Requiring the `ont-app.vocabulary.wikidata` module imports
declarations for the [several namespaces](https://www.mediawiki.org/wiki/Wikibase/Indexing/RDF_Dump_Format#Full_list_of_prefixes)
pertinent to the
[Wikidata](#https://www.wikidata.org/wiki/Wikidata:Main_Page) database.

It also defines the value for [Wikidata's public SPARQL
endpoint](https://query.wikidata.org/bigdata/namespace/wdq/sparql) as
this constant:

`ont-app.vocabulary.wikidata/sparql-endpoint`

<a name="h4-imported-with-ling"></a>
### Imported with _ont-app.vocabulary.linguistics_ 

The `ont-app.vocabulary.linguistics` module declares namespaces for:

| PREFIX | URI | Comments |
| --- | --- | --- |
| [ontolex](https://www.w3.org/2016/05/ontolex/) | http://www.w3.org/ns/lemon/ontolex# | for encoding lexical data | 
| [pmn](http://premon.fbk.eu/ontology/core.html) | http://premon.fbk.eu/ontology/core# | PreMOn - dedicated to describing English verbs |
| [nif](http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html) | http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#  | Natural Language Interchange Format - for annotating corpora |

There are also a set of namespaces particular to my Natural Lexicon
project, which are still under development.
    
<a name="h2-language-tagged-strings"></a>
## Language-tagged strings

RDF entails use of language-tagged strings (e.g. `"gaol"@en-GB`) when
providing natural-language content. Typing this directly in Clojure
code is a bit awkward, since the inner quotes would need to be
escaped.

To enable this language tag, we must require the namespace:

```clj
(require ...
  [ont-app.vocabulary.lstr :refer [lang]]
  )
```

This library defines a reader macro `#voc/lstr` and accompanying
deftype _LangStr_ to facilitate writing language-tagged strings in
clojure. The value above for example would be written: `#voc/lstr
"gaol@en-GB"`.

The reader encodes an instance of type LangStr (it is autoiconic):

```clj
> (def brit-jail #voc/lstr "gaol@en-GB")
brit-jail
> brit-jail
#voc/lstr "gaol@en-GB"
> (type brit-jail)
ont_app.vocabulary.lstr.LangStr
>
```

Rendered as a string, the language tag is dropped

```clj
> (str #voc/lstr "gaol@en-GB")
"gaol"
>
```

We get the language tag with `lang`:

```clj
> (lang #voc/lstr "gaol@en-GB")
"en-GB"
>
```


<a name="h2-license"></a>
## License

Copyright © 2019-22 Eric D. Scott

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

<table>
<tr>
<td width=75>
<img src="http://ericdscott.com/NaturalLexiconLogo.png" alt="Natural Lexicon logo" :width=50 height=50/> </td>
<td>
<p>Natural Lexicon logo - Copyright © 2020 Eric D. Scott. Artwork by Athena M. Scott.</p>
<p>Released under <a href="https://creativecommons.org/licenses/by-sa/4.0/">Creative Commons Attribution-ShareAlike 4.0 International license</a>. Under the terms of this license, if you display this logo or derivates thereof, you must include an attribution to the original source, with a link to https://github.com/ont-app, or  http://ericdscott.com. </p> 
</td>
</tr>
<table>
