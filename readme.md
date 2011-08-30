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
