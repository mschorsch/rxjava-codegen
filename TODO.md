# TODO
----------

## Additional features
- [] rescursive class resolution
- [] class modifier change
- [] remove extends `java.lang.Object`
- [] create oberservable
- [] private method call over reflection ?
- [] Observable generation with distinction between just and from
- [] support for `rxjava-async-util`
- [] support invoke_on_object for object methods  

## Changes
- [] change @author  

## Refactoring
- [] remove messy code
- [x] change code generation (done. using `FreeMarker`!)
	- `JavaWriter` has no support for generics 
	- `JavaPoet` is not usable because of Java 7 dependencies.
- [] add compile tests see [https://github.com/google/compile-testing](https://github.com/google/compile-testing)

## Todo
- [] javadoc
- [] increase performance
- [] process the hole jdk
- [] write documentation in readme.md