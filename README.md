# Function Passing: Typed, Distributed Functional Programming

[![Build Status](https://travis-ci.org/heathermiller/f-p.svg?branch=master)](https://travis-ci.org/heathermiller/f-p/)

The `F-P` library uses [sbt](http://www.scala-sbt.org/) for building and
testing. Distributed tests and examples are supported using the
[sbt-multi-jvm](https://github.com/sbt/sbt-multi-jvm) sbt plug-in.

Homepage: [http://lampwww.epfl.ch/~hmiller/f-p](http://lampwww.epfl.ch/~hmiller/f-p)

## How to Run Examples and Tests

Examples and tests can be launched from the sbt prompt:

```
> project samples
[info] Set current project to f-p samples (in build file:/Users/nrm/Sources/f-p/)
> multi-jvm:run multijvm.cs.Example
```

## Implementation agnostic

The default `F-P` realization is backed by [Netty](http://netty.io Netty). In order to
demonstrate our implementation agnostic approach, you can find at
`samples/single-jvm/main/scala/actor` an [Akka](http://akka.io Akka)-based,
alternative realization.
