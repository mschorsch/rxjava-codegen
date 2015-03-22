# TODO
----------

## Additional features
- [] rescursive class resolution
- [x] constructor generation
- [] class modifier change
- [] remove extends `java.lang.Object`
- [] create oberservable (async) on demand
- [x] include/exclude deprecated
- [] private method call over reflection ? 

## Changes
- [x] sort methods by name and number of parameters
- [] change @author  

## Refactoring
- [] remove messy code
- [] process the hole jdk
- [x] change code generation (done. using FreeMarker!)
	- `JavaWriter` has no support for generics 
	- `JavaPoet` is not usable because of Java 7 dependencies.
- [] add compile tests see [https://github.com/google/compile-testing](https://github.com/google/compile-testing)

## Todo
- [] increase performance
- [] javadoc