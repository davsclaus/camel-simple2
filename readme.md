# Improved Simple language

An improved Simple expression language for Camel based on a recursive descent parser.

## Current problems

The current [Simple language](http://camel.apache.org/simple) in Camel has two drawbacks:

* no parser to report exact position syntax errors
* no ast
* it does not differentiate whether its a 'org.apache.camel.Expression' or 'org.apache.camel.Predicate'
  being evaluated.
* causes end users on a trial and error run in case their Camel routes does not route message
  in the right destination in their Content Based Router using Simple language.

The current implementation is based on a sophisticated regular expression matcher and thus
we have stretched as far we can take this.

## Improving

This is a rewrite of an improved Simple language for Apache Camel.
Its based on the principles of a
[recursive descent parser](http://en.wikipedia.org/wiki/Recursive_descent_parser)

The new parser will have exact position error reporting, for example if you enter an invalid operator
as shown below (eg the is a '=' missing in the equals operator)

    org.apache.camel.language.simple.SimpleIllegalSyntaxException:
    unexpected character symbol at location 15
    ${header.high} = true
                   *

Then there is a error indicator that the problem is at position 15.

## Changes

The simple language now operates in two modes
- expression
- predicate

In the former mode, it operates in a template like mode, where dynamic ${ } placeholders is replaced.
The binary and logical operators is not supported.

In the latter mode, it operates in a predicate mode, where operators is in use.

The reason for these two modes is that it makes it easier to use the language as a very simple template language
as well as for predicates in the Camel routes. Having distinct modes helps the parser knowing this, and thus
being able to parse and report invalid syntax errors much better.

## Backwards compatible

The simple language should be backwards compatible. However as the parser is now much better to pickup syntax errors
it may report slight syntax errors which the old language did not.

It is encouraged to always use dynamic ${ } placeholders for the simple functions to make it clear that its a function.
However the old style of being able to do just "body" or "header.foo" is supported (but the style is considered @deprecated).
This style will be removed in Camel 3.0.

## Todo

* Configure start and end tokens for function, eg to use something else than ${ }

* SimpleTokenizer should initialize list of known tokens once