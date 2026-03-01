# Kanbana

A simple microservice learning project built using bun, scala and nodejs. The intended outcome is a project that will allow users to be able to manage tasks and chat with each other.


# Decisons for project

## Scala
- Why scala?
	- Cuz I am learning how to use it
	- Why not akka or some of the more scala idiomatic solutions for websockets? Why use bun for handling websockets?
		- Truth to be told: I wanted to use socket.io as I am also learning vue as I go, I fear that if I do end up using websockets for the client with a different implementation than what I am used to, I might take a longer time. But maybe this decision to use bun might change.

## Bun
- Why Bun?
	- I worked with it on a previous project and liked the out of the box support for sql

## Dragonfly DB?
- Redis-like caching oriented db that is also compliant to the API (to my understanding)
- Got curious about it, might end up using something else
